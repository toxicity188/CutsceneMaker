package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class Dialog {

    public static final ConfigMapReader<String> READER_STRING = new ConfigMapReader<>(ConfigurationSection::getString);
    public static final ConfigMapReader<ConfigurationSection> READER_CONFIGURATION = new ConfigMapReader<>((c, k) -> (c.isConfigurationSection(k)) ? c.getConfigurationSection(k) : null);

    private static final Map<Player,DialogRun> CURRENT_TASK = new HashMap<>();
    static final List<Runnable> LATE_CHECK = new ArrayList<>();
    static void stopAll() {
        CURRENT_TASK.forEach((p,d) -> d.cancel());
        CURRENT_TASK.clear();
    }

    static {
        READER_STRING.add("Talker",(d,s) -> d.talker = s);
        READER_STRING.add("Sound",(d,s) -> {
            String[] sounds = TextUtil.getInstance().split(s," ");
            String sound = sounds[0];
            final float volume, pitch;
            volume = (sounds.length > 1) ? getFloat(sounds[1]) : 1;
            pitch = (sounds.length > 2) ? getFloat(sounds[2]) : 1;
            d.addConsumer(p -> p.playSound(p.getLocation(),sound,volume,pitch));
        });
        READER_CONFIGURATION.add("Item",(d,c) -> c.getKeys(false).forEach(s -> {
            if (c.isConfigurationSection(s)) {
                ConfigurationSection section = c.getConfigurationSection(s);
                section.getKeys(false).forEach(t -> {
                    try {
                        d.stacks.put(Integer.parseInt(t),section.getItemStack(t));
                    } catch (Exception ignored) {
                    }
                });
            }
        }));
    }
    private final CutsceneManager manager;
    private final DialogRecord[] records;
    private Dialog[] subDialog;
    private Dialog[] endDialog;
    private Predicate<DialogCurrent> conditions;

    public Dialog(CutsceneManager manager, ConfigurationSection section) {
        this.manager = manager;
        List<String> talk = getStringList(section,"Talk");
        if (talk != null) {
            records = talk.stream().map(s -> new DialogRecord(new FunctionPrinter(s))).toArray(DialogRecord[]::new);

            READER_STRING.apply(this,section);
            READER_CONFIGURATION.apply(this,section);

            getOptionalList(section,"Conditions").ifPresent(t -> t.stream().map(s -> {
                String[] cond = TextUtil.getInstance().split(s," ");
                return (cond.length >= 3) ? ConditionBuilder.LIVING_ENTITY.find(cond) : null;
            }).filter(Objects::nonNull).forEach(p -> addPredicate(d -> p.test(d.player))));
            getOptionalList(section,"LinkedDialog").ifPresent(t -> LATE_CHECK.add(() -> {
                Dialog[] dialogs = ArrayUtil.getInstance().getDialog(t);
                if (dialogs.length > 0) endDialog = dialogs;
            }));
            getOptionalList(section,"LinkedSubDialog").ifPresent(t -> LATE_CHECK.add(() -> {
                Dialog[] dialogs = ArrayUtil.getInstance().getDialog(t);
                if (dialogs.length > 0) subDialog = dialogs;
            }));
        } else throw new IllegalStateException("Invalid statement.");
    }
    private Dialog random(Dialog[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(0,dialogs.length)];
    }

    public void invoke(Player player, String talker) {
        invoke(player,InvUtil.getInstance().create(talker + "'s dialog",5));
    }
    public void invoke(Player player, Inventory inv) {
        player.openInventory(inv);
        invoke(new DialogCurrent(player,inv));
    }
    private void invoke(DialogCurrent current) {
        if (CURRENT_TASK.containsKey(current.player)) return;
        if (conditions == null || conditions.test(current)) {
            CURRENT_TASK.put(current.player, new DialogRun(current));
        } else if (subDialog != null) random(subDialog).invoke(current);
    }
    private void addPredicate(Predicate<DialogCurrent> predicate) {
        if (conditions == null) conditions = predicate;
        else conditions = conditions.and(predicate);
    }
    private static float getFloat(String target) {
        try {
            return Float.parseFloat(target);
        } catch (Exception e) {
            return (float) 1;
        }
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
            listener.unregister();
            if (endDialog == null) current.player.closeInventory();
            CURRENT_TASK.remove(current.player);
            if (reader != null) reader.cancel();
        }
        private void load() {
            if (count < records.length) {
                reader = new DialogReader(this, current, records[count]);
                count++;
            } else {
                cancel();
            }
        }
        @EventHandler
        public void onInvClose(InventoryCloseEvent e) {
            if (e.getPlayer().equals(current.player)) cancel();
        }
        @EventHandler
        public void onInvClick(InventoryClickEvent e) {
            if (e.getWhoClicked().equals(current.player)) e.setCancelled(true);
        }
    }
    private class DialogReader implements Runnable {
        private final DialogRun run;
        private final String message;
        private final Player player;
        private final Inventory inventory;
        private BukkitTask task;

        private int count;

        private DialogReader(DialogRun run, DialogCurrent current, DialogRecord record) {
            this.run = run;
            this.message = record.invoke(current.player);
            this.player = current.player;
            this.inventory = current.inventory;
            task = manager.runTaskTimer(this,0,2);

            inventory.clear();
            record.stacks.forEach(inventory::setItem);
            ItemStack item = new ItemStack(CutsceneConfig.getInstance().getDialogReader());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.WHITE + record.talker + ":");
            inventory.setItem(22,item);
        }

        @Override
        public void run() {
            if (count < message.length()) {
                count ++;
                String sub = sub(message);
                if (!last(sub,1).equals("*")) {
                    ItemStack item = inventory.getItem(22);
                    ItemMeta meta = item.getItemMeta();
                    meta.setLore(Collections.singletonList(ChatColor.WHITE + TextUtil.getInstance().colored(sub.replace("*",""))));
                    item.setItemMeta(meta);
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
                count += 2;
                substring = full.substring(0,count);
            }
            return substring;
        }
        private String last(String text, int key) {
            return text.substring(Math.max(text.length() - key,1));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ConfigMapReader<T> {
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
                for (String s : section.getConfigurationSection(key).getKeys(false)) {
                    try {
                        int i = Integer.parseInt(s);
                        Optional.ofNullable((dialog.records.length < i) ? dialog.records[i - 1] : null).ifPresent(d -> {
                            T get = getter.apply(section,s);
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
        private final Inventory inventory;
    }

    public static final class DialogRecord {
        private Consumer<Player> consumer;
        private final FunctionPrinter printer;
        private String talker;
        private final Map<Integer,ItemStack> stacks = new HashMap<>();

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
