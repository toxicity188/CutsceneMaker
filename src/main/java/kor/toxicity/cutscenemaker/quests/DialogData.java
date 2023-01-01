package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.TextUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class DialogData extends CutsceneData {

    static final Map<String,Dialog> DIALOG_MAP = new HashMap<>();
    private static final Map<String,NPCData> NPC_MAP = new HashMap<>();

    public DialogData(CutsceneMaker pl) {
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
                        int vars = (data.followVars != null) ? manager.getVars(player).get(data.followVars).getAsNum(0).intValue() : 0;
                        data.dialogs[Math.min(data.dialogs.length - 1, vars)].invoke(player, name);
                        delay.put(player,manager.runTaskLaterAsynchronously(() -> delay.remove(player), 4));
                    }
                }
            }
        });
    }

    @Override
    public void reload() {
        Dialog.stopAll();
        DIALOG_MAP.clear();
        NPC_MAP.clear();
        ConfigLoad load = getPlugin().read("Dialog");
        load.getAllFiles().forEach(s -> {
            try {
                DIALOG_MAP.put(s,new Dialog(getPlugin().getManager(),load.getConfigurationSection(s)));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage() + " (Dialog " + s + ")");
            }
        });
        Dialog.LATE_CHECK.forEach(Runnable::run);
        Dialog.LATE_CHECK.clear();
        ConfigLoad npc = getPlugin().read("NPC");
        npc.getAllFiles().forEach(s -> {
            ConfigurationSection section = npc.getConfigurationSection(s);
            if (section != null && section.isSet("dialogs")) {
                NPC_MAP.put(section.getString("name",s),new NPCData(
                        section.getString("follow-vars",null),
                        ArrayUtil.getInstance().getDialog(section.getStringList("dialogs"))
                ));
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(DIALOG_MAP.size()) + " dialogs successfully loaded.");
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(NPC_MAP.size()) + " npcs successfully loaded.");
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class NPCData {
        private final String followVars;
        private final Dialog[] dialogs;

    }
}
