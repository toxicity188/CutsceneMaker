package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.quests.data.QuestCurrent;
import kor.toxicity.cutscenemaker.quests.enums.QuestAction;
import kor.toxicity.cutscenemaker.quests.enums.QuestButton;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.InventorySupplier;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class QuestData extends CutsceneData {

    static final Map<String,Dialog> DIALOG_MAP = new HashMap<>();
    static final Map<String,Present> PRESENT_MAP = new HashMap<>();
    static final Map<String,QuestSet> QUEST_SET_MAP = new HashMap<>();
    static final Map<String,QnA> QNA_MAP = new HashMap<>();
    private static final Map<String,NPCData> NPC_MAP = new HashMap<>();
    private static final String INTERNAL_NAME_KEY = "cutscene.quest.name";
    public static void run(String name, Player player, String talker) {
        Dialog dialog = DIALOG_MAP.get(name);
        if (dialog != null) dialog.run(player,talker,null);
    }
    public static Dialog getDialog(String name) {
        return DIALOG_MAP.get(name);
    }
    public static QuestSet getQuestSet(String name) {
        return QUEST_SET_MAP.get(name);
    }

    private final Consumer<Player> questGui;
    public QuestData(CutsceneMaker pl) {
        super(pl);
        CutsceneManager manager = pl.getManager();
        manager.register(new Listener() {
            private final Map<Player,BukkitTask> delay = new HashMap<>();
            @EventHandler
            public void onRightClick(PlayerInteractAtEntityEvent e) {
                Player player = e.getPlayer();
                if (!delay.containsKey(player)) {
                    String name = TextUtil.getEntityName(e.getRightClicked());
                    NPCData data = NPC_MAP.get(name);
                    if (data != null) {
                        int vars = (data.followVars != null)
                                ? (int) manager.getVars(player).get(data.followVars).getAsNum(0).doubleValue()
                                : ThreadLocalRandom.current().nextInt(0,data.dialogs.length);
                        data.dialogs[Math.min(data.dialogs.length - 1, vars)].run(
                                player,
                                name,
                                (data.supplier != null) ? data.supplier.getInventory(player) : null,
                                data.soundPlay
                        );
                        delay.put(player,manager.runTaskLaterAsynchronously(() -> delay.remove(player), 20));
                    }
                }
            }
        });
        Dialog.setExecutor(pl);
        Navigator.register(pl);
        questGui = p -> {

            Inventory inventory = supplier.getInventory(p);
            final int first = 9;

            int i = first;
            List<QuestSet> questList = getPlugin().getManager().getVars(p).getVars().keySet().stream().map(
                    s -> (s.startsWith("quest.")) ? QUEST_SET_MAP.get(s.substring("quest.".length())) : null
            ).filter(Objects::nonNull).collect(Collectors.toList());
            for (String s : getPlugin().getManager().getVars(p).getVars().keySet()) {
                if (i < first + 9 && s.startsWith("quest.")) {
                    String name = s.substring("quest.".length());
                    QuestSet questSet = QUEST_SET_MAP.get(name);
                    if (questSet != null) {
                        inventory.setItem(i, questSet.getIcon(p));
                        i++;
                    }
                }
            }
            final int totalSize = (int) Math.ceil((double) questList.size() / 9D);

            int loop = first;
            List<QuestSet> sub = questList.subList(0, Math.min(questList.size(), 9));
            sub.sort(Comparator.naturalOrder());
            for (QuestSet questSet : sub) {
                inventory.setItem(loop, ItemUtil.setInternalTag(questSet.getIcon(p),INTERNAL_NAME_KEY,questSet.getName()));
                loop++;
            }
            final QuestCurrent current = new QuestCurrent(new ArrayList<>(QuestSet.TYPE_LIST));
            current.setTotalPage(Math.max(totalSize,1));
            Set<CurrentGuiButton> buttonSet = new HashSet<>();
            for (GuiButton guiButton : QUEST_GUI_BUTTON) {
                inventory.setItem(guiButton.slot, guiButton.builder.get(p));

                ItemStack copy = inventory.getItem(guiButton.slot);
                buttonSet.add(new CurrentGuiButton(
                        guiButton.button,
                        copy,
                        current
                ));
            }
            GuiRegister.registerNewGui(new GuiAdapter(p, inventory) {
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (isPlayerInventory) return;
                    CurrentGuiButton action = buttonSet.stream().filter(c -> c.stack.equals(item)).findFirst().orElse(null);
                    if (action != null) {
                        switch (action.button) {
                            case TYPE_SORT:
                                switch (button) {
                                    case LEFT:
                                    case LEFT_WITH_SHIFT:
                                        sort();
                                        break;
                                    case RIGHT:
                                    case RIGHT_WITH_SHIFT:
                                        removeSort();
                                        break;
                                }
                                break;
                            case PAGE_BEFORE:
                                addPage(-1);
                                break;
                            case PAGE_AFTER:
                                addPage(1);
                                break;
                        }
                    } else {
                        QuestSet get = QUEST_SET_MAP.get(ItemUtil.readInternalTag(item, INTERNAL_NAME_KEY));
                        if (get == null) return;
                        switch (button) {
                            case LEFT:
                                QuestSet.LocationSet set = get.getLocationSet();
                                if (set == null) {
                                    MessageSender q = QUEST_MESSAGE_MAP.get("quest-no-location-found");
                                    if (q != null) q.send(p);
                                } else set.open(p);
                                break;
                            case RIGHT:
                                Navigator.endNavigate(p);
                                break;
                            case LEFT_WITH_SHIFT:
                                if (get.isCancellable()) {
                                    get.remove(p);
                                    MessageSender printer = QUEST_MESSAGE_MAP.get("quest-cancel-message");
                                    if (printer != null) printer.send(p);
                                    pl.getManager().runTaskLater(p::closeInventory, 1);
                                } else {
                                    MessageSender printer = QUEST_MESSAGE_MAP.get("quest-cancel-fail-message");
                                    if (printer != null) printer.send(p);
                                }
                                break;
                        }
                    }
                }

                private void addPage(int i) {
                    current.setPage(Math.min(Math.max(current.getPage() + i, 1), current.getTotalPage()));
                    setup();
                }
                private void sort() {
                    int size = current.getTypeList().size();
                    if (size == 0) return;
                    current.setPage(1);
                    int index = current.getTypeIndex();
                    if (index >= size) {
                        index = 0;
                        current.setTypeIndex(0);
                    }
                    current.setTypeIndex(index + 1);
                    String key = current.getTypeList().get(index);
                    List<QuestSet> sorted = questList.stream().filter(q -> {
                        String t = q.getType();
                        return t != null && t.equals(key);
                    }).collect(Collectors.toList());
                    current.setType(key);
                    current.setSorted(sorted);
                    current.setTotalPage(Math.max((int)Math.ceil((double)sorted.size()/9D),1));
                    setup();
                }
                private void removeSort() {
                    current.setType(null);
                    current.setPage(1);
                    current.setTotalPage(Math.max(totalSize,1));
                    setup();
                }
                private void setup() {
                    int k = (current.getPage() - 1) * 9;
                    int i = first;
                    List<QuestSet> questSets = (current.getType() != null && current.getSorted() != null) ? current.getSorted() : questList;
                    List<QuestSet> subList = questSets.subList(k, Math.min(k + 9, questSets.size()));
                    subList.sort(Comparator.naturalOrder());
                    for (QuestSet questSet : subList) {
                        inventory.setItem(i++, ItemUtil.setInternalTag(questSet.getIcon(p),INTERNAL_NAME_KEY,questSet.getName()));
                    }
                    for (int t = 0; t < 9 - subList.size(); t++) {
                        inventory.setItem(i++,null);
                    }
                    buttonSet.forEach(CurrentGuiButton::reloadLore);
                    p.updateInventory();
                }
            });
        };
        getPlugin().getCommand("quest").setExecutor((sender, command, label, args) -> {
            getPlugin().getManager().runTask(() -> {
                if (sender instanceof Player) questGui.accept((Player) sender);
            });
            return true;
        });
    }
    @EqualsAndHashCode
    private static class CurrentGuiButton {
        private final QuestButton button;
        @EqualsAndHashCode.Exclude
        private final ItemStack stack;
        @EqualsAndHashCode.Exclude
        private final ItemMeta meta;
        @EqualsAndHashCode.Exclude
        private final QuestCurrent current;
        @EqualsAndHashCode.Exclude
        private final List<String> lore;

        private CurrentGuiButton(QuestButton button, ItemStack stack, QuestCurrent current) {
            this.button = button;
            this.stack = stack;
            this.meta = stack.getItemMeta();
            this.current = current;
            lore = meta.getLore();
            reloadLore();
        }
        private void reloadLore() {
            if (lore == null) meta.setLore(button.getLore(current));
            else {
                List<String> ret = new ArrayList<>(button.getLore(current));
                ret.add("");
                ret.addAll(lore);
                meta.setLore(ret);
            }
            stack.setItemMeta(meta);
        }
    }

    private static final InventorySupplier DEFAULT_GUI_SUPPLIER = new InventorySupplier(new FunctionPrinter("Quest Gui"),3,null);
    static final Map<String,MessageSender> QUEST_MESSAGE_MAP = new HashMap<>();
    private static final List<GuiButton> QUEST_GUI_BUTTON = new ArrayList<>();
    private static class GuiButton {
        private final QuestButton button;
        private final int slot;
        private final ItemBuilder builder;
        private GuiButton(QuestButton button, ConfigurationSection section) {
            this.button = button;
            slot = section.getInt("Slot", button.getDefaultSlot());
            builder = (section.isSet("Item")) ? InvUtil.fromConfig(section,"Item") : QuestButton.DEFAULT_ITEM_BUILDER;
        }
    }
    private InventorySupplier supplier;
    static List<String> suffix;

    private final ConfigFunction questSetFunction = getFunction("QuestSet",(c,f,s) -> {
        QuestSet questSet = new QuestSet(f, s, getPlugin().getManager(), c);
        QUEST_SET_MAP.put(questSet.getName(),questSet);
    });
    private final ConfigFunction dialogFunction = getFunction("Dialog",(c,f,s) -> DIALOG_MAP.put(s,new Dialog(f,s,getPlugin().getManager(),c)));
    private final ConfigFunction qnaFunction = getFunction("QnA",(c,f,s) -> QNA_MAP.put(s,new QnA(f,s,getPlugin().getManager(),c)));
    private final ConfigFunction presentFunction = getFunction("Present",(c,f,s) -> PRESENT_MAP.put(s,new Present(f,s,getPlugin().getManager(),c)));
    private final ConfigFunction npcFunction = getFunction("NPC",(c,f,s) -> {
        if (c.isSet("Dialog")) {
            NPCData data = new NPCData(
                    c.getString("Vars", null),
                    QuestUtil.getDialog(c.getStringList("Dialog"),f,s),
                    ConfigUtil.getString(c,"TypingSound").map(QuestUtil::getSoundPlay).orElse(null),
                    (c.isSet("Inventory")) ? (c.isConfigurationSection("Inventory") ? new InventorySupplier(c.getConfigurationSection("Inventory")) : ItemData.getGui(c.getString("Inventory"))) : null
            );
            if (data.dialogs != null) NPC_MAP.put(c.getString("Name", s), data);
            else CutsceneMaker.warn("Dialog not found: NPC must have at least one Dialog! (NPC " + s + " in file \"" + f + "\".yml)");
        } else CutsceneMaker.warn("Syntax error: NPC must have at least one Dialog! (NPC " + s + " in file \"" + f + "\".yml)");
    });
    private static final List<Runnable> PRE_DIALOG_TASK = new ArrayList<>();
    private static final List<Runnable> POST_DIALOG_TASK = new ArrayList<>();
    @Override
    public void reload() {
        Dialog.stopAll(getPlugin());
        QUEST_MESSAGE_MAP.clear();
        DIALOG_MAP.clear();
        NPC_MAP.clear();
        PRESENT_MAP.clear();
        QNA_MAP.clear();
        QUEST_GUI_BUTTON.clear();
        QUEST_SET_MAP.clear();
        QuestSet.TYPE_LIST.clear();
        ConfigLoad def = getPlugin().readResourceFile("quest");
        ConfigurationSection gui = def.getConfigurationSection("Gui");
        supplier = (gui != null) ? new InventorySupplier(gui) : DEFAULT_GUI_SUPPLIER;
        List<String> suffix1 = def.getStringList("Suffix");
        suffix = (suffix1 != null) ? suffix1.stream().map(TextUtil::colored).collect(Collectors.toList()) : null;
        ConfigurationSection button = def.getConfigurationSection("Button");
        if (button != null) {
            for (String key : button.getKeys(false)) {
                try {
                    QuestButton button1 = QuestButton.valueOf(key.toUpperCase());
                    QUEST_GUI_BUTTON.add(new GuiButton(button1, button.getConfigurationSection(key)));
                } catch (Exception ignored) {}
            }
        }
        ConfigurationSection message = def.getConfigurationSection("Message");
        if (message != null) {
            message.getKeys(false).forEach(k -> {
                MessageSender printer = MessageSender.toConfig(message,k);
                if (printer != null) QUEST_MESSAGE_MAP.put(k,printer);
            });
        }



        tryParse("QuestSet",switchFunction(questSetFunction));
        tryParse("Dialog",switchFunction(dialogFunction));
        tryParse("QnA",switchFunction(qnaFunction));
        tryParse("Present",switchFunction(presentFunction));
        runAll(PRE_DIALOG_TASK);
        runAll(Dialog.LAZY_TASK);
        tryParse("NPC",switchFunction(npcFunction));
        runAll(POST_DIALOG_TASK);
        Navigator.reload();
        QuestSet.EVENT_MAP.clear();
        send(QUEST_SET_MAP.size(),"QuestSets");
        send(DIALOG_MAP.size(),"Dialogs");
        send(QNA_MAP.size(),"QnAs");
        send(PRESENT_MAP.size(),"Presents");
        send(NPC_MAP.size(),"NPCs");
    }
    private static void runAll(List<Runnable> list) {
        list.forEach(r -> {
            try {
                r.run();
            } catch (Exception ignored) {}
        });
        list.clear();
    }
    private ConfigFunction switchFunction(ConfigFunction function) {
        return (c,f,s) -> {
            String clazz = c.getString("Class",null);
            if (clazz != null) switch (clazz.toLowerCase()) {
                case "qna":
                    PRE_DIALOG_TASK.add(() -> qnaFunction.accept(c,f,s));
                    return;
                case "present":
                    PRE_DIALOG_TASK.add(() -> presentFunction.accept(c,f,s));
                    return;
                case "npc":
                    POST_DIALOG_TASK.add(() -> npcFunction.accept(c,f,s));
                    return;
                case "questset":
                    questSetFunction.accept(c,f,s);
                    return;
                case "dialog":
                    dialogFunction.accept(c,f,s);
            }
            else function.accept(c,f,s);
        };
    }
    private void send(int i, String s) {
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(i) + " "+ s + " successfully loaded.");
    }
    private void tryParse(String name, ConfigFunction consumer) {
        ConfigLoad load = getPlugin().read(name);
        load.forEach((f,k) -> k.forEach(s -> consumer.accept(load.getConfigurationSection(s),f,s)));
    }
    private static ConfigFunction getFunction(String name, ConfigFunction function) {
        return (c,f,s) -> {
            if (c == null) {
                CutsceneMaker.warn("Syntax error: this is not a section! (" + name + " " + s + " in file \"" + f + ".yml\")");
                return;
            }
            try {
                function.accept(c,f,s);
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage() + " (" + name + " " + s + " in file \"" + f + ".yml\")");
            }
        };
    }
    private interface ConfigFunction {
        void accept(ConfigurationSection load, String file, String key);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class NPCData {
        private final String followVars;
        private final Dialog[] dialogs;
        private final Consumer<Player> soundPlay;
        private final InventorySupplier supplier;

    }

    public static boolean applyQuest(Player player, String name, QuestAction task) {
        QuestSet questSet = QUEST_SET_MAP.get(name);
        if (questSet == null) return false;
        task.getTask().accept(questSet,player);
        return true;
    }
}
