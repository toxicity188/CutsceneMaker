package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.events.QuestCompleteEvent;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class QuestSet implements Comparable<QuestSet> {

    private static final List<BiConsumer<Player,Double>> EXP_GETTER = new ArrayList<>();
    static final NavigableSet<String> TYPE_LIST = new TreeSet<>();
    public static void addExpGetter(BiConsumer<Player,Double> consumer) {
        EXP_GETTER.add(consumer);
    }

    private final CutsceneMaker plugin;
    private final FunctionPrinter title;
    @Getter
    private final String type;
    @Getter
    private final int priority;
    @Getter
    private final String name;
    private final String completeAction;
    private List<QuestListener> listeners;

    private final List<FunctionPrinter> lore, recommend;

    private final ItemBuilder[] giveItem, takeItem;
    @Getter
    private final double money;
    @Getter
    private final double exp;
    @Getter
    private final boolean cancellable;

    @Getter
    private final LocationSet locationSet;

    QuestSet(CutsceneMaker plugin, String node, ConfigurationSection section) {
        this.plugin = plugin;

        name = TextUtil.colored(section.getString("Name",node));
        type = section.getString("Type",null);
        if (type != null) TYPE_LIST.add(type);
        title = getFunctionPrinter(section);
        priority = section.getInt("Priority",-1);
        completeAction = getString(section);
        cancellable = section.getBoolean("Cancellable",false);

        lore = getFunctionList(section,"Lore",true);
        recommend = getFunctionList(section, "Recommend",false);

        money = section.getDouble("Money",0);
        exp = section.getDouble("Exp",0);


        giveItem = getArray(section, "RewardItem", QuestUtil::getItemBuilders);
        takeItem = getArray(section, "TakeItem", QuestUtil::getItemBuilders);

        getConfig(section,"Events").ifPresent(events -> {
            listeners = new ArrayList<>();
            events.getKeys(false).forEach(s -> getConfig(events,s).ifPresent(key -> {
                try {
                    listeners.add(new QuestListener(key));
                } catch (Exception e) {
                    CutsceneMaker.warn("unable to read the quest event: "  + s + " (QuestSet " + name + ")");
                }
            }));
            if (listeners.isEmpty()) listeners = null;
        });
        LocationSet set = new LocationSet();
        getConfig(section,"Locations").ifPresent(loc -> loc.getKeys(false).forEach(s -> getConfig(loc,s).ifPresent(t -> {
            if (!t.isSet("Location")) return;
            String n = t.getString("Location");
            Location l = plugin.getManager().getLocations().getValue(n);
            if (l != null) set.add(new NamedLocation(
                    TextUtil.colored(t.getString("Name","Unknown Name")),
                    l
            ));
            else CutsceneMaker.warn("The Location named \"" + n + "\" doesn't exist!");
        })));
        locationSet = (!set.isEmpty()) ? set.build(section.getString("Navigator","click the location you want go to!")) : null;
    }
    public ItemStack getIcon(Player player) {
        ItemStack stack = CutsceneConfig.getInstance().getDefaultQuestIcon().clone();
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + title.print(player));
        List<String> list = lore.stream().map(p -> ChatColor.WHITE + p.print(player)).collect(Collectors.toList());

        if (recommend != null)
            addLore(list,ChatColor.GOLD.toString() + ChatColor.BOLD + "[!] Recommends:",recommend.stream().map(p -> p.print(player)).collect(Collectors.toList()));
        if (listeners != null)
            addLore(list, ChatColor.YELLOW.toString() + ChatColor.BOLD + "[!] Conditions:",listeners.stream().map(l -> l.getLore(player)).collect(Collectors.toList()));

        final List<String> rewardsArray;

        String expString = TextUtil.applyComma(exp);
        String moneyString = TextUtil.applyComma(money);
        if (giveItem != null) {
            rewardsArray = new ArrayList<>(giveItem.length + 2);
            for (ItemBuilder builder : giveItem) {
                rewardsArray.add(InvUtil.getItemName(builder.get(player)));
            }
            rewardsArray.add("Exp: " + expString);
            rewardsArray.add("Gold: " + moneyString);
        } else {
            rewardsArray = Arrays.asList(
                    "Exp: " + expString,
                    "Gold: " + moneyString
            );
        }
        addLore(list, ChatColor.GREEN.toString() + ChatColor.BOLD + "[!] Rewards:", rewardsArray);

        meta.setLore(list);
        if (isCompleted(player)) meta.addEnchant(Enchantment.DURABILITY,0,true);
        stack.setItemMeta(meta);
        return stack;
    }
    private void addLore(List<String> target, String tag, List<String> collection) {
        target.add("");
        target.add(tag);
        for (String s : collection) {
            target.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "└ " + ChatColor.WHITE + s);
        }
    }
    private FunctionPrinter getFunctionPrinter(ConfigurationSection section) {
        return (section.isSet("Title") && section.isString("Title")) ? new FunctionPrinter(section.getString("Title")) : null;
    }
    private String getString(ConfigurationSection section) {
        return (section.isSet("CompleteAction") && section.isString("CompleteAction")) ? section.getString("CompleteAction") : null;
    }
    private <T> T[] getArray(ConfigurationSection section, String key, Function<List<String>,T[]> function) {
        List<String> list = getStringList(section,key,false);
        return (list != null) ? function.apply(list) : null;
    }
    private List<FunctionPrinter> getFunctionList(ConfigurationSection section, String key, boolean nonNull) {
        List<String> list = getStringList(section,key,nonNull);
        return (list != null) ? list.stream().map(FunctionPrinter::new).collect(Collectors.toList()) : null;
    }
    private List<String> getStringList(ConfigurationSection section, String key, boolean nonNull) {
        if (section.isSet(key)) {
            try {
                return section.getStringList(key);
            } catch (Exception e) {
                CutsceneMaker.warn("unable to read the string list: "  + key + " (QuestSet " + name + ")");
            }
        }
        return (nonNull) ? Collections.emptyList() : null;
    }

    public boolean isReady(Player player) {
        if (giveItem == null) return true;
        for (ItemBuilder builder : giveItem) {
            if (InvUtil.storage(player,builder.get(player)) == 0) return false;
        }
        return true;
    }
    public void give(Player player) {
        MessageSender sender = QuestData.QUEST_MESSAGE_MAP.get("quest-give");
        if (sender != null) sender.send(player,title.print(player));
        plugin.getManager().getVars(player).get("quest." +name).setVar("true");
    }
    public boolean has(Player player) {
        return plugin.getManager().getVars(player).contains("quest." +name);
    }
    static final Map<String,QuestEvent> EVENT_MAP = new HashMap<>();
    public void complete(Player player) {
        QuestCompleteEvent event = new QuestCompleteEvent(player,this);
        EvtUtil.call(event);

        double exp = event.getExp();
        double money = event.getMoney();
        Consumer<Player> consumer = CutsceneConfig.getInstance().getQuestCompleteSound();
        if (consumer != null) {
            player.sendTitle(title.print(player),"Quest Complete! - Gold: " + TextUtil.applyComma(money) + ", Exp:  " + TextUtil.applyComma(exp),10,60,10);
            consumer.accept(player);
        }
        if (takeItem != null) InvUtil.take(player, Arrays.stream(takeItem).map(i -> i.get(player)).toArray(ItemStack[]::new));
        if (giveItem != null) for (ItemBuilder builder : giveItem) {
            player.getInventory().addItem(builder.get(player));
        }
        MoneyUtil.addMoney(player,money);
        EXP_GETTER.forEach(b -> b.accept(player,exp));
        remove(player);
        if (completeAction != null) ActionData.start(completeAction,player);
    }

    public void remove(Player player) {
        plugin.getManager().getVars(player).remove("quest." +name);
    }

    private Optional<ConfigurationSection> getConfig(ConfigurationSection section, String key) {
        return (section.isSet(key) && section.isConfigurationSection(key)) ? Optional.of(section.getConfigurationSection(key)) : Optional.empty();
    }
    public boolean isCompleted(Player player) {
        return listeners != null && listeners.stream().allMatch(e -> e.isCompleted(player));
    }

    @Override
    public int compareTo(@NotNull QuestSet o) {
        int p = (priority >= 0 && o.priority >= 0) ? Integer.compare(priority,o.priority) : 0;
        return ((p != 0) ? p : title.compareTo(o.title));
    }


    private static final class QuestEvent {
        private Predicate<LivingEntity> predicate;
        private final ActionContainer container;
        private final String parameter;

        private QuestEvent(Predicate<LivingEntity> predicate, ActionContainer container, String parameter) {
            this.predicate = predicate;
            this.container = container;
            this.parameter = parameter;
            container.setConditions(predicate);
        }
        private void or(Predicate<LivingEntity> or) {
            predicate = predicate.or(or);
            container.setConditions(predicate);
        }
    }
    private class QuestListener {
        private final Predicate<LivingEntity> condition;
        private final FunctionPrinter lore;

        private QuestListener(ConfigurationSection section) {
            lore = new FunctionPrinter(section.getString("Lore","fail to read custom lore."));
            if (stringSet(section,"Condition")) {
                String[] t = TextUtil.split(section.getString("Condition")," ");
                if (t.length >= 3) condition = ConditionBuilder.LIVING_ENTITY.find(t);
                else condition = null;
            } else condition = null;
            if (stringSet(section,"Event")) {
                if (stringSet(section,"Variable")) {
                    String vars = section.getString("Variable");
                    String parameter = section.getString("Event");

                    ActionContainer container = new ActionContainer(plugin);
                    VarsAdder variable = new VarsAdder(plugin.getManager());
                    Predicate<LivingEntity> check;
                    if (condition != null) {
                        check = e -> {
                            Player p = (Player) e;
                            return has(p) && !condition.test(p);
                        };
                    } else {
                        check = e -> has((Player) e);
                    }
                    if (!EVENT_MAP.containsKey(vars)) {
                        variable.name = vars;
                        variable.initialize();
                        container.add(variable);

                        container.confirm();
                        if (ActionData.addHandler(parameter, container))
                            EVENT_MAP.put(vars,new QuestEvent(check,container,parameter));
                    } else {
                        QuestEvent event = EVENT_MAP.get(vars);
                        event.or(check);
                        if (!event.parameter.equals(parameter)) ActionData.addHandler(parameter, event.container);
                    }

                } else throw new RuntimeException("variable value not found.");
            }

        }
        private boolean stringSet(ConfigurationSection section, String s) {
            return section.isSet(s) && section.isString(s);
        }

        private boolean isCompleted(Player player) {
            return condition == null || condition.test(player);
        }
        private String getLore(Player player) {
            return (isCompleted(player) ? ChatColor.STRIKETHROUGH + TextUtil.uncolored(lore.print(player)) + ChatColor.YELLOW + ChatColor.BOLD + " Success!":  lore.print(player));
        }
    }
    private class VarsAdder extends CutsceneAction {
        private String name;
        public VarsAdder(CutsceneManager pl) {
            super(pl);
        }
        @Override
        protected void apply(LivingEntity entity) {
            Player p = (Player) entity;
            Vars vars = plugin.getManager().getVars(p).get(name);
            vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() + 1));
        }
    }

    static final class LocationSet {
        private final List<NamedLocation> set = new ArrayList<>();
        private Inventory inventory;

        private boolean isEmpty() {
            return set.isEmpty();
        }
        private void add(NamedLocation location) {
            set.add(location);
        }

        private LocationSet build(String name) {
            inventory = InvUtil.create(name,(int) Math.ceil((double) set.size() / 9D) + 2);
            ItemStack stack = new ItemStack(Material.BOOK);
            ItemMeta meta = stack.getItemMeta();
            int i = 0;
            for (NamedLocation n : set) {
                meta.setDisplayName(ChatColor.WHITE + n.name);
                meta.setLore(Collections.singletonList(
                        ChatColor.WHITE + TextUtil.toSimpleLoc(n.location)
                ));
                stack.setItemMeta(meta);
                inventory.setItem(9 + i++,stack);
            }
            return this;
        }
        void open(Player player) {
            GuiRegister.registerNewGui(new GuiAdapter(player,inventory) {
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    int t = slot - 9;
                    if (t >= 0 && t < set.size()) {
                        Navigator.startNavigate(player,set.get(t).location);
                    }
                }
            });
        }
    }
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class NamedLocation {
        private final String name;
        @EqualsAndHashCode.Exclude
        private final Location location;
    }

}
