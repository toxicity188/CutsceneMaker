package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.actions.mechanics.ActAddVariable;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class QuestSet {

    private static final List<BiConsumer<Player,Double>> EXP_GETTER = new ArrayList<>();
    public static void addExpGetter(BiConsumer<Player,Double> consumer) {
        EXP_GETTER.add(consumer);
    }

    private final CutsceneMaker plugin;
    private final FunctionPrinter title;
    private final String name, completeAction;
    private List<QuestListener> listeners;

    private final List<FunctionPrinter> lore, recommend;

    private final ItemBuilder[] giveItem, takeItem;
    private final double money;
    private final double exp;
    @Getter
    private final boolean cancellable;

    public QuestSet(CutsceneMaker plugin, String node, ConfigurationSection section) {
        this.plugin = plugin;

        name = TextUtil.getInstance().colored(section.getString("Name",node));
        title = getFunctionPrinter(section);
        completeAction = getString(section);
        cancellable = section.getBoolean("Cancellable",false);

        lore = getFunctionList(section,"Lore",true);
        recommend = getFunctionList(section, "Recommend",false);

        money = section.getDouble("Money",0);
        exp = section.getDouble("Exp",0);

        giveItem = getArray(section, "RewardItem", this::toItemBuilder);
        takeItem = getArray(section, "TakeItem", this::toItemBuilder);

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

        if (giveItem != null) {
            rewardsArray = new ArrayList<>(giveItem.length + 2);
            for (ItemBuilder builder : giveItem) {
                rewardsArray.add(InvUtil.getInstance().getItemName(builder.get(player)));
            }
            rewardsArray.add("Exp: " + exp);
            rewardsArray.add("Gold: " + money);
        } else {
            rewardsArray = Arrays.asList(
                    "Exp: " + exp,
                    "Gold: " + money
            );
        }
        addLore(list, ChatColor.GREEN.toString() + ChatColor.BOLD + "[!] Rewards:", rewardsArray);

        meta.setLore(list);
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
            if (builder == null) CutsceneMaker.warn("The item named \"" + i + "\"doesn't exists!");
            return builder;
        }).filter(Objects::nonNull).toArray(ItemBuilder[]::new);
    }
    public int requiredStorage(Player player) {
        if (giveItem == null) return -1;
        return Arrays.stream(giveItem).mapToInt(b -> InvUtil.getInstance().storage(player,b.get(player))).sum();
    }
    public void give(Player player) {
        player.playSound(player.getLocation(),"entity.experience_orb.pickup",1.0F,1.0F);
        player.sendMessage("다음 퀘스트가 발주되었습니다: " + title.print(player));
        plugin.getManager().getVars(player).get("quest." +name).setVar("true");
    }
    public boolean has(Player player) {
        return plugin.getManager().getVars(player).get("quest." +name).getAsBool();
    }
    public void complete(Player player) {
        if (takeItem != null) for (ItemBuilder builder : takeItem) {
            player.getInventory().remove(builder.get(player));
        }
        if (giveItem != null) for (ItemBuilder builder : giveItem) {
            player.getInventory().addItem(builder.get(player));
        }
        MoneyUtil.getInstance().addMoney(player,money);
        EXP_GETTER.forEach(b -> b.accept(player,exp));
        plugin.getManager().getVars(player).remove("quest." +name);
        if (completeAction != null) ActionData.start(completeAction,player);
    }


    private ConfigurationSection getConfigurationSection(ConfigurationSection section, String key) {
        return (section.isSet(key) && section.isConfigurationSection(key)) ? section.getConfigurationSection(key) : null;
    }
    public boolean isCompleted(Player player) {
        return listeners.stream().allMatch(e -> e.isCompleted(player));
    }
    private class QuestListener {
        private Predicate<LivingEntity> condition;
        private final FunctionPrinter lore;

        private QuestListener(ConfigurationSection section) {
            lore = new FunctionPrinter(section.getString("lore","fail to read custom lore."));
            if (stringSet(section,"condition")) {
                String[] t = TextUtil.getInstance().split(section.getString("condition")," ");
                if (t.length >= 3) condition = ConditionBuilder.LIVING_ENTITY.find(t);
            }
            if (stringSet(section,"variable")) {
                if (stringSet(section,"event")) {
                    ActionContainer container = new ActionContainer(plugin);
                    ActAddVariable variable = new ActAddVariable(plugin.getManager());
                    variable.name = section.getString("variable");
                    variable.value = 1;
                    variable.initialize();
                    container.add(variable);
                    container.setConditions(p -> has((Player) p));
                    container.confirm();

                    ActionData.addHandler(section.getString("event"), container);
                }
            } else throw new RuntimeException("variable value not found.");

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
}
