package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.data.EventData;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.data.Reloadable;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CutsceneMaker extends JavaPlugin {

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;

    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "\\User").mkdir();

        manager = new CutsceneManager(this);
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new ActionData(this));
        load();
        getCommand("cutscene").setExecutor(new CutsceneCommand(this));

        send("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        send("Plugin disabled.");
    }

    void load() {
        reload.forEach(Reloadable::reload);
    }

    public static void send(String s) {
        Bukkit.getConsoleSender().sendMessage("[CutsceneMaker] " + s);
    }

    public CutsceneManager getManager() {
        return manager;
    }

    public ConfigLoad read(String dict) {
        return new ConfigLoad(new File(this.getDataFolder().getAbsolutePath() + "\\" + dict),"");
    }
    public static Vars getVars(Player player, String key) {
        return manager.getVars(player,key);
    }
}
