package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.events.DialogEndEvent;
import kor.toxicity.cutscenemaker.events.DialogStartEvent;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ActionPredicate;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Dialog {

    private static final TypingManager DEFAULT_TYPING_EXECUTOR = current -> {
        if (current.inventory == null) current.inventory = InvUtil.getInstance().create(current.talker + "'s dialog",CutsceneConfig.getInstance().getDefaultDialogRows());
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
    public static final ConfigMapReader<String> READER_STRING = new ConfigMapReader<>(ConfigurationSection::getString);
    public static final ConfigMapReader<ConfigurationSection> READER_CONFIGURATION = new ConfigMapReader<>((c, k) -> (c.isConfigurationSection(k)) ? c.getConfigurationSection(k) : null);

    private static final Map<String,TypingManager> TYPING_MANAGER_MAP = new HashMap<>();
    private static final Map<Player,DialogRun> CURRENT_TASK = new ConcurrentHashMap<>();
    static final List<Runnable> LATE_CHECK = new ArrayList<>();
    static void stopAll(CutsceneMaker plugin) {
        plugin.getManager().runTaskLater(() -> {
            new WeakHashMap<>(CURRENT_TASK).forEach((p, d) -> {
                d.cancel();
                if (d.current.isOpened) p.closeInventory();
            });
            CURRENT_TASK.clear();
        },0);
    }

    static {
        TYPING_MANAGER_MAP.put("default", DEFAULT_TYPING_EXECUTOR);
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

        READER_STRING.add("Talker",(d,s) -> d.talker = new FunctionPrinter(s));
        READER_STRING.add("Interface",(d,s) -> {
            d.typingManager = TYPING_MANAGER_MAP.get(s);
            if (d.typingManager == null) CutsceneMaker.warn("The Interface named \"" + s + "\" doesn't exist!");
        });
        READER_STRING.add("Sound",(d,s) -> d.addConsumer(QuestUtil.getInstance().getSoundPlay(s)));
        READER_CONFIGURATION.add("Item",(d,c) -> c.getKeys(false).forEach(s -> {
            try {
                int i = Integer.parseInt(s);
                CutsceneConfig config = CutsceneConfig.getInstance();
                if (i != config.getDefaultDialogCenter() && i < config.getDefaultDialogRows() * 9) {
                    ItemBuilder builder = InvUtil.getInstance().fromConfig(c,s);
                    if (builder != null) d.stacks.put(i, builder);
                }
            } catch (Exception e) {
                CutsceneMaker.warn("fail to load the item data: " + s);
            }
        }));
    }
    private final CutsceneManager manager;
    private final DialogRecord[] records;
    private Dialog[] subDialog;
    private Dialog[] endDialog;
    private String[] actions;
    private QnA[] endQnA;

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
                    if (run != null) e.setCancelled(true);
                }
            }
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                DialogRun run = getDialogRun(e.getPlayer());
                if (run != null) run.stop();
            }
            private final Map<Player,BukkitTask> delay = new HashMap<>();
            @EventHandler
            public void onInvClick(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    DialogRun run = getDialogRun(p);
                    if (run != null) {
                        e.setCancelled(true);
                        Inventory clickedInventory = e.getClickedInventory();
                        if (clickedInventory != null && clickedInventory.equals(run.current.inventory) && e.getSlot() == CutsceneConfig.getInstance().getDefaultDialogCenter() && !delay.containsKey(p)) {
                            delay.put(p,plugin.getManager().runTaskLaterAsynchronously(() -> delay.remove(p), 4));
                            if (e.isLeftClick()) {
                                run.current.time = Math.max(run.current.time - 1, 1);
                                run.reader.restart(run.current.time);
                            }
                            if (e.isRightClick()) {
                                run.current.time = Math.min(run.current.time + 1, 4);
                                run.reader.restart(run.current.time);
                            }
                        }
                    }
                }
            }
        });
    }

    Dialog(CutsceneManager manager, ConfigurationSection section) {
        this.manager = manager;
        List<String> talk = getStringList(section,"Talk");
        if (talk != null) {
            records = talk.stream().map(s -> {
                try {
                    return new DialogRecord(s);
                } catch (Exception e) {
                    CutsceneMaker.warn("Error: " + e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull).toArray(DialogRecord[]::new);

            READER_STRING.apply(this,section);
            READER_CONFIGURATION.apply(this,section);

            if (section.isSet("TypingSound") && section.isConfigurationSection("TypingSound")) {
                ConfigurationSection typing = section.getConfigurationSection("TypingSound");
                typingSounds = typing.getKeys(false).stream().collect(Collectors.toMap(s -> s.replace("_"," "), s -> QuestUtil.getInstance().getSoundPlay(typing.getString(s))));
            }

            getOptionalList(section,"Condition").ifPresent(t -> LATE_CHECK.add(() -> t.forEach(s -> {
                String[] cond = TextUtil.getInstance().split(s," ");
                ActionPredicate<LivingEntity> check = (cond.length >= 3) ? ConditionBuilder.LIVING_ENTITY.find(cond) : null;
                if (check != null) {
                    if (cond.length > 3) {
                        Dialog dialog = QuestUtil.getInstance().getDialog(cond[3]);
                        if (dialog != null) addPredicate(d -> check.castInstead(p -> dialog.run(d)).test(d.player));
                    } else addPredicate(d -> check.test(d.player));
                }
            })));
            getOptionalList(section,"CheckQuest").ifPresent(t -> LATE_CHECK.add(() -> t.forEach(s -> {
                String[] args = TextUtil.getInstance().split(s," ");
                ActionPredicate<Player> predicate = getQuestChecker(args[0],(args.length > 1) ? args[1].toLowerCase() : "complete");
                if (predicate != null) {
                    if (args.length > 2) {
                        Dialog dialog = QuestUtil.getInstance().getDialog(args[2]);
                        if (dialog != null) addPredicate(d -> predicate.castInstead(p -> dialog.run(d)).test(d.player));
                    } else addPredicate(d -> predicate.test(d.player));
                }
            })));
            getOptionalList(section,"LinkedDialog").ifPresent(t -> LATE_CHECK.add(() -> endDialog = QuestUtil.getInstance().getDialog(t)));
            getOptionalList(section,"LinkedSubDialog").ifPresent(t -> LATE_CHECK.add(() -> subDialog = QuestUtil.getInstance().getDialog(t)));
            getOptionalList(section,"LinkedAction").ifPresent(t -> actions = t.toArray(new String[0]));
            getOptionalList(section,"SetQuest").ifPresent(t -> t.stream().map(s -> {
                String[] a = TextUtil.getInstance().split(s," ");
                return getQuestConsumer(a[0],(a.length > 1) ? a[1].toLowerCase() : "give");
            }).filter(Objects::nonNull).forEach(c -> setQuest = setQuest.andThen(c)));
            getOptionalList(section,"SetVars").ifPresent(t -> t.stream().map(s -> {
                String[] a = TextUtil.getInstance().split(s," ");
                Consumer<Player> vars;
                if (a.length > 1) {
                    vars = QuestUtil.getInstance().getVarsConsumer(a[0],(a.length > 2) ? a[2] : null,a[1]);
                } else vars = null;
                if (vars == null) CutsceneMaker.warn("unable to load this variable operation: \"" + s + "\"");
                return vars;
            }).filter(Objects::nonNull).forEach(c -> setQuest = setQuest.andThen(c)));
            getOptionalList(section,"LinkedQnA").ifPresent(t -> LATE_CHECK.add(() -> {
                QnA[] qna = t.stream().map(s -> {
                    QnA q = QuestData.QNA_MAP.get(s);
                    if (q == null) CutsceneMaker.warn("the QnA named \"" + s + "\" doesn't exist!");
                    return q;
                }).filter(Objects::nonNull).toArray(QnA[]::new);
                if (qna.length > 0) endQnA = qna;
            }));
        } else throw new IllegalStateException("Invalid statement.");
    }
    private ActionPredicate<Player> getQuestChecker(String name, String action) {
        QuestSet questSet = getQuestSet(name);
        if (questSet != null) {
            switch (action) {
                default:
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
        if (questSet == null) CutsceneMaker.warn("The QuestSet named \"" + key + "\" doesn't exist!");
        return questSet;
    }
    private <T> T random(T[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(0,dialogs.length)];
    }

    public void run(@NotNull Player player, @NotNull String talker, @Nullable Consumer<Player> typingSound) {
        run(player,talker,null,typingSound);
    }
    public void run(@NotNull Player player, @NotNull String talker, @Nullable Inventory inv, @Nullable Consumer<Player> typingSound) {
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
        run(
                player,
                talker,
                inv,
                soundMap
        );
    }
    public void run(Player player, String talker, Inventory inv, Map<String,Consumer<Player>> typingSound) {
        DialogStartEvent event = new DialogStartEvent(player,this);
        EvtUtil.call(event);
        if (!event.isCancelled()) {
            run(new DialogCurrent(player, talker, inv, typingSound));
        }
    }
    synchronized boolean run(DialogCurrent current) {
        if (CURRENT_TASK.containsKey(current.player)) return false;
        if (conditions == null || conditions.test(current)) {
            CURRENT_TASK.put(current.player, new DialogRun(current));
        } else if (subDialog != null) return random(subDialog).run(current);
        return true;
    }
    private void addPredicate(Predicate<DialogCurrent> predicate) {
        if (conditions == null) conditions = predicate;
        else conditions = conditions.and(predicate);
    }
    private List<String> getStringList(ConfigurationSection section, String key) {
        return (section.isSet(key)) ? section.getStringList(key) : null;
    }
    private Optional<List<String>> getOptionalList(ConfigurationSection section, String key) {
        return Optional.ofNullable(getStringList(section,key));
    }
    private class DialogRun {
        private final DialogCurrent current;
        private final DialogReader reader = new DialogReader();
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
                if (endQnA != null) {
                    random(endQnA).run(current);
                    current.isOpened = false;
                    return;
                }
                if (current.isOpened) {
                    current.player.closeInventory();
                    current.isOpened = false;
                }
                EvtUtil.call(new DialogEndEvent(current.player,Dialog.this));
                if (actions != null) ActionData.start(random(actions), current.player);
            }
        }
        private void stop() {
            CURRENT_TASK.remove(current.player);
            reader.cancel();
        }
        private void load() {
            if (count < records.length) {
                DialogRecord record = records[count];
                if (!record.printer.print(current.player).equals("skip")) {
                    if (record.typingManager != null) executor = record.typingManager.initialize(current);
                    else if (executor == null) executor = DEFAULT_TYPING_EXECUTOR.initialize(current);
                    reader.initialize(record);
                    count++;
                } else {
                    count++;
                    load();
                }
            } else {
                cancel();
            }
        }
        private class DialogReader implements Runnable {
            private String message;
            private String output;
            private BukkitTask task;
            private Consumer<Player> soundActual;

            private int length;
            private int outputLength;
            private void initialize(DialogRecord record) {
                length = 0;
                outputLength = 0;
                this.message = record.invoke(current.player);
                char[] array = message.toCharArray();
                int size = 0;
                for (char c : array) {
                    if (c != '*') size ++;
                }
                char[] newArray = new char[size];
                int i = 0;
                for (char c : array) {
                    if (c != '*') {
                        newArray[i] = c;
                        i++;
                    }
                }
                output = new String(newArray);

                String talker = (record.talker != null) ? record.talker.print(current.player) : current.talker;
                Consumer<Player> sound = (current.typingSound != null) ? current.typingSound.get(talker) : null;
                soundActual = (sound != null) ? sound : CutsceneConfig.getInstance().getDefaultTypingSound();

                executor.initialize(record,talker);
                start(current.time);
            }

            private void restart(long time) {
                if (length < message.length()) {
                    cancel();
                    start(time);
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
                    cancel();
                    task = manager.runTaskLater(DialogRun.this::load,20);
                }
            }
            private void cancel() {
                if (task != null) task.cancel();
            }
        }
    }


    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConfigMapReader<T> {
        private final BiFunction<ConfigurationSection,String,T> getter;
        private final Map<String, BiConsumer<DialogRecord,T>> taskMap = new HashMap<>();

        private void apply(Dialog dialog, ConfigurationSection section) {
            taskMap.forEach((s,c) -> forEach(dialog,section,s,c));
        }
        public void add(String name, BiConsumer<DialogRecord,T> action) {
            taskMap.putIfAbsent(name,action);
        }
        private void forEach(Dialog dialog, ConfigurationSection section, String key, BiConsumer<DialogRecord,T> consumer) {
            if (section.isSet(key) && section.isConfigurationSection(key)) {
                ConfigurationSection target = section.getConfigurationSection(key);
                for (String s : target.getKeys(false)) {
                    try {
                        int i = Integer.parseInt(s);
                        Optional.ofNullable((dialog.records.length >= i) ? dialog.records[i - 1] : null).ifPresent(d -> {
                            T get = getter.apply(target,s);
                            if (get != null) consumer.accept(d,get);
                        });
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
    static final class DialogCurrent {
        final Player player;
        final String talker;
        Inventory inventory;
        final Map<String ,Consumer<Player>> typingSound;
        boolean isOpened = false;
        int time = CutsceneConfig.getInstance().getDefaultTypingDelay();
        private DialogCurrent(Player player, String talker, Inventory inventory, Map<String,Consumer<Player>> typingSound) {
            this.player = player;
            this.talker = talker;
            this.inventory = inventory;
            this.typingSound = typingSound;
        }
    }

    public static final class DialogRecord {
        private Consumer<Player> consumer;
        private final FunctionPrinter printer;
        private FunctionPrinter talker;
        private final Map<Integer, ItemBuilder> stacks = new HashMap<>();

        private TypingManager typingManager;

        private DialogRecord(String printer) {
            Matcher matcher = TALK_PATTERN.matcher(printer);
            if (matcher.find()) {
                String t = matcher.group("talker");
                if (t != null) talker = new FunctionPrinter(t);
                this.printer = new FunctionPrinter(matcher.group("content"));
            } else throw new RuntimeException("unable to read talk statement:" + printer);
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
}
