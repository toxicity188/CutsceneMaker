package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.events.QuestCompleteEvent;
import kor.toxicity.cutscenemaker.material.WrappedMaterial;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.*;
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
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class QuestSet extends EditorSupplier implements Comparable<QuestSet> {

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

    @Getter
    private final long limit;
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


    private final Consumer<Player> onRemove, onComplete, onGive, onTimeOver;

    QuestSet(String fileName, String name, CutsceneManager manager, ConfigurationSection section) {
        super(fileName,name,manager,section);
        this.plugin = manager.getPlugin();

        this.name = TextUtil.colored(section.getString("Name",name));
        type = section.getString("Type",null);
        if (type != null) TYPE_LIST.add(type);
        title = getFunctionPrinter(section);
        limit = section.getLong("Limit",-1);
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
                    warn("unable to read the quest event: "  + s);
                }
            }));
            if (listeners.isEmpty()) listeners = null;
        });
        LocationSet set = new LocationSet();

        onGive = getVarsConsumer(section,"OnGive");
        onComplete = getVarsConsumer(section,"OnComplete");
        onRemove = getVarsConsumer(section,"OnRemove");
        onTimeOver = getVarsConsumer(section,"OnTimeOut");

        getConfig(section,"Locations").ifPresent(loc -> loc.getKeys(false).forEach(s -> getConfig(loc,s).ifPresent(t -> {
            if (!t.isSet("Location")) return;
            String n = t.getString("Location");
            Location l = plugin.getManager().getLocations().getValue(n);
            if (l != null) set.add(new NamedLocation(
                    TextUtil.colored(t.getString("Name","Unknown Name")),
                    l
            ));
            else warn("The Location named \"" + n + "\" doesn't exist!");
        })));
        locationSet = (!set.isEmpty()) ? set.build(section.getString("Navigator","click the location you want go to!")) : null;
    }
    private static Consumer<Player> getVarsConsumer(ConfigurationSection section, String key) {
        return ConfigUtil.getStringList(section,key).map(l -> {
            Consumer<Player> consumer = null;
            for (String s : l) {
                String[] t = TextUtil.split(s," ");

                Consumer<Player> t2 = QuestUtil.getVarsConsumer(t[0],t[2],t[1]);
                if (t2 == null) continue;

                if (consumer == null) consumer = t2;
                else consumer = consumer.andThen(t2);
            }
            return consumer;
        }).orElse(null);
    }

    public void warn(String s) {
        CutsceneMaker.warn(s + " (QuestSet " + name + " in file \"" + fileName + "\".yml)");
    }

    @NotNull
    public ItemStack getIcon(Player player) {
        return getIcon(player,true);
    }
    @NotNull
    public ItemStack getIconWithoutSuffix(Player player) {
        return getIcon(player,false);
    }
    private ItemStack getIcon(Player player, boolean addSuffix) {
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
        if (limit > 0) {
            String printer = CutsceneConfig.getInstance().getTimeLimit().print(player);
            if (!"".equals(printer)) {
                list.add("");
                String time;
                try {
                    time = Long.toString(Math.max((limit - ChronoUnit.MINUTES.between(LocalDateTime.parse(manager.getVars(player).get("quest." + name).getVar()),LocalDateTime.now())),0));
                } catch (Exception e) {
                    time = "NaN";
                }
                list.add(String.format(printer,time));
            }
        }
        if (addSuffix && QuestData.suffix != null) list.addAll(QuestData.suffix);

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
            if (section.isList(key) )return section.getStringList(key);
            else warn("unable to read the string list: "  + key);
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
        manager.getVars(player).get("quest." +name).setVar(LocalDateTime.now().toString());
        if (onGive != null) onGive.accept(player);
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
        if (onComplete != null) onComplete.accept(player);
        if (completeAction != null) ActionData.start(completeAction,player);
    }

    public void remove(Player player) {
        plugin.getManager().getVars(player).remove("quest." +name);
        if (onRemove != null) onRemove.accept(player);
    }
    public void timeOver(Player player) {
        if (onTimeOver != null) onTimeOver.accept(player);
    }

    private Optional<ConfigurationSection> getConfig(ConfigurationSection section, String key) {
        return ConfigUtil.getConfig(section,key);
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
            if (section.isSet("Event") && (section.isList("Event") || section.isString("Event"))) {
                if (stringSet(section,"Variable")) {
                    String vars = section.getString("Variable");
                    List<String> parameters = (section.isString("Event")) ? Collections.singletonList(section.getString("Event")) : section.getStringList("Event");

                    for (String parameter : parameters) {
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

    @Override
    public String toString() {
        return name;
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
            inventory = InvUtil.create(TextUtil.colored(name),(int) Math.ceil((double) set.size() / 9D) + 2);
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

    @Override
    Editor getEditor(Player player) {
        return new QuestSetEditor(player);
    }

    private static final List<String> NULL_ARRAY = Collections.singletonList(ChatColor.GRAY + "--- <none> ---");

    private class QuestSetEditor extends Editor {

        private final ConfigurationSection resources = QuestUtil.copy(section);

        private String title = ConfigUtil.getString(resources,"Title").orElse("a new title");
        private String[] lore = ConfigUtil.getStringList(resources,"Lore").map(l -> l.toArray(new String[0])).orElse(new String[] {"a new lore"});
        private String[] recommend = ConfigUtil.getStringList(resources,"Recommend").map(l -> l.toArray(new String[0])).orElse(null);

        private String[] rewardItem = ConfigUtil.getStringList(resources,"RewardItem").map(l -> l.toArray(new String[0])).orElse(null);

        private String[] takeItem = ConfigUtil.getStringList(resources,"TakeItem").map(l -> l.toArray(new String[0])).orElse(null);


        private String type = ConfigUtil.getString(resources,"Type").orElse(null);
        private int priority = ConfigUtil.getInt(resources,"Priority").orElse(-1);
        private int money = ConfigUtil.getInt(resources,"Money").orElse(0);
        private int exp = ConfigUtil.getInt(resources,"Exp").orElse(0);

        private boolean cancellable = resources.getBoolean("Cancellable",false);

        private Inventory inv;
        QuestSetEditor(Player player) {
            super(player, "QuestSet");
        }

        @Override
        GuiExecutor getMainExecutor() {
            if (inv == null) {
                inv = InvUtil.create(invName,6);
                setupGui();
            }
            return new GuiAdapter(player,inv) {
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    switch (slot) {
                        case 11:
                            chatTask(
                                    button,
                                    new String[] {
                                            ChatColor.YELLOW + "enter a title in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\""
                                    },
                                    s -> title = s[0],
                                    null
                            );
                            break;
                        case 13:
                            chatTask(
                                    button,
                                    new String[] {
                                            ChatColor.YELLOW + "enter a lore in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\""
                                    },
                                    s -> lore = QuestUtil.plusElement(lore,s[0]),
                                    () -> lore = QuestUtil.deleteLast(lore)
                            );
                            break;
                        case 15:
                            chatTask(
                                    button,
                                    new String[] {
                                            ChatColor.YELLOW + "enter a recommend in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\""
                                    },
                                    s -> recommend = QuestUtil.plusElement(recommend,s[0]),
                                    () -> recommend = QuestUtil.deleteLast(recommend)
                            );
                            break;
                        case 19:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the type!",
                                            "",
                                            ""
                                    },
                                    s -> type = s[0],
                                    () -> type = null
                            );
                            break;
                        case 21:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the priority!",
                                            "format) integer",
                                            "disable) -1"
                                    },
                                    s -> {
                                        try {
                                            priority = Integer.parseInt(s[0]);
                                        } catch (NumberFormatException e) {
                                            CutsceneMaker.send(player,"This is not an integer!");
                                        }
                                    },
                                    () -> priority = -1
                            );
                            break;
                        case 23:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the money amount!",
                                            "format) integer",
                                            ""
                                    },
                                    s -> {
                                        try {
                                            money = Integer.parseInt(s[0]);
                                        } catch (NumberFormatException e) {
                                            CutsceneMaker.send(player,"This is not an integer!");
                                        }
                                    }
                            );
                            break;
                        case 25:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the exp amount!",
                                            "format) integer",
                                            ""
                                    },
                                    s -> {
                                        try {
                                            exp = Integer.parseInt(s[0]);
                                        } catch (NumberFormatException e) {
                                            CutsceneMaker.send(player,"This is not an integer!");
                                        }
                                    }
                            );
                            break;
                        case 29:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the item name!",
                                            "format) <name> <amount>",
                                            ""
                                    },
                                    s -> rewardItem = QuestUtil.plusElement(rewardItem,s[0]),
                                    () -> rewardItem = QuestUtil.deleteLast(rewardItem)
                            );
                            break;
                        case 31:
                            cancellable = !cancellable;
                            setupGui();
                            player.updateInventory();
                            break;
                        case 33:
                            signTask(
                                    button,
                                    new String[] {
                                            "",
                                            "write the item name!",
                                            "format) <name> <amount>",
                                            ""
                                    },
                                    s -> takeItem = QuestUtil.plusElement(takeItem,s[0]),
                                    () -> takeItem = QuestUtil.deleteLast(takeItem)
                            );
                            break;
                    }
                }
            };
        }
        private void setupGui() {
            inv.setItem(
                    11,
                    getStringItem(
                            new ItemStack(Material.PAINTING),
                            "Title",
                            title,
                            "",
                            ChatColor.GRAY + "(Left: change title)"
                    )
            );
            inv.setItem(
                    13,
                    getArrayItem(
                            new ItemStack(Material.PAPER),
                            "Lore",
                            lore
                    )
            );
            inv.setItem(
                    15,
                    getArrayItem(
                            new ItemStack(Material.BOOK),
                            "Recommend",
                            recommend
                    )
            );
            inv.setItem(
                    19,
                    getStringItem(
                            new ItemStack(WrappedMaterial.getWrapper().getMonsterEgg()),
                            "Type",
                            type,
                            "",
                            ChatColor.GRAY + "(Left: change type)",
                            ChatColor.GRAY + "(Right: delete type)"
                    )
            );
            inv.setItem(
                    21,
                    getIntItem(
                            new ItemStack(WrappedMaterial.getWrapper().getCommandBlock()),
                            "Priority",
                            priority,
                            "",
                            ChatColor.GRAY + "(Left: change priority)"
                    )
            );
            inv.setItem(
                    23,
                    getIntItem(
                            new ItemStack(Material.GOLD_INGOT),
                            "Money",
                            money,
                            "",
                            ChatColor.GRAY + "(Left: change money)"
                    )
            );
            inv.setItem(
                    25,
                    getIntItem(
                            new ItemStack(Material.EMERALD),
                            "Exp",
                            exp,
                            "",
                            ChatColor.GRAY + "(Left: change exp)"
                    )
            );
            inv.setItem(
                    29,
                    getArrayItem(
                            new ItemStack(Material.CHEST),
                            "Reward Item",
                            rewardItem
                    )
            );
            inv.setItem(
                    31,
                    getBooleanItem(
                            "Cancellable",
                            cancellable,
                            "",
                            ChatColor.GRAY + "(Click: toggle cancellable)"
                    )
            );
            inv.setItem(
                    33,
                    getArrayItem(
                            new ItemStack(Material.ENDER_CHEST),
                            "Take Item",
                            takeItem
                    )
            );
        }

        private void chatTask(MouseButton button, String[] args, Consumer<String[]> callback, Runnable delete) {
            switch (button) {
                case LEFT:
                case LEFT_WITH_SHIFT:
                    CallbackManager.callbackChat(player,args,s -> {
                        callback.accept(s);
                        setupGui();
                        manager.runTaskLater(this::updateGui,5);
                    });
                    break;
                case RIGHT:
                case RIGHT_WITH_SHIFT:
                    if (delete != null) {
                        delete.run();
                        setupGui();
                        player.updateInventory();
                    }
                    break;
            }
        }
        private void signTask(MouseButton button, String[] args, Consumer<String[]> callback) {
            signTask(button, args, callback,null);
        }
        private void signTask(MouseButton button, String[] args, Consumer<String[]> callback, Runnable delete) {
            switch (button) {
                case LEFT:
                case LEFT_WITH_SHIFT:
                    CallbackManager.openSign(player,args,s -> {
                        if (!s[0].equals("")) {
                            callback.accept(s);
                        } else CutsceneMaker.send(player,"This value cannot be empty string!");
                        setupGui();
                        manager.runTaskLater(this::updateGui,5);
                    });
                    break;
                case RIGHT:
                case RIGHT_WITH_SHIFT:
                    if (delete != null) {
                        delete.run();
                        setupGui();
                        player.updateInventory();
                    }
                    break;
            }
        }
        @Override
        ConfigurationSection getSaveData() {
            resources.set("Title",title);

            resources.set("Cancellable",(cancellable) ? true : null);
            resources.set("Type",type);
            resources.set("Priority",(priority >= 0) ? priority : null);

            resources.set("Lore",lore);
            resources.set("Recommend",recommend);

            resources.set("Money",(money >= 0) ? money : null);
            resources.set("Exp",(exp >= 0) ? exp : null);

            resources.set("TakeItem",takeItem);
            resources.set("RewardItem",rewardItem);

            return resources;
        }
        private ItemStack getBooleanItem(@NotNull String display, boolean name, String... lore) {
            return getStringItem(
                    (name) ? new ItemStack(Material.EMERALD_BLOCK) : new ItemStack(Material.REDSTONE_BLOCK),
                    display,
                    (name) ? ChatColor.GREEN + "Enable" : ChatColor.RED + "Disable", lore
            );
        }
        private ItemStack getIntItem(@NotNull ItemStack item, @NotNull String display, int name, String... lore) {
            return getStringItem(item,display,TextUtil.applyComma(name),lore);
        }
        private ItemStack getStringItem(@NotNull ItemStack item, @NotNull String display, @Nullable String name, String... lore) {
            item.setItemMeta(
                    NBTReflector.addLore(
                            NBTReflector.edit(
                                    item.getItemMeta(),
                                    ChatColor.WHITE + TextUtil.colored(display),
                                    (name == null) ? NULL_ARRAY : Collections.singletonList(ChatColor.WHITE + TextUtil.colored(name))
                            ),
                            lore
                    )
            );
            return item;
        }
        private ItemStack getArrayItem(@NotNull ItemStack item, @NotNull String display ,@Nullable String... array) {
            item.setItemMeta(NBTReflector.addLore((array == null || array.length == 0) ?
                    NBTReflector.edit(item.getItemMeta(),ChatColor.WHITE + TextUtil.colored(display),NULL_ARRAY) :
                    NBTReflector.edit(item.getItemMeta(),ChatColor.WHITE + TextUtil.colored(display),
                            (array.length == 1) ?
                                    Collections.singletonList(ChatColor.YELLOW + " - " + ChatColor.WHITE + TextUtil.colored(array[0])) :
                                    Arrays.stream(array).map(s -> ChatColor.YELLOW + " - " + ChatColor.WHITE + TextUtil.colored(s)).collect(Collectors.toList())
                    ),
                    "",
                    ChatColor.GRAY + "(Left: add text)",
                    ChatColor.GRAY + "(Right: delete last text)"
            ));
            return item;
        }
    }
}
