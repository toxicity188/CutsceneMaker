package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class QuestData extends CutsceneData {

    static final Map<String,Dialog> DIALOG_MAP = new HashMap<>();
    static final Map<String,QuestSet> QUEST_SET_MAP = new HashMap<>();
    static final Map<String,QnA> QNA_MAP = new HashMap<>();
    private static final Map<String,NPCData> NPC_MAP = new HashMap<>();
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
                        data.dialogs[Math.min(data.dialogs.length - 1, vars)].run(player, name, data.soundPlay);
                        delay.put(player,manager.runTaskLaterAsynchronously(() -> delay.remove(player), 4));
                    }
                }
            }
        });
        getPlugin().getCommand("quest").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                Inventory inventory = InvUtil.getInstance().create("진행중인 퀘스트 목록",3);
                int i = 9;
                for (String s : getPlugin().getManager().getVars(p).getVars().keySet()) {
                    if (i < 18 && s.startsWith("quest.")) {
                        String name = s.substring("quest.".length());
                        QuestSet questSet = QUEST_SET_MAP.get(name);
                        if (questSet != null) {
                            inventory.setItem(i,questSet.getIcon(p));
                            i++;
                        }
                    }
                }
                GuiRegister.registerNewGui(new GuiAdapter(p,inventory) {
                    @Override
                    public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {

                    }
                });
            }
            return true;
        });
    }

    @Override
    public void reload() {
        Dialog.stopAll();
        DIALOG_MAP.clear();
        NPC_MAP.clear();
        QNA_MAP.clear();
        QUEST_SET_MAP.clear();
        ConfigLoad quest = getPlugin().read("QuestSet");
        quest.getAllFiles().forEach(s -> {
            try {
                QUEST_SET_MAP.put(s,new QuestSet(getPlugin(),s,quest.getConfigurationSection(s)));
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
                QNA_MAP.put(s,new QnA(qna.getConfigurationSection(s)));
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
                       typingSound
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

    }
}
