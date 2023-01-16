package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.events.QuestCompleteEvent;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class QuestSet {

    private static final List<BiConsumer<Player,Double>> EXP_GETTER = new ArrayList<>();
    static List<String> TYPE_LIST = new ArrayList<>();
    public static void addExpGetter(BiConsumer<Player,Double> consumer) {
        EXP_GETTER.add(consumer);
    }

    private final CutsceneMaker plugin;
    private final FunctionPrinter title;
    @Getter
    private final String type;
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

    QuestSet(CutsceneMaker plugin, String node, ConfigurationSection section) {
        this.plugin = plugin;

        name = TextUtil.getInstance().colored(section.getString("Name",node));
        type = section.getString("Type",null);
        if (type != null) TYPE_LIST.add(type);
        title = getFunctionPrinter(section);
        completeAction = getString(section);
        cancellable = section.getBoolean("Cancellable",false);

        lore = getFunctionList(section,"Lore",true);
        recommend = getFunctionList(section, "Recommend",false);

        money = section.getDouble("Money",0);
        exp = section.getDouble("Exp",0);

        Function<List<String>,ItemBuilder[]> function = p -> p.stream().map(s -> {
            String[] t = TextUtil.getInstance().split(s," ");
            ItemBuilder builder = InvUtil.getInstance().toName(t[0]);
            if (builder != null && t.length > 1) {
                try {
                    return builder.setAmount(Integer.parseInt(t[1]));
                } catch (Exception ignored) {
                }
            }
            return builder;
        }).filter(Objects::nonNull).toArray(ItemBuilder[]::new);
        giveItem = getArray(section, "RewardItem", function);
        takeItem = getArray(section, "TakeItem", function);

        ConfigurationSection events = getConfigurationSection(section,"Events");
        if (events != null) {
            listeners = new ArrayList<>();
            events.getKeys(false).forEach(s -> {
                ConfigurationSection key = getConfigurationSection(events,s);
                if (key != null) {
                    try {
                        listeners.add(new QuestListener(key));
                    } catch (Exception e) {
                        CutsceneMaker.warn("unable to read the quest event: "  + s + " (QuestSet " + name + ")");
                    }
                }
            });
            if (listeners.isEmpty()) listeners = null;
        }
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

        String expString = TextUtil.getInstance().applyComma(exp);
        String moneyString = TextUtil.getInstance().applyComma(money);
        if (giveItem != null) {
            rewardsArray = new ArrayList<>(giveItem.length + 2);
            for (ItemBuilder builder : giveItem) {
                rewardsArray.add(InvUtil.getInstance().getItemName(builder.get(player)));
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

    private ItemBuilder[] toItemBuilder(List<String> list) {
        return list.stream().map(i -> {
            ItemBuilder builder = ItemData.getItem(i);
            if (builder == null) CutsceneMaker.warn("The item named \"" + i + "\"doesn't exist!");
            return builder;
        }).filter(Objects::nonNull).toArray(ItemBuilder[]::new);
    }
    public int requiredStorage(Player player) {
        if (giveItem == null) return -1;
        return Arrays.stream(giveItem).mapToInt(b -> InvUtil.getInstance().storage(player,b.get(player))).sum();
    }
    public boolean isReady(Player player) {
        if (giveItem == null) return true;
        for (ItemBuilder builder : giveItem) {
            if (InvUtil.getInstance().storage(player,builder.get(player)) == 0) return false;
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
    public void complete(Player player) {
        QuestCompleteEvent event = new QuestCompleteEvent(player,this);
        EvtUtil.call(event);

        double exp = event.getExp();
        double money = event.getMoney();
        Consumer<Player> consumer = CutsceneConfig.getInstance().getQuestCompleteSound();
        if (consumer != null) {
            player.sendTitle(title.print(player),"Quest Complete! - Gold: " + TextUtil.getInstance().applyComma(money) + ", Exp:  " + TextUtil.getInstance().applyComma(exp),10,60,10);
            consumer.accept(player);
        }
        if (takeItem != null) InvUtil.getInstance().take(player, Arrays.stream(takeItem).map(i -> i.get(player)).toArray(ItemStack[]::new));
        if (giveItem != null) for (ItemBuilder builder : giveItem) {
            player.getInventory().addItem(builder.get(player));
        }
        MoneyUtil.getInstance().addMoney(player,money);
        EXP_GETTER.forEach(b -> b.accept(player,exp));
        remove(player);
        if (completeAction != null) ActionData.start(completeAction,player);
    }

    public void remove(Player player) {
        plugin.getManager().getVars(player).remove("quest." +name);
    }

    private ConfigurationSection getConfigurationSection(ConfigurationSection section, String key) {
        return (section.isSet(key) && section.isConfigurationSection(key)) ? section.getConfigurationSection(key) : null;
    }
    public boolean isCompleted(Player player) {
        return listeners != null && listeners.stream().allMatch(e -> e.isCompleted(player));
    }

    private static final Map<String,QuestEvent> EVENT_MAP = new HashMap<>();
    static void clear() {
        TYPE_LIST.clear();
        EVENT_MAP.clear();
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
                String[] t = TextUtil.getInstance().split(section.getString("Condition")," ");
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
            return (isCompleted(player) ? ChatColor.STRIKETHROUGH + TextUtil.getInstance().uncolored(lore.print(player)) + ChatColor.YELLOW + ChatColor.BOLD + " Success!":  lore.print(player));
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

}
