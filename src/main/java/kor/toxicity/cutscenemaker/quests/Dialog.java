package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.events.DialogEndEvent;
import kor.toxicity.cutscenemaker.events.DialogStartEvent;
import kor.toxicity.cutscenemaker.material.WrappedMaterial;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.ActionPredicate;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Dialog extends EditorSupplier implements Comparable<Dialog> {

    private static final TypingManager DEFAULT_TYPING_EXECUTOR = current -> {
        if (current.inventory == null) current.inventory = InvUtil.create(current.talker + "'s dialog",CutsceneConfig.getInstance().getDefaultDialogRows());
        if (!current.isOpened) {
            current.isOpened = true;
            current.player.openInventory(current.inventory);
        }
        final int center = CutsceneConfig.getInstance().getDefaultDialogCenter();
        current.inventory.setItem(center,new ItemStack(CutsceneConfig.getInstance().getDialogReader()));
        final ItemStack target = current.inventory.getItem(center);
        final ItemMeta meta = target.getItemMeta();
        return new TypingExecutor() {
            private Map<Integer,ItemBuilder> before;
            @Override
            public void initialize(DialogRecord record, String currentTalker) {
                if (before != null) before.keySet().forEach(i -> {
                    if (i != center) current.inventory.setItem(i,null);
                });
                before = record.stacks;
                before.forEach((k,v) -> {
                    if (k != center) current.inventory.setItem(k,v.get(current.player));
                });
                meta.setDisplayName(ChatColor.WHITE + currentTalker + ":");
                target.setItemMeta(meta);
            }
            @Override
            public void apply(String message, Consumer<Player> soundPlay) {
                meta.setLore(Collections.singletonList(message));
                target.setItemMeta(meta);
                if (soundPlay != null) soundPlay.accept(current.player);
                current.player.updateInventory();
            }
        };
    };

    private static final Pattern TALK_PATTERN = Pattern.compile("(((?<talker>(\\w|\\W)+):)?)(\\s?)(?<content>(\\w|\\W)+)",Pattern.UNICODE_CHARACTER_CLASS);
    public static final ConfigMapReader<String> TALK_READER_STRING = new ConfigMapReader<>(ConfigurationSection::getString);
    public static final ConfigMapReader<ConfigurationSection> TALK_READER_CONFIGURATION = new ConfigMapReader<>((c, k) -> (c.isConfigurationSection(k)) ? c.getConfigurationSection(k) : null);

    private static final Map<String, Function<String,? extends DialogAddon>> ADDON_MAP = new HashMap<>();

    private static final Map<String,TypingManager> TYPING_MANAGER_MAP = new HashMap<>();
    private static final Map<Player,DialogRun> CURRENT_TASK = new ConcurrentHashMap<>();
    static final List<Runnable> LAZY_TASK = new ArrayList<>();

    private static void addLazyTask(Runnable runnable) {
        LAZY_TASK.add(runnable);
    }
    public static void addDialogAddon(String[] keys, Function<String,? extends DialogAddon> function) {
        addValue(
                ADDON_MAP,
                function,
                keys
        );
    }

    private static final Map<String,BiConsumer<Dialog,List<String>>> STRING_LIST_PARSER = new HashMap<>();
    static void stopAll(CutsceneMaker plugin) {
        plugin.getManager().runTask(() -> {
            CURRENT_TASK.forEach((p, d) -> {
                d.cancel();
                if (d.current.isOpened) p.closeInventory();
            });
            CURRENT_TASK.clear();
        });
    }

    private static <V> void addValue(Map<String,V> target, V value, String... key) {
        for (String s : key) {
            target.put(s,value);
        }
    }
    static {
        addValue(TYPING_MANAGER_MAP,DEFAULT_TYPING_EXECUTOR,"default","gui");
        TYPING_MANAGER_MAP.put("title",current -> {
            if (current.isOpened) {
                current.isOpened = false;
                current.player.closeInventory();
            }
            return new TypingExecutor() {
                private String talker;
                @Override
                public void initialize(DialogRecord record, String currentTalker) {
                    talker = ChatColor.YELLOW + ChatColor.BOLD.toString() + currentTalker;
                }

                @Override
                public void apply(String message, Consumer<Player> soundPlay) {
                    if (soundPlay != null) soundPlay.accept(current.player);
                    current.player.sendTitle(talker,message,0,60,20);
                }
            };
        });
        TALK_READER_STRING.add("Talker",(d, s) -> d.talker = new FunctionPrinter(s));
        TALK_READER_STRING.add("Interface",(d, s) -> {
            d.typingManager = TYPING_MANAGER_MAP.get(s);
            if (d.typingManager == null) d.warn("The Interface named \"" + s + "\" doesn't exist!");
        });
        TALK_READER_STRING.add("Sound",(d, s) -> d.addConsumer(QuestUtil.getSoundPlay(s)));
        TALK_READER_CONFIGURATION.add("Item",(d, c) -> c.getKeys(false).forEach(s -> {
            try {
                int i = Integer.parseInt(s);
                CutsceneConfig config = CutsceneConfig.getInstance();
                if (i != config.getDefaultDialogCenter() && i < config.getDefaultDialogRows() * 9) {
                    ItemBuilder builder = InvUtil.fromConfig(c,s);
                    if (builder != null) d.stacks.put(i, builder);
                }
            } catch (Exception e) {
                d.warn("fail to load the item data: " + s);
            }
        }));
        STRING_LIST_PARSER.put("CheckQuest",(q,t) -> addLazyTask(() -> t.forEach(s -> {
            String[] args = TextUtil.split(s," ");
            ActionPredicate<Player> predicate = q.getQuestChecker(args[0],(args.length > 1) ? args[1].toLowerCase() : "complete");
            if (predicate != null) {
                if (args.length > 2) {
                    Dialog dialog = QuestUtil.getDialog(args[2], q.fileName, q.name);
                    if (dialog != null) q.addPredicate(d -> predicate.castInstead(p -> dialog.run(d)).test(d.player));
                } else q.addPredicate(d -> predicate.test(d.player));
            }
        })));
        STRING_LIST_PARSER.put("Condition",(q,t) -> addLazyTask(() -> t.forEach(s -> {
            String[] cond = TextUtil.split(s," ");
            ActionPredicate<LivingEntity> check = (cond.length >= 3) ? ConditionBuilder.LIVING_ENTITY.find(cond) : null;
            if (check != null) {
                if (cond.length > 3) {
                    Dialog dialog = QuestUtil.getDialog(cond[3], q.fileName, q.name);
                    if (dialog != null) q.addPredicate(d -> check.castInstead(p -> dialog.run(d)).test(d.player));
                } else q.addPredicate(d -> check.test(d.player));
            }
        })));
        addValue(
                STRING_LIST_PARSER,
                (q,t) -> addLazyTask(() -> q.endDialog = QuestUtil.getDialog(t, q.fileName, q.name)),
                "LinkedDialog","Dialog"
        );
        addValue(
                STRING_LIST_PARSER,
                (q,t) -> addLazyTask(() -> q.subDialog = QuestUtil.getDialog(t, q.fileName, q.name)),
                "LinkedSubDialog","SubDialog"
        );
        addValue(
                STRING_LIST_PARSER,
                (q,t) -> q.actions = t.toArray(new String[0]),
                "LinkedAction","Action"
        );
        addDialogAddon(
                new String[] {"LinkedQnA","QnA"},
                QuestData.QNA_MAP::get
        );
        addDialogAddon(
                new String[] {"LinkedPresent","Present"},
                QuestData.PRESENT_MAP::get
        );

        addValue(
                STRING_LIST_PARSER,
                (q,t) -> q.takeItem = QuestUtil.getItemBuilders(t),
                "TakeItem","Take"
        );
        addValue(
                STRING_LIST_PARSER,
                (q,t) -> q.giveItem = QuestUtil.getItemBuilders(t),
                "GiveItem","Give"
        );
        STRING_LIST_PARSER.put("SetQuest",(q,t) -> addLazyTask(() -> t.stream().map(s -> {
            String[] a = TextUtil.split(s," ");
            return q.getQuestConsumer(a[0],(a.length > 1) ? a[1].toLowerCase() : "give");
        }).filter(Objects::nonNull).forEach(c -> q.setQuest = q.setQuest.andThen(c))));
        STRING_LIST_PARSER.put("SetVars",(q,t) -> t.stream().map(s -> {
            String[] a = TextUtil.split(s," ");
            Consumer<Player> vars;
            if (a.length > 1) {
                vars = QuestUtil.getVarsConsumer(a[0],(a.length > 2) ? a[2] : null,a[1]);
            } else vars = null;
            if (vars == null) q.warn("unable to load this variable operation: \"" + s + "\"");
            return vars;
        }).filter(Objects::nonNull).forEach(c -> q.setQuest = q.setQuest.andThen(c)));
    }
    private boolean cancelDamage = true, cancelPickup = true;
    private final DialogRecord[] records;
    private Dialog[] subDialog, endDialog;
    private String[] actions;

    private final List<DialogAddon> addonList = new ArrayList<>();

    private ItemBuilder[] takeItem, giveItem;

    private Consumer<Player> setQuest = p -> {};
    private Predicate<DialogCurrent> conditions;
    private Map<String,Consumer<Player>> typingSounds;

    static void setExecutor(CutsceneMaker plugin) {
        EvtUtil.register(plugin, new Listener() {
            private DialogRun getDialogRun(Player player) {
                return CURRENT_TASK.get(player);
            }
            @EventHandler
            public void onInvClose(InventoryCloseEvent e) {
                if (e.getPlayer() instanceof Player) {
                    DialogRun run = getDialogRun((Player) e.getPlayer());
                    if (run != null && run.current.isOpened) run.stop();
                }
            }
            @EventHandler
            public void onDeath(PlayerDeathEvent e) {
                DialogRun run = getDialogRun(e.getEntity());
                if (run != null) run.stop();
            }
            @EventHandler
            public void onDamaged(EntityDamageEvent e) {
                if (e.getEntity() instanceof Player) {
                    DialogRun run = getDialogRun((Player) e.getEntity());
                    if (run != null && run.current.cancelDamage) e.setCancelled(true);
                }
            }
            @EventHandler
            public void onPickup(EntityPickupItemEvent e) {
                if (e.getEntity() instanceof Player) {
                    DialogRun run = getDialogRun((Player) e.getEntity());
                    if (run != null && run.current.cancelPickup) e.setCancelled(true);
                }
            }
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onInteract(PlayerInteractEvent e) {
                DialogRun run = getDialogRun(e.getPlayer());
                if (run != null) e.setCancelled(true);
            }
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                DialogRun run = getDialogRun(e.getPlayer());
                if (run != null) run.stop();
            }
            @EventHandler
            public void onCommand(PlayerCommandPreprocessEvent e) {
                DialogRun run = getDialogRun(e.getPlayer());
                if (run != null) e.setCancelled(true);
            }
            private final Map<Player,BukkitTask> delay = new HashMap<>();
            @EventHandler
            public void onInvClick(InventoryClickEvent e) {
                if (!(e.getWhoClicked() instanceof Player)) return;
                Player p = (Player) e.getWhoClicked();
                DialogRun run = getDialogRun(p);
                if (run == null) return;
                e.setCancelled(true);
                Inventory clickedInventory = e.getClickedInventory();
                if (clickedInventory != null && clickedInventory.equals(run.current.inventory) && e.getSlot() == CutsceneConfig.getInstance().getDefaultDialogCenter() && !delay.containsKey(p)) {
                    delay.put(p,plugin.getManager().runTaskLaterAsynchronously(() -> delay.remove(p), 4));
                    if (e.isLeftClick()) {
                        if (e.isShiftClick() && CutsceneConfig.getInstance().isEnableSkip()) {
                            run.skip();
                        } else {
                            run.current.time = Math.max(run.current.time - 1, 1);
                            run.restart();
                        }
                    }
                    if (e.isRightClick()) {
                        if (e.isShiftClick() && CutsceneConfig.getInstance().isEnableSkip()) {
                            run.completeSkip();
                        } else {
                            run.current.time = Math.min(run.current.time + 1, 4);
                            run.restart();
                        }
                    }
                }
            }
        });
    }


    private final boolean exception;
    Dialog(String fileName, String name, CutsceneManager manager, ConfigurationSection section) {
        super(fileName, name, manager, section);
        List<String> talk = getStringList(section,"Talk");
        if (talk != null) {
            records = talk.stream().map(s -> {
                try {
                    return new DialogRecord(s);
                } catch (Exception e) {
                    warn("Error: " + e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull).toArray(DialogRecord[]::new);
            exception = section.getBoolean("Exception",false);

            TALK_READER_STRING.apply(this,section);
            TALK_READER_CONFIGURATION.apply(this,section);

            Iterator<Map.Entry<String,Function<String,? extends DialogAddon>>> addonEntry = ADDON_MAP.entrySet().iterator();
            while (addonEntry.hasNext()) {
                Map.Entry<String,Function<String,? extends DialogAddon>> entry = addonEntry.next();
                ConfigUtil.getStringList(section,entry.getKey()).ifPresent(l -> l.forEach(s -> addLazyTask(() -> {
                    try {
                        DialogAddon addon = entry.getValue().apply(s);
                        if (addon != null) addonList.add(addon);
                        else warn("The " + entry.getKey() + " named \"" + s + "\" doesn't exist!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        addonEntry.remove();
                    }
                })));
            }
            ConfigUtil.getConfig(section,"TypingSound").ifPresent(c -> typingSounds = c.getKeys(false).stream().collect(Collectors.toMap(s -> s.replace("_"," "), s -> QuestUtil.getSoundPlay(c.getString(s)))));
            ConfigUtil.getConfig(section,"Option").ifPresent(c -> c.getKeys(false).forEach(s -> {
                switch (s.toLowerCase()) {
                    case "damage":
                        cancelDamage = c.getBoolean(s);
                        break;
                    case "pickup":
                        cancelPickup = c.getBoolean(s);
                        break;
                }
            }));
            STRING_LIST_PARSER.forEach((k,d) -> {
                List<String> list = getStringList(section,k);
                if (list != null) d.accept(this,list);
            });
        } else throw new IllegalStateException("Invalid statement.");
    }
    private void warn(String msg) {
        CutsceneMaker.warn(msg + " (Dialog " + name + " in file \"" + fileName + ".yml\")");
    }
    private ActionPredicate<Player> getQuestChecker(String name, String action) {
        QuestSet questSet = getQuestSet(name);
        if (questSet != null) {
            switch (action) {
                default:
                    warn("The quest checker \"" + name + "\" doesn't exist!");
                    warn("So it changed to \"complete\" automatically.");
                case "complete":
                    return questSet::isCompleted;
                case "ready":
                    return questSet::isReady;
                case "has":
                    return questSet::has;
                case "!complete":
                    return p -> !questSet.isCompleted(p);
                case "!ready":
                    return p -> !questSet.isReady(p);
                case "!has":
                    return p -> !questSet.has(p);
            }
        } else return null;
    }
    private Consumer<Player> getQuestConsumer(String key, String action) {
        QuestSet set = getQuestSet(key);
        if (set != null) {
            switch (action) {
                default:
                    warn("The quest action \"" + action + "\" doesn't exist!");
                    warn("So it changed to \"give\" automatically. ");
                case "give":
                    return set::give;
                case "complete":
                    return set::complete;
                case "remove":
                    return set::remove;
            }
        } else return null;
    }
    private QuestSet getQuestSet(String key) {
        QuestSet questSet = QuestData.QUEST_SET_MAP.get(key);
        if (questSet == null) warn("The QuestSet named \"" + key + "\" doesn't exist!");
        return questSet;
    }
    private <T> T random(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(0,list.size()));
    }
    private <T> T random(T[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(0,dialogs.length)];
    }

    public static boolean isRunning(Player player) {
        return CURRENT_TASK.containsKey(player);
    }
    public boolean run(@NotNull Player player, @NotNull String talker, @Nullable Consumer<Player> typingSound) {
        return run(player,talker,null,typingSound);
    }
    public boolean run(@NotNull Player player, @NotNull String talker, @Nullable Inventory inv, @Nullable Consumer<Player> typingSound) {
        Map<String,Consumer<Player>> soundMap;
        if (typingSounds != null) {
            soundMap = new WeakHashMap<>(typingSounds);
            if (typingSound != null) soundMap.put(talker, typingSound);
        } else if (typingSound != null) {
            soundMap = Collections.singletonMap(talker,typingSound);
        }
        else {
            soundMap = null;
        }
        return run(
                player,
                talker,
                inv,
                soundMap
        );
    }
    public boolean run(@NotNull Player player, @NotNull String talker, @Nullable Inventory inv, @Nullable Map<String,Consumer<Player>> typingSound) {
        if (isRunning(player)) return false;
        if (run(new DialogCurrent(player, talker, inv, typingSound))) {
            EvtUtil.call(new DialogStartEvent(player,this));
            return true;
        }
        else return false;
    }
    synchronized boolean run(DialogCurrent current) {
        if (conditions == null || conditions.test(current)) {
            current.cancelPickup = cancelPickup;
            current.cancelDamage = cancelDamage;
            if (giveItem != null) current.addGiveItem(giveItem);
            if (takeItem != null) current.addTakeItem(takeItem);
            CURRENT_TASK.put(current.player, new DialogRun(current));
        } else if (subDialog != null) return !isRunning(current.player) && random(subDialog).run(current);
        return isRunning(current.player);
    }
    private void addPredicate(Predicate<DialogCurrent> predicate) {
        if (conditions == null) conditions = predicate;
        else conditions = conditions.and(predicate);
    }
    private List<String> getStringList(ConfigurationSection section, String key) {
        return (section.isSet(key)) ? section.getStringList(key) : null;
    }

    @Override
    public int compareTo(@NotNull Dialog o) {
        return name.compareTo(o.name);
    }

    private class DialogRun implements Runnable {
        private final DialogCurrent current;
        private TypingExecutor executor;
        private int count;

        private DialogRun(DialogCurrent current) {
            this.current = current;
            load();
        }
        private void cancel() {
            stop();
            if (setQuest != null) setQuest.accept(current.player);
            if (endDialog == null || !random(endDialog).run(current)) {
                if (!addonList.isEmpty()) {
                    DialogAddon addon = random(addonList);
                    current.isOpened = !addon.isGui();
                    addon.run(current);
                }
                if (current.isOpened) {
                    current.player.closeInventory();
                    current.isOpened = false;
                }
                if (!exception) current.finish();
                EvtUtil.call(new DialogEndEvent(current.player,Dialog.this));
                if (actions != null) ActionData.start(random(actions), current.player);
            }
        }
        private void stop() {
            CURRENT_TASK.remove(current.player);
            endTask();
        }
        private void load() {
            if (count < records.length) {
                DialogRecord record = records[count];
                if (!record.printer.print(current.player).equals("skip")) {
                    if (record.typingManager != null) executor = record.typingManager.initialize(current);
                    else if (executor == null) executor = DEFAULT_TYPING_EXECUTOR.initialize(current);
                    init(record);
                    count++;
                } else {
                    count++;
                    load();
                }
            } else {
                cancel();
            }
        }
        private String message;
        private String output;
        private BukkitTask task;
        private Consumer<Player> soundActual;

        private int length;
        private int outputLength;
        private void init(DialogRecord record) {
            length = 0;
            outputLength = 0;
            this.message = record.invoke(current.player);
            char[] array = message.toCharArray();
            int size = 0;
            for (char c : array) {
                if (c != '*') size++;
            }
            char[] newArray = new char[size];
            int i = 0;
            for (char c : array) {
                if (c != '*') {
                    newArray[i++] = c;
                }
            }
            output = new String(newArray);

            String talker = (record.talker != null) ? record.talker.print(current.player) : current.talker;
            Consumer<Player> sound = (current.typingSound != null) ? current.typingSound.get(talker) : null;
            soundActual = (sound != null) ? sound : CutsceneConfig.getInstance().getDefaultTypingSound();

            executor.initialize(record,talker);
            start(current.time);
        }

        private void skip() {
            if (executor == null || output == null) return;
            executor.apply(ChatColor.WHITE + output,soundActual);
            endTask();
            load();
        }
        private void completeSkip() {
            if (executor == null) return;
            DialogRecord record = records[records.length - 1];
            executor.initialize(record,(record.talker != null) ? record.talker.print(current.player) : current.talker);
            executor.apply(ChatColor.WHITE + record.invoke(current.player).replace("*",""),soundActual);
            cancel();
        }
        private void restart() {
            if (message == null) return;
            if (length < message.length()) {
                endTask();
                start(current.time);
            }
        }
        private void start(long time) {
            task = manager.runTaskTimer(this,0,time);
        }
        @Override
        public void run() {
            if (length < message.length()) {
                length++;
                if (message.charAt(length -1) != '*') {
                    outputLength++;
                    char t = '§';
                    int i = 0;
                    while (message.charAt(length - 1) == t || (length >= 2 && message.charAt(length - 2) == t)) {
                        length = Math.min(length + 2,message.length());
                        i += 2;
                    }
                    outputLength = Math.min(outputLength + i,output.length());
                    executor.apply(ChatColor.WHITE + output.substring(0, outputLength), soundActual);
                }
            } else {
                endTask();
                task = manager.runTaskLater(this::load,20);
            }
        }
        private void endTask() {
            if (task != null) task.cancel();
        }
    }


    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConfigMapReader<T> {
        private final BiFunction<ConfigurationSection,String,? extends T> getter;
        private final Map<String, BiConsumer<DialogRecord,? super T>> taskMap = new HashMap<>();

        private void apply(Dialog dialog, ConfigurationSection section) {
            taskMap.forEach((s,c) -> forEach(dialog,section,s,c));
        }
        public void add(String name, BiConsumer<DialogRecord,? super T> action) {
            taskMap.putIfAbsent(name,action);
        }
        private void forEach(Dialog dialog, ConfigurationSection section, String key, BiConsumer<DialogRecord,? super T> consumer) {
            if (!section.isSet(key)) return;
            if (!section.isConfigurationSection(key)) {
                dialog.warn("Invalid syntax: " + key);
                return;
            }
            ConfigurationSection target = section.getConfigurationSection(key);
            for (String s : target.getKeys(false)) {
                try {
                    int i = Integer.parseInt(s);
                    DialogRecord record = (dialog.records.length >= i) ? dialog.records[i - 1] : null;
                    if (record != null) {
                        T get = getter.apply(target,s);
                        if (get != null) consumer.accept(record,get);
                        else dialog.warn("Unable to get value from key \"" + s + "\"!");
                    } else dialog.warn("The " + s + " line of Talk is not set!");
                } catch (Exception ignored) {
                    dialog.warn("This is not integer: " + s);
                }
            }
        }
    }
    public static class DialogCurrent {
        @Getter
        final Player player;
        @Getter
        final String talker;
        Inventory inventory;
        final Map<String ,Consumer<Player>> typingSound;
        boolean isOpened = false;
        int time = CutsceneConfig.getInstance().getDefaultTypingDelay();


        private boolean cancelDamage;
        private boolean cancelPickup;
        private List<ItemBuilder> takeItem;
        private List<ItemBuilder> giveItem;
        private DialogCurrent(Player player, String talker, Inventory inventory, Map<String,Consumer<Player>> typingSound) {
            this.player = player;
            this.talker = talker;
            this.inventory = inventory;
            this.typingSound = typingSound;
        }

        void addGiveItem(ItemBuilder... items) {
            if (giveItem == null) giveItem = new ArrayList<>(items.length);
            giveItem.addAll(Arrays.asList(items));
        }
        void addTakeItem(ItemBuilder... items) {
            if (takeItem == null) takeItem = new ArrayList<>(items.length);
            takeItem.addAll(Arrays.asList(items));
        }
        private void finish() {
            if (takeItem != null) {
                for (ItemBuilder builder : takeItem) {
                    InvUtil.take(player,builder.get(player));
                }
            }
            if (giveItem != null) {
                for (ItemBuilder builder : giveItem) {
                    InvUtil.give(player,builder.get(player));
                }
            }
        }
    }

    public class DialogRecord {
        private Consumer<Player> consumer;
        private final FunctionPrinter printer;
        private FunctionPrinter talker;
        private final Map<Integer, ItemBuilder> stacks = new HashMap<>();

        private TypingManager typingManager;
        public void warn(String s) {
            Dialog.this.warn(s);
        }

        private DialogRecord(String printer) {
            Matcher matcher = TALK_PATTERN.matcher(printer);
            if (matcher.find()) {
                String t = matcher.group("talker");
                if (t != null) talker = new FunctionPrinter(t);
                this.printer = new FunctionPrinter(matcher.group("content"));
            } else throw new RuntimeException("unable to read this talk syntax: " + printer);
        }
        public void addConsumer(Consumer<Player> playerConsumer) {
            if (consumer == null) consumer = playerConsumer;
            else consumer = consumer.andThen(playerConsumer);
        }
        private String invoke(Player player) {
            if (consumer != null) consumer.accept(player);
            return printer.print(player);
        }
    }

    private interface TypingManager {
        TypingExecutor initialize(DialogCurrent current);
    }
    private interface TypingExecutor {
        void initialize(DialogRecord record, String currentTalker);
        void apply(String message, Consumer<Player> soundPlay);
    }

    @Override
    public Editor getEditor(Player player) {
        return new DialogEditor(player);
    }
    private class DialogEditor extends Editor {
        private final ConfigurationSection resource = QuestUtil.copy(section);
        private TalkEditor[] talk;

        private String[] linkedDialog = getStringArray("LinkedDialog","Dialog");
        private String[] linkedSubDialog = getStringArray("LinkedSubDialog","SubDialog");
        private String[] linkedQnA = getStringArray("LinkedQnA","QnA");
        private String[] linkedPresent = getStringArray("LinkedPresent","Present");
        private String[] linkedAction = getStringArray("LinkedAction","Action");

        private String[] checkQuest = getStringArray("CheckQuest");
        private String[] setQuest = getStringArray("SetQuest");


        private String[] condition = getStringArray("Condition");
        private String[] setVars = getStringArray("SetVars");

        private int page = 1;
        private int totalPage = 1;

        private boolean exception = resource.getBoolean("Exception",false);

        private final ItemStack exceptionDisabled = write(
                new ItemStack(Material.EMERALD_BLOCK),
                "Exception",
                Arrays.asList(
                        "",
                        ChatColor.YELLOW + "Exception: " + ChatColor.GREEN + "Disabled",
                        "",
                        ChatColor.GRAY + "(Click: toggle Exception)"
                )
        );
        private final ItemStack exceptionEnabled = write(
                new ItemStack(Material.REDSTONE_BLOCK),
                "Exception",
                Arrays.asList(
                        "",
                        ChatColor.YELLOW + "Exception: " + ChatColor.RED + "Enabled",
                        "",
                        ChatColor.GRAY + "(Click: toggle Exception)"
                )
        );

        private String[] getStringArray(String... key) {
            return ConfigUtil.getStringList(resource,key).map(s -> s.toArray(new String[0])).orElse(null);
        }
        DialogEditor(Player player) {
            super(player, "Dialog");
            List<String> list = ConfigUtil.getStringList(resource,"Talk").orElse(null);
            if (list != null) {
                talk = new TalkEditor[list.size()];
                int i = 0;
                TalkEditor editor;
                ConfigurationSection items = ConfigUtil.getConfig(resource,"Item").orElse(null);
                ConfigurationSection talkers = ConfigUtil.getConfig(resource,"Talker").orElse(null);
                ConfigurationSection sounds = ConfigUtil.getConfig(resource,"Sound").orElse(null);
                ConfigurationSection interfaces = ConfigUtil.getConfig(resource,"Interface").orElse(null);
                for (String s : list) {
                    String key = Integer.toString(i + 1);

                    editor = new TalkEditor();

                    Matcher matcher = TALK_PATTERN.matcher(s);
                    if (matcher.find()) {
                        editor.talk = matcher.group("content");
                        String t = matcher.group("talker");
                        editor.talker = (t != null) ? t : (talkers != null) ? ConfigUtil.getString(talkers,key).orElse(null) : null;
                    }
                    editor.sound = (sounds != null) ? ConfigUtil.getString(sounds,key).orElse(null) : null;
                    editor.typing = (interfaces != null) ? ConfigUtil.getString(interfaces,key).orElse(null) : null;
                    editor.item = (items != null) ? ConfigUtil.getConfig(items,key).orElse(null) : null;
                    talk[i++] = editor;
                }
            } else {
                talk = new TalkEditor[1];
                TalkEditor e = new TalkEditor();
                e.talk = "a new talk";
                talk[0] = e;
            }
        }
        private void extend(int index) {
            TalkEditor n = new TalkEditor();
            n.talk = "a new talk";
            extend(index,n);
        }
        private void extend(int index, TalkEditor add) {
            TalkEditor[] editors = new TalkEditor[talk.length + 1];
            int i = 0;
            for (TalkEditor editor : talk) {
                editors[i++] = editor;
                if (i > index) break;
            }
            editors[i++] = add;
            if (i < editors.length) {
                for (; i < editors.length; i++) {
                    editors[i] = talk[i - 1];
                }
            }
            talk = editors;
            setupPage();
        }
        private void reduce(int index) {
            if (index == 0) {
                talk[0].talk = "skip";
                return;
            }
            talk[index] = null;
            talk = Arrays.stream(talk).filter(Objects::nonNull).toArray(TalkEditor[]::new);
            setupPage();
        }
        private void setupPage() {
            totalPage = Math.max((int)Math.ceil((double) talk.length/36D),1);
            ItemStack button = new ItemStack(Material.STONE_BUTTON);
            inv.setItem(46,write(
                    button,
                    "previous page",
                    Collections.singletonList("Page : " + page + " / " + totalPage)
            ));
            inv.setItem(52,write(
                    button,
                    "next page",
                    Collections.singletonList("Page : " + page + " / " + totalPage)
            ));
        }
        private void dialogUpdate() {
            manager.runTaskLater(() -> {
                resetInv();
                updateGui();
            },5);
        }
        private String write(String target) {
            return (target != null) ? ChatColor.WHITE + TextUtil.colored(target) : ChatColor.GRAY + "<none>";
        }
        private ItemStack write(ItemStack target, String name, List<String> lore) {
            ItemMeta meta = target.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + name);
            if (lore != null) meta.setLore(write(lore));
            target.setItemMeta(meta);
            return target;
        }
        private List<String> write(List<String> target) {
            return (!target.isEmpty()) ? target
                    .stream()
                    .map(s -> ChatColor.GRAY + " - " + ChatColor.WHITE + TextUtil.colored(s))
                    .collect(Collectors.toList()) : Collections.singletonList(ChatColor.GRAY + "<none>");
        }


        private class TalkEditor {
            private String talk, talker, sound, typing;
            private ConfigurationSection item;

            private void reopen() {
                manager.runTaskLater(this::open,5);
            }
            private List<String> getItemList() {
                return (item != null) ? item.getKeys(false)
                        .stream()
                        .map(l -> {
                            ItemBuilder builder = InvUtil.fromConfig(item,l);
                            if (builder != null) return l + ": " + TextUtil.getItemName(builder.get(player));
                            else return ChatColor.YELLOW + "<error!>";
                        })
                        .collect(Collectors.toList()) : Collections.emptyList();
            }
            private void open() {
                Inventory sub = InvUtil.create(invName + ": Talk",3);
                sub.setItem(9,getItem(Material.BOOK,"Talk",talk));
                sub.setItem(11,getItem(Material.PAPER,"Talker",talker));
                sub.setItem(13,getItem(Material.NOTE_BLOCK,"Sound",sound));
                sub.setItem(15,getItem(WrappedMaterial.getWrapper().getCommandBlock(),"Interface",typing));
                sub.setItem(26,write(
                        new ItemStack(Material.STONE_BUTTON),
                        "Back",
                        null
                ));
                sub.setItem(17,write(
                        new ItemStack(Material.CHEST),
                        "Item",
                        getItemList()
                ));
                GuiRegister.registerNewGui(new GuiAdapter(player,manager,sub) {
                    @Override
                    public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                        switch (slot) {
                            case 26:
                                resetInv();
                                updateGui();
                                break;
                            case 9:
                                chatTask(new String[]{
                                        ChatColor.YELLOW + "enter a value in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\"",
                                        ChatColor.GOLD + "format: " + ChatColor.GREEN + "<talk>" + ChatColor.WHITE + " or " + ChatColor.GREEN + "<talker: talk>",
                                        ChatColor.GOLD + "function format: " + ChatColor.GREEN + "%function[]%"
                                },e -> {
                                    CutsceneMaker.send(player,"successfully changed.");
                                    Matcher matcher = TALK_PATTERN.matcher(e);
                                    if (matcher.find()) {
                                        talk = matcher.group("content");
                                        talker = matcher.group("talker");
                                    }
                                });
                                break;
                            case 11:
                                chatTask(new String[]{
                                        ChatColor.YELLOW + "enter a value in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\"",
                                        ChatColor.GOLD + "format: " + ChatColor.GREEN + "<talker>",
                                        ChatColor.GOLD + "function format: " + ChatColor.GREEN + "%function[]%",
                                        ChatColor.GOLD + "remove: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "null" + ChatColor.WHITE + "\""
                                },e -> {
                                    CutsceneMaker.send(player,"successfully changed.");
                                    talker = (e.equals("null")) ? null : e;
                                });
                                break;
                            case 13:
                                chatTask(new String[]{
                                        ChatColor.YELLOW + "enter a value in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\"",
                                        ChatColor.GOLD + "format: " + ChatColor.GREEN + "<sound name> <volume> <pitch>",
                                        ChatColor.GOLD + "remove: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "null" + ChatColor.WHITE + "\""
                                },e -> {
                                    CutsceneMaker.send(player,"successfully changed.");
                                    sound = (e.equals("null")) ? null : e;
                                });
                                break;
                            case 15:
                                chatTask(new String[]{
                                        ChatColor.YELLOW + "enter a value in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\"",
                                        ChatColor.GOLD + "format: " + ChatColor.GREEN + "<interface name>",
                                        ChatColor.GOLD + "example: " + ChatColor.GREEN + "default",
                                        ChatColor.GOLD + "remove: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "null" + ChatColor.WHITE + "\""
                                },e -> {
                                    String t = e.toLowerCase();
                                    boolean isNull = e.equals("null");
                                    if (!isNull && !TYPING_MANAGER_MAP.containsKey(t)) {
                                        CutsceneMaker.send(player,"The Interface named \"" + t + "\" doesn't exist!");
                                    } else {
                                        CutsceneMaker.send(player,"successfully changed.");
                                        typing = (isNull) ? null : e;
                                    }
                                });
                                break;
                            case 17:
                                Inventory cal = InvUtil.create("Put your item in here!",CutsceneConfig.getInstance().getDefaultDialogRows());
                                cal.setItem(
                                        CutsceneConfig.getInstance().getDefaultDialogCenter(),
                                        write(
                                                new ItemStack(Material.BARRIER),
                                                "Prevention",
                                                Collections.singletonList("you can't put any item here!")
                                        )
                                );
                                ConfigurationSection t = TalkEditor.this.item;
                                if (t != null) t.getKeys(false).forEach(s -> {
                                    ItemBuilder builder = InvUtil.fromConfig(t,s);
                                    if (builder != null) {
                                        try {
                                            cal.setItem(
                                                    Integer.parseInt(s),
                                                    builder.get()
                                            );
                                        } catch (Exception ignored) {}
                                    }
                                });
                                CallbackManager.callbackInventory(
                                        player,
                                        cal,
                                        m -> {
                                            ConfigurationSection sec = new MemoryConfiguration();
                                            m.forEach((k,v) -> {
                                                String q = Integer.toString(k);
                                                if (v.getType() != Material.AIR) sec.set(q,v);
                                            });
                                            TalkEditor.this.item = (sec.getKeys(false).isEmpty()) ? null : sec;
                                            CutsceneMaker.send(player,"successfully changed.");
                                            reopen();
                                        }
                                );
                                break;
                        }
                    }
                    private void chatTask(String[] message, Consumer<String> consumer) {
                        CallbackManager.callbackChat(
                                player,
                                message,
                                strings -> {
                                    String s = strings[0];
                                    if (s.equals("cancel")) {
                                        CutsceneMaker.send(player,"task cancelled.");
                                        reopen();
                                    } else {
                                        consumer.accept(s);
                                        reopen();
                                    }
                                }
                        );
                    }
                });
            }
            private ItemStack getItem(Material material, String name, String target) {
                return write(
                        new ItemStack(material),
                        name,
                        (target != null) ? Collections.singletonList(target) : Collections.emptyList()
                );
            }
        }
        private Inventory inv;

        public void resetInv() {
            for (int t = 0; t < 45; t++) {
                inv.setItem(t,null);
            }
            int i = 0;
            final ItemStack talkItem = new ItemStack(Material.BOOK);
            final ItemMeta meta = talkItem.getItemMeta();
            int p = (page - 1) * 36;
            if (talk != null) for (int t = p; t < Math.min(page * 36,talk.length); t++) {
                TalkEditor s = talk[t];
                meta.setDisplayName(ChatColor.WHITE + "Talk: " + (i + 1 + p));
                List<String> list = new ArrayList<>();
                list.add(ChatColor.WHITE + write(s.talk));
                list.add("");
                list.add(ChatColor.YELLOW + "Talker: " + write(s.talker));
                list.add(ChatColor.YELLOW + "Sound: " + write(s.sound));
                list.add(ChatColor.YELLOW + "Interface: " + write(s.typing));
                list.add(ChatColor.YELLOW + "Item:");
                list.addAll(write(s.getItemList()));
                list.add("");
                list.add(ChatColor.GRAY + "(Left: rewrite this Talk)");
                list.add(ChatColor.GRAY + "(Shift+Left: create new Talk)");
                list.add(ChatColor.GRAY + "(Right: copy this Talk");
                list.add(ChatColor.GRAY + "(Shift+Right: delete this Talk)");
                meta.setLore(list);
                talkItem.setItemMeta(meta);
                inv.setItem(9 + i++,talkItem);
            }
            ItemStack iron = new ItemStack(Material.IRON_INGOT);
            ItemStack gold = new ItemStack(Material.GOLD_INGOT);
            ItemStack emerald = new ItemStack(Material.EMERALD);
            inv.setItem(47,write(
                    iron,
                    "Linked Dialog",
                    toList(linkedDialog)
            ));
            inv.setItem(48,write(
                    iron,
                    "Linked SubDialog",
                    toList(linkedSubDialog)
            ));
            inv.setItem(49,write(
                    iron,
                    "Linked QnA",
                    toList(linkedQnA)
            ));
            inv.setItem(50,write(
                    iron,
                    "Linked Present",
                    toList(linkedPresent)
            ));
            inv.setItem(51,write(
                    iron,
                    "Linked Action",
                    toList(linkedAction)
            ));
            inv.setItem(0,write(
                    gold,
                    "Check Quest",
                    toList(checkQuest)
            ));
            inv.setItem(1,write(
                    gold,
                    "Set Quest",
                    toList(setQuest)
            ));
            inv.setItem(2,write(
                    emerald,
                    "Condition",
                    toList(condition)
            ));
            inv.setItem(3,write(
                    emerald,
                    "Set Vars",
                    toList(setVars)
            ));
            ItemStack gpt = new ItemStack(WrappedMaterial.getWrapper().getCommandBlock());
            ItemMeta gptMeta = gpt.getItemMeta();
            NBTReflector.edit(gptMeta,ChatColor.GOLD + "Call ChatGPT",Collections.singletonList(ChatColor.GRAY + "(Left: add random dialog with ChatGPT)"));
            gpt.setItemMeta(gptMeta);
            inv.setItem(7,gpt);
            setupException();
        }
        private void toggleException() {
            exception = !exception;
            setupException();
        }
        private void setupException() {
            inv.setItem(8,(exception) ? exceptionEnabled : exceptionDisabled);
        }
        private <T> List<T> toList(T[] array) {
            return (array != null) ? Arrays.asList(array) : Collections.emptyList();
        }
        @Override
        public GuiExecutor getMainExecutor() {
            if (inv == null) {
                inv = InvUtil.create(invName, 6);
                resetInv();
            }
            setupPage();
            return new GuiAdapter(player,manager,inv) {
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (isPlayerInventory) return;
                    if (item.getType() == Material.BOOK) {
                        int i = slot - 9 + ((page -1) * 36);
                        switch (button) {
                            case LEFT:
                                talk[i].open();
                                break;
                            case LEFT_WITH_SHIFT:
                                extend(i);
                                resetInv();
                                break;
                            case RIGHT:
                                TalkEditor editor = new TalkEditor();
                                TalkEditor target = talk[i];
                                editor.talk = target.talk;
                                editor.typing = target.typing;
                                editor.talker = target.talker;
                                editor.item = (target.item != null) ? QuestUtil.copy(target.item) : null;
                                extend(i,editor);
                                resetInv();
                                break;
                            case RIGHT_WITH_SHIFT:
                                reduce(i);
                                resetInv();
                                break;
                        }
                    } else {
                        switch (slot) {
                            case 7:
                                String key = CutsceneConfig.getInstance().getOpenAPIKey();
                                if (key == null) {
                                    CutsceneMaker.send(player,"you have to write your api key in config.yml!");
                                    return;
                                }
                                CallbackManager.openSign(
                                        player,
                                        new String[] {
                                                "",
                                                "write the NPC's job!",
                                                "",
                                                ""
                                        },
                                        s -> {
                                            if (s[0].equals("")) {
                                                CutsceneMaker.send(player,"value cannot be empty string!");
                                                dialogUpdate();
                                            } else {
                                                manager.runTaskAsynchronously(() -> {
                                                    TalkEditor editor = new TalkEditor();
                                                    String k = TalkGenerator.generate(key,s[0]);
                                                    if (k == null) {
                                                        CutsceneMaker.send(player,"invalid api key!");
                                                        dialogUpdate();
                                                        return;
                                                    }
                                                    editor.talk = k;
                                                    extend(talk.length - 1,editor);
                                                    dialogUpdate();
                                                });
                                            }
                                        }
                                );
                                break;
                            case 46:
                                page = Math.max(page - 1, 1);
                                setupPage();
                                resetInv();
                                break;
                            case 8:
                                toggleException();
                                break;
                            case 0:
                                signTask(
                                        button,
                                        new String[] {
                                                "",
                                                "",
                                                "1: QuestSet's name",
                                                "2: has/complete/ready"
                                        },
                                        s -> {
                                            if (s[1].equals("")) s[1] = "complete";
                                            if (!QuestData.QUEST_SET_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The QuestSet named \"" + s[0] + "\" doesn't exist!");
                                            } else checkQuest = QuestUtil.plusElement(checkQuest,s[0] + " " + s[1]);
                                        },
                                        () -> checkQuest = QuestUtil.deleteLast(checkQuest)
                                );
                                break;
                            case 1:
                                signTask(
                                        button,
                                        new String[] {
                                                "",
                                                "",
                                                "1: QuestSet's name",
                                                "2: give/complete/remove"
                                        },
                                        s -> {
                                            if (s[1].equals("")) s[1] = "complete";
                                            if (!QuestData.QUEST_SET_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The QuestSet named \"" + s[0] + "\" doesn't exist!");
                                            } else setQuest = QuestUtil.plusElement(setQuest,s[0] + " " + s[1]);
                                        },
                                        () -> setQuest = QuestUtil.deleteLast(setQuest)
                                );
                                break;
                            case 2:
                                signTask(
                                        button,
                                        new String[] {
                                                "",
                                                "<fun> <operator> <fun>",
                                                "ex) name[] == toxicity",
                                                "ex2) health[] > num[_var]"
                                        },
                                        s -> {
                                            String[] t = TextUtil.split(s[0]," ");
                                            if (t.length > 2 && ConditionBuilder.LIVING_ENTITY.find(t) != null) {
                                                condition = QuestUtil.plusElement(condition,s[0]);
                                            } else CutsceneMaker.send(player,"Invalid syntax: " + s[0]);
                                        },
                                        () -> condition = QuestUtil.deleteLast(condition)
                                );
                                break;
                            case 3:
                                signTask(
                                        button,
                                        new String[] {
                                                "",
                                                "<name> <operator> <fun/value>",
                                                "ex) _var + randomInt[1,100]",
                                                "ex2) _var del"
                                        },
                                        s -> {
                                            String[] t = TextUtil.split(s[0]," ");
                                            if (t.length > 2 && QuestUtil.getVarsConsumer(t[0],t[1],t[2]) != null) {
                                                setVars = QuestUtil.plusElement(setVars,s[0]);
                                            } else CutsceneMaker.send(player,"Invalid syntax: " + s[0]);
                                        },
                                        () -> setVars = QuestUtil.deleteLast(setVars)
                                );
                                break;
                            case 47:
                                signTask(
                                        button,
                                        "write the Dialog's name!",
                                        s -> {
                                            if (!QuestData.DIALOG_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The Dialog named \"" + s[0] + "\" doesn't exist!");
                                            } else linkedDialog = QuestUtil.plusElement(linkedDialog,s[0]);
                                        },
                                        () -> linkedDialog = QuestUtil.deleteLast(linkedAction)
                                );
                                break;
                            case 48:
                                signTask(
                                        button,
                                        "write the Dialog's name!",
                                        s -> {
                                            if (!QuestData.DIALOG_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The Dialog named \"" + s[0] + "\" doesn't exist!");
                                            } else linkedSubDialog = QuestUtil.plusElement(linkedSubDialog,s[0]);
                                        },
                                        () -> linkedSubDialog = QuestUtil.deleteLast(linkedSubDialog)
                                );
                                break;
                            case 49:
                                signTask(
                                        button,
                                        "write the QnA's name!",
                                        s -> {
                                            if (!QuestData.QNA_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The QnA named \"" + s[0] + "\" doesn't exist!");
                                            } else linkedQnA = QuestUtil.plusElement(linkedQnA,s[0]);
                                        },
                                        () -> linkedQnA = QuestUtil.deleteLast(linkedQnA)
                                );
                                break;
                            case 50:
                                signTask(
                                        button,
                                        "write the Present's name!",
                                        s -> {
                                            if (!QuestData.PRESENT_MAP.containsKey(s[0])) {
                                                CutsceneMaker.send(player,"The Present named \"" + s[0] + "\" doesn't exist!");
                                            } else linkedPresent = QuestUtil.plusElement(linkedPresent,s[0]);
                                        },
                                        () -> linkedPresent = QuestUtil.deleteLast(linkedPresent)
                                );
                                break;
                            case 51:
                                signTask(
                                        button,
                                        "write the Action's name!",
                                        s -> linkedAction = QuestUtil.plusElement(linkedAction,s[0]),
                                        () -> linkedAction = QuestUtil.deleteLast(linkedAction)
                                );
                                break;
                            case 52:
                                page = Math.min(page + 1,totalPage);
                                setupPage();
                                resetInv();
                                break;
                        }
                    }
                }
            };
        }
        private void signTask(MouseButton button, String msg, Consumer<String[]> callback, Runnable resizeArray) {
            signTask(button,new String[] {"",msg,"","",},callback,resizeArray);
        }
        private void signTask(MouseButton button, String[] msg, Consumer<String[]> callback, Runnable resizeArray) {
            switch (button) {
                case LEFT:
                    CallbackManager.openSign(
                            player,
                            msg,
                            s -> {
                                if (s[0].equals("")) {
                                    CutsceneMaker.send(player,"value cannot be empty string!");
                                } else {
                                    callback.accept(s);
                                }
                                dialogUpdate();
                            }
                    );
                    break;
                case RIGHT:
                    resizeArray.run();
                    resetInv();
                    break;
            }
        }

        @Override
        public ConfigurationSection getSaveData() {
            resource.set("Talk", Arrays.stream(talk).map(t -> t.talk).toArray(String[]::new));

            //Talk Configuration
            ConfigurationSection sounds = new MemoryConfiguration();
            ConfigurationSection interfaces = new MemoryConfiguration();
            ConfigurationSection talker = new MemoryConfiguration();
            ConfigurationSection items = new MemoryConfiguration();
            int i = 0;
            for (TalkEditor editor : talk) {
                String key = Integer.toString(++i);
                talker.set(key,editor.talker);
                sounds.set(key,editor.sound);
                interfaces.set(key,editor.typing);
                items.set(key,editor.item);
            }
            resource.set("Sound", (sounds.getKeys(false).isEmpty()) ? null : sounds);
            resource.set("Interface",(interfaces.getKeys(false).isEmpty()) ? null : interfaces);
            resource.set("Talker",(talker.getKeys(false).isEmpty()) ? null : talker);
            resource.set("Item",(items.getKeys(false).isEmpty()) ? null : items);

            //Linked Source Configuration
            resource.set("Dialog",linkedDialog);
            resource.set("SubDialog",linkedSubDialog);
            resource.set("QnA",linkedQnA);
            resource.set("Present",linkedPresent);
            resource.set("Action",linkedAction);

            //Variable Configuration
            resource.set("SetVars",setVars);
            resource.set("Condition",condition);

            //Quest Configuration
            resource.set("CheckQuest",checkQuest);
            resource.set("SetQuest",setQuest);
            resource.set("Exception",(exception) ? true : null);
            return resource;
        }
    }
}
