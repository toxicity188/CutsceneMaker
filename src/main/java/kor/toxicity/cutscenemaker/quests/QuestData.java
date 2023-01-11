package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.quests.enums.QuestGuiButton;
import kor.toxicity.cutscenemaker.quests.enums.QuestSetTask;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.InventorySupplier;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class QuestData extends CutsceneData {

    static final Map<String,Dialog> DIALOG_MAP = new HashMap<>();
    static final Map<String,QuestSet> QUEST_SET_MAP = new HashMap<>();
    static final Map<String,QnA> QNA_MAP = new HashMap<>();
    private static final Map<String,NPCData> NPC_MAP = new HashMap<>();
    private static final String INTERNAL_NAME_KEY = "cutscene.quest.name";
    public static void run(String name, Player player, String talker) {
        Dialog dialog = DIALOG_MAP.get(name);
        if (dialog != null) dialog.run(player,talker,null);
    }

    public QuestData(CutsceneMaker pl) {
        super(pl);
        CutsceneManager manager = pl.getManager();
        manager.register(new Listener() {

            private final Map<Player,BukkitTask> delay = new HashMap<>();

            @EventHandler
            public void onRightClick(PlayerInteractAtEntityEvent e) {
                Player player = e.getPlayer();
                if (!delay.containsKey(player)) {
                    String name = TextUtil.getInstance().getEntityName(e.getRightClicked());
                    NPCData data = NPC_MAP.get(name);
                    if (data != null) {
                        int vars = (data.followVars != null) ? (int) manager.getVars(player).get(data.followVars).getAsNum(0).doubleValue() : 0;
                        data.dialogs[Math.min(data.dialogs.length - 1, vars)].run(
                                player,
                                name,
                                (data.supplier != null) ? data.supplier.getInventory(player) : null,
                                data.soundPlay
                        );
                        delay.put(player,manager.runTaskLaterAsynchronously(() -> delay.remove(player), 4));
                    }
                }
            }
        });
        Dialog.setExecutor(pl);
        getPlugin().getCommand("quest").setExecutor((sender, command, label, args) -> {
            Bukkit.getScheduler().runTask(getPlugin(),() -> {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
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
                    for (QuestSet questSet : questList.subList(0, Math.min(questList.size(), 8))) {
                        inventory.setItem(loop, ItemUtil.setInternalTag(questSet.getIcon(p),INTERNAL_NAME_KEY,questSet.getName()));
                        loop++;
                    }
                    GuiRegister.registerNewGui(new GuiAdapter(p, inventory) {
                        private int page = 1;
                        private String type;
                        private List<QuestSet> sorted;

                        @Override
                        public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                            if (!isPlayerInventory) {
                                if (button != MouseButton.LEFT_WITH_SHIFT) return;
                                QuestSet get = QUEST_SET_MAP.get(ItemUtil.readInternalTag(item,INTERNAL_NAME_KEY));
                                if (get == null) return;
                                if (get.isCancellable()) {
                                    p.closeInventory();
                                    questList.remove(get);
                                    FunctionPrinter printer = QUEST_MESSAGE_MAP.get("quest-cancel-message");
                                    if (printer != null) p.sendMessage(printer.print(p));
                                } else {
                                    FunctionPrinter printer = QUEST_MESSAGE_MAP.get("quest-cancel-fail-message");
                                    if (printer != null) p.sendMessage(printer.print(p));
                                }
                            }
                        }

                        private void addPage(int i) {
                            page = Math.min(Math.max(page + i, 1), totalSize);
                            clear();
                            setup();
                        }

                        private QuestSet getQuest(int i) {
                            List<QuestSet> questSets = getQuestList();
                            return (questSets.size() > i) ? questSets.get(i - 1) : null;
                        }

                        private List<QuestSet> getQuestList() {
                            return (sorted != null) ? sorted : questList;
                        }

                        private List<QuestSet> init(String key) {
                            if (type == null || !type.equals(key))
                                sorted = questList.stream().filter(q -> q.getType().equals(key)).collect(Collectors.toList());
                            type = key;
                            return sorted;
                        }

                        private void clear() {
                            for (int i = 9; i < 18; i++) {
                                inventory.setItem(i, null);
                            }
                        }

                        private void setup() {
                            int k = (page - 1) * 9;
                            int i = first;
                            List<QuestSet> questSets = getQuestList();
                            for (QuestSet questSet : questSets.subList(k, Math.min(k + 8, questSets.size()))) {
                                inventory.setItem(i, questSet.getIcon(p));
                                i++;
                            }
                        }
                    });
                }
            });
            return true;
        });
    }

    private static final InventorySupplier DEFAULT_GUI_SUPPLIER = new InventorySupplier(new FunctionPrinter("Quest Gui"),3,null);
    private static final Map<String,FunctionPrinter> QUEST_MESSAGE_MAP = new HashMap<>();
    private static final List<GuiButton> QUEST_GUI_BUTTON = new ArrayList<>();
    private static class GuiButton {
        private final QuestGuiButton button;
        private final int slot;
        private final ItemBuilder builder;
        private GuiButton(QuestGuiButton button, ConfigurationSection section) {
            this.button = button;
            slot = section.getInt("Slot", button.getDefaultSlot());
            builder = (section.isSet("Item")) ? InvUtil.getInstance().fromConfig(section,"Item") : QuestGuiButton.DEFAULT_ITEM_BUILDER;
        }
    }
    private InventorySupplier supplier;
    @Override
    public void reload() {
        Dialog.stopAll();
        QUEST_MESSAGE_MAP.clear();
        DIALOG_MAP.clear();
        NPC_MAP.clear();
        QNA_MAP.clear();
        QUEST_GUI_BUTTON.clear();
        QUEST_SET_MAP.clear();
        ConfigLoad def = getPlugin().readSingleFile("quest");
        ConfigurationSection gui = def.getConfigurationSection("Gui");
        supplier = (gui != null) ? new InventorySupplier(gui) : DEFAULT_GUI_SUPPLIER;
        ConfigurationSection button = def.getConfigurationSection("Button");
        if (button != null) {
            for (String key : button.getKeys(false)) {
                try {
                    QuestGuiButton button1 = QuestGuiButton.valueOf(key.toUpperCase());
                    QUEST_GUI_BUTTON.add(new GuiButton(button1, button.getConfigurationSection(key)));
                } catch (Exception ignored) {}
            }
        }
        ConfigurationSection message = def.getConfigurationSection("Message");
        if (message != null) {
            message.getKeys(false).forEach(k -> {
                FunctionPrinter printer = (message.isString(k)) ? new FunctionPrinter(message.getString(k)) : null;
                if (printer != null) QUEST_MESSAGE_MAP.put(k,printer);
            });
        }

        ConfigLoad quest = getPlugin().read("QuestSet");
        quest.getAllFiles().forEach(s -> {
            try {
                QuestSet questSet = new QuestSet(getPlugin(),s,quest.getConfigurationSection(s));
                QUEST_SET_MAP.put(questSet.getName(),questSet);
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage() + " (QuestSet " + s + ")");
            }
        });
        ConfigLoad dialog = getPlugin().read("Dialog");
        dialog.getAllFiles().forEach(s -> {
            try {
                DIALOG_MAP.put(s,new Dialog(getPlugin().getManager(),dialog.getConfigurationSection(s)));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage() + " (Dialog " + s + ")");
            }
        });
        ConfigLoad qna = getPlugin().read("QnA");
        qna.getAllFiles().forEach(s -> {
            try {
                QNA_MAP.put(s,new QnA(getPlugin().getManager(),qna.getConfigurationSection(s)));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage() + " (QnA " + s + ")");
            }
        });
        Dialog.LATE_CHECK.forEach(Runnable::run);
        Dialog.LATE_CHECK.clear();
        ConfigLoad npc = getPlugin().read("NPC");
        npc.getAllFiles().forEach(s -> {
            ConfigurationSection section = npc.getConfigurationSection(s);
            if (section != null && section.isSet("Dialog")) {
               Consumer<Player> typingSound = null;
               if (section.isSet("TypingSound") && section.isString("TypingSound")) typingSound = QuestUtil.getInstance().getSoundPlay(section.getString("TypingSound"));
               NPCData data = new NPCData(
                       section.getString("Vars",null),
                       QuestUtil.getInstance().getDialog(section.getStringList("Dialog")),
                       typingSound,
                       (section.isSet("Inventory")) ? (section.isConfigurationSection("Inventory") ? new InventorySupplier(section.getConfigurationSection("Inventory")) : ItemData.getGui(section.getString("Inventory"))) : null
               );
               if (data.dialogs != null) NPC_MAP.put(section.getString("Name",s),data);
            }
        });
        QuestSet.clear();
        send(QUEST_SET_MAP.size(),"QuestSets");
        send(DIALOG_MAP.size(),"Dialogs");
        send(QNA_MAP.size(),"QnAs");
        send(NPC_MAP.size(),"NPCs");
    }
    private void send(int i, String s) {
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(i) + " "+ s + " successfully loaded.");
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class NPCData {
        private final String followVars;
        private final Dialog[] dialogs;
        private final Consumer<Player> soundPlay;
        private final InventorySupplier supplier;

    }

    public static boolean applyQuest(Player player, String name, QuestSetTask task) {
        QuestSet questSet = QUEST_SET_MAP.get(name);
        if (questSet == null) return false;
        task.getTask().accept(questSet,player);
        return true;
    }
}
