package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.events.DialogEndEvent;
import kor.toxicity.cutscenemaker.events.DialogStartEvent;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Dialog {

    public static final ConfigMapReader<String> READER_STRING = new ConfigMapReader<>(ConfigurationSection::getString);
    public static final ConfigMapReader<ConfigurationSection> READER_CONFIGURATION = new ConfigMapReader<>((c, k) -> (c.isConfigurationSection(k)) ? c.getConfigurationSection(k) : null);

    private static final Map<Player,DialogRun> CURRENT_TASK = new HashMap<>();
    static final List<Runnable> LATE_CHECK = new ArrayList<>();
    private static Consumer<Player> defaultSound;
    static void stopAll() {
        CURRENT_TASK.forEach((p,d) -> d.cancel());
        CURRENT_TASK.clear();
    }

    static {
        READER_STRING.add("Talker",(d,s) -> d.talker = new FunctionPrinter(s));
        READER_STRING.add("Sound",(d,s) -> d.addConsumer(QuestUtil.getInstance().getSoundPlay(s)));
        READER_CONFIGURATION.add("Item",(d,c) -> c.getKeys(false).forEach(s -> {
            try {
                int i = Integer.parseInt(s);
                CutsceneConfig config = CutsceneConfig.getInstance();
                if (i != config.getDefaultDialogCenter() && i < config.getDefaultDialogRows() * 9) {
                    if (c.isItemStack(s)) d.stacks.put(i,new ItemBuilder(c.getItemStack(s)));
                    else if (c.isString(s)) d.stacks.put(i, ItemData.getItem(c.getString(s)));
                }
            } catch (Exception ignored) {
                CutsceneMaker.warn("fail to load item data: " + s);
            }
        }));
    }
    private final CutsceneManager manager;
    private final DialogRecord[] records;
    private Dialog[] subDialog;
    private Dialog[] endDialog;
    private String[] actions;

    private Consumer<Player> setQuest = p -> {};
    private Predicate<DialogCurrent> conditions;
    private Map<String,Consumer<Player>> typingSounds;

    public Dialog(CutsceneManager manager, ConfigurationSection section) {
        this.manager = manager;
        List<String> talk = getStringList(section,"Talk");
        if (talk != null) {
            records = talk.stream().map(s -> new DialogRecord(new FunctionPrinter(s))).toArray(DialogRecord[]::new);

            READER_STRING.apply(this,section);
            READER_CONFIGURATION.apply(this,section);

            if (section.isSet("TypingSound") && section.isConfigurationSection("TypingSound")) {
                ConfigurationSection typing = section.getConfigurationSection("TypingSound");
                typingSounds = typing.getKeys(false).stream().collect(Collectors.toMap(s -> s, s -> QuestUtil.getInstance().getSoundPlay(s)));
            }

            getOptionalList(section,"Conditions").ifPresent(t -> t.stream().map(s -> {
                String[] cond = TextUtil.getInstance().split(s," ");
                return (cond.length >= 3) ? ConditionBuilder.LIVING_ENTITY.find(cond) : null;
            }).filter(Objects::nonNull).forEach(p -> addPredicate(d -> p.test(d.player))));
            getOptionalList(section,"LinkedDialog").ifPresent(t -> LATE_CHECK.add(() -> {
                Dialog[] dialogs = QuestUtil.getInstance().getDialog(t);
                if (dialogs.length > 0) endDialog = dialogs;
            }));
            getOptionalList(section,"LinkedSubDialog").ifPresent(t -> LATE_CHECK.add(() -> {
                Dialog[] dialogs = QuestUtil.getInstance().getDialog(t);
                if (dialogs.length > 0) subDialog = dialogs;
            }));
            getOptionalList(section,"LinkedAction").ifPresent(t -> actions = t.toArray(new String[0]));
            getOptionalList(section,"SetQuest").ifPresent(t -> t.stream().map(s -> {
                String[] a = TextUtil.getInstance().split(s," ");
                return getQuestConsumer(a[0],(a.length > 1) ? a[1] : "give");
            }).filter(Objects::nonNull).forEach(c -> setQuest = setQuest.andThen(c)));
            getOptionalList(section,"SetVars").ifPresent(t -> t.stream().map(s -> {
                String[] a = TextUtil.getInstance().split(s," ");
                return (a.length > 1) ? getVarsConsumer(a[0],a[1],(a.length > 2) ? a[2] : "add") : null;
            }).filter(Objects::nonNull).forEach(c -> setQuest = setQuest.andThen(c)));

        } else throw new IllegalStateException("Invalid statement.");
        if (defaultSound == null) defaultSound = QuestUtil.getInstance().getSoundPlay(CutsceneConfig.getInstance().getDefaultTypingSound());
    }
    private Consumer<Player> getVarsConsumer(String key, String value, String change) {
        if (change.equals("set") || change.equals("=")) return p -> manager.getVars(p).get(key).setVar(value);
        else {
            try {
                double d = Double.parseDouble(value);
                switch (change) {
                    default:
                    case "+":
                    case "add":
                        return p -> {
                            Vars vars = manager.getVars(p).get(key);
                            vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() + d));
                        };
                    case "-":
                    case "sub":
                    case "subtract":
                        return p -> {
                            Vars vars = manager.getVars(p).get(key);
                            vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() - d));
                        };
                    case "*":
                    case "mul":
                    case "multiply":
                        return p -> {
                            Vars vars = manager.getVars(p).get(key);
                            vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() * d));
                        };
                    case "/":
                    case "div":
                    case "divide":
                        return p -> {
                            Vars vars = manager.getVars(p).get(key);
                            vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() / d));
                        };
                }
            } catch (Exception e) {
                CutsceneMaker.warn("The value \"" + value + "\" is not a number!");
                return null;
            }
        }
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
            }
        } else return null;
    }
    private QuestSet getQuestSet(String key) {
        QuestSet questSet = QuestData.QUEST_SET_MAP.get(key);
        if (questSet == null) CutsceneMaker.warn("The QuestSet named \"" + key + "\"doesn't exists!");
        return questSet;
    }
    private <T> T random(T[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(0,dialogs.length)];
    }

    public void run(@NotNull Player player, @NotNull String talker, @Nullable Consumer<Player> typingSound) {
        Map<String,Consumer<Player>> targetMap;
        if (typingSounds != null) {
            targetMap = new WeakHashMap<>(typingSounds);
            if (typingSound != null) targetMap.put(talker, typingSound);
        } else if (typingSound != null) {
            targetMap = Collections.singletonMap(talker,typingSound);
        }
        else {
            targetMap = null;
        }
        run(
                player,
                talker,
                InvUtil.getInstance().create(talker + "'s dialog",CutsceneConfig.getInstance().getDefaultDialogRows()),
                targetMap
        );
    }
    public void run(Player player, String talker, Inventory inv, Map<String,Consumer<Player>> typingSound) {
        DialogStartEvent event = new DialogStartEvent(player,this);
        EvtUtil.call(event);
        if (!event.isCancelled()) {
            player.openInventory(inv);
            run(new DialogCurrent(player, talker, inv, typingSound));
        }
    }
    private boolean run(DialogCurrent current) {
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
    private class DialogRun implements Listener {
        private final DialogCurrent current;
        private final ListenerManager listener = manager.register(this);
        private DialogReader reader;

        private int count;

        private DialogRun(DialogCurrent current) {
            this.current = current;
            load();
        }
        private void cancel() {
            stop();
            if (endDialog == null || !random(endDialog).run(current)) {
                current.player.closeInventory();
                EvtUtil.call(new DialogEndEvent(current.player,Dialog.this));
                if (actions != null) ActionData.start(random(actions), current.player);
                if (setQuest != null) setQuest.accept(current.player);
            }
        }
        private void stop() {
            listener.unregister();
            CURRENT_TASK.remove(current.player);
            if (reader != null) reader.cancel();
        }
        private void load() {
            if (count < records.length) {
                DialogRecord record = records[count];
                if (!record.printer.print(current.player).equals("skip")) {
                    reader = new DialogReader(this, current, record, time);
                    count++;
                } else {
                    load();
                }
            } else {
                cancel();
            }
        }
        private boolean isStopped = false;
        private int time = 2;
        private BukkitTask delay;
        @EventHandler
        public void onInvClose(InventoryCloseEvent e) {
            if (e.getPlayer().equals(current.player)) stop();
        }
        @EventHandler
        public void onInvClick(InventoryClickEvent e) {
            if (e.getWhoClicked().equals(current.player)) {
                e.setCancelled(true);
                if (current.inventory.equals(e.getClickedInventory()) && e.getSlot() == CutsceneConfig.getInstance().getDefaultDialogCenter() && delay == null && reader != null) {
                    delay = manager.runTaskLaterAsynchronously(() -> delay = null,4);
                    if (e.isLeftClick()) {
                        if (e.isShiftClick()) {
                            if (isStopped) {
                                reader.start(time);
                                isStopped = false;
                            }
                            else {
                                reader.stop();
                                isStopped = true;
                            }
                        } else {
                            time = Math.max(time - 1, 1);
                            reader.restart(time);
                        }
                    }
                    if (e.isRightClick()) {
                        time = Math.min(time + 1, 4);
                        reader.restart(time);
                    }
                }
            }
        }
    }
    private class DialogReader implements Runnable {
        private final DialogRun run;
        private final String message;
        private final Player player;
        private final Inventory inventory;
        private BukkitTask task;
        private final Consumer<Player> soundPlay;

        private int count;
        private final int center = CutsceneConfig.getInstance().getDefaultDialogCenter();

        private DialogReader(DialogRun run, DialogCurrent current, DialogRecord record, long time) {
            this.run = run;
            this.message = record.invoke(current.player);
            this.player = current.player;
            this.inventory = current.inventory;
            start(time);

            Consumer<Player> sound = (current.typingSound != null && record.talker != null) ? current.typingSound.get(record.talker.print(current.player)) : null;
            soundPlay = (sound != null) ? sound : defaultSound;

            inventory.clear();
            record.stacks.forEach((k,v) -> inventory.setItem(k,v.get(player)));
            ItemStack item = new ItemStack(CutsceneConfig.getInstance().getDialogReader());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + ((record.talker != null) ? record.talker.print(current.player) : current.talker) + ":");
            item.setItemMeta(meta);
            inventory.setItem(center,item);
        }
        private void restart(long time) {
            stop();
            start(time);
        }
        private void start(long time) {
            if (count < message.length()) task = manager.runTaskTimer(this,0,time);
        }
        private void stop() {
            if (count < message.length()) cancel();
        }

        @Override
        public void run() {
            if (count < message.length()) {
                count ++;
                String sub = sub(message);
                if (!last(sub,1).equals("*")) {
                    ItemStack item = inventory.getItem(center);
                    ItemMeta meta = item.getItemMeta();
                    meta.setLore(Collections.singletonList(ChatColor.WHITE + TextUtil.getInstance().colored(sub.replace("*",""))));
                    item.setItemMeta(meta);
                    if (soundPlay != null) soundPlay.accept(player);
                    player.updateInventory();
                }
            } else {
                cancel();
                task = manager.runTaskLater(run::load,20);
            }
        }
        private void cancel() {
            if (task != null) task.cancel();
        }

        private String sub(String full) {
            String substring = full.substring(0, count);
            while (((substring.length() >= 2) ? last(substring,2) : substring).contains("§")) {
                count = Math.min(count + 2,full.length());
                substring = full.substring(0,count);
            }
            return substring;
        }
        private String last(String text, int key) {
            return text.substring(Math.max(text.length() - key,1));
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
    @RequiredArgsConstructor
    private static class DialogCurrent {
        private final Player player;
        private final String talker;
        private final Inventory inventory;
        private final Map<String ,Consumer<Player>> typingSound;
    }

    public static final class DialogRecord {
        private Consumer<Player> consumer;
        private final FunctionPrinter printer;
        private FunctionPrinter talker;
        private final Map<Integer, ItemBuilder> stacks = new HashMap<>();

        private DialogRecord(FunctionPrinter printer) {
            this.printer = printer;
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
}
