package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.npc.CutsceneNPC;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NPCData extends CutsceneData {
    private static final Map<String, CutsceneNPC> NPC_MAP = new ConcurrentHashMap<>();
    public static final String METADATA_KEY = "CutsceneNPC";
    public NPCData(CutsceneMaker pl) {
        super(pl);
    }

    @Override
    public void reload() {
        CutsceneMaker maker = getPlugin();
        ConfigLoad load = maker.read("Entity");
        CutsceneManager manager = maker.getManager();
        manager.runTask(() -> {
            NPC_MAP.values().forEach(CutsceneNPC::kill);
            NPC_MAP.clear();
            load.forEach((f,fs) -> fs.forEach(s -> {
                ConfigurationSection section = load.getConfigurationSection(s);
                if (section != null) {
                    manager.runTaskAsynchronously(() -> {
                        try {
                            CutsceneNPC npc = new CutsceneNPC(manager,s,section);
                            NPC_MAP.put(s,npc);
                        } catch (Exception e) {
                            String message = e.getMessage();
                            CutsceneMaker.warn("An error has occurred. reason: " + (message != null ? message : "Unknown") + " (\"" + s + "\" in file \"" + f + "\")");
                        }
                    });
                }
            }));
        });
    }

}
