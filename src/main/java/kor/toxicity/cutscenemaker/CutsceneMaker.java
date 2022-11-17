package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.data.*;
import kor.toxicity.cutscenemaker.events.ActionReloadEndEvent;
import kor.toxicity.cutscenemaker.events.ActionReloadStartEvent;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CutsceneMaker extends JavaPlugin {

    public static final String NAME = "[CutsceneMaker]";

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;

    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "\\User").mkdir();

        manager = new CutsceneManager(this);
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new LocationData(this));
        reload.add(new ActionData(this));
        getCommand("cutscene").setExecutor(new CutsceneCommand(this));

        load(() -> send("Plugin enabled."));
    }

    @Override
    public void onDisable() {
        send("Plugin disabled.");
    }

    void load(Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this,() -> {
            EvtUtil.call(new ActionReloadStartEvent());
            reload.forEach(Reloadable::reload);
            if (callback != null) callback.run();
            EvtUtil.call(new ActionReloadEndEvent());
        });
    }

    public static void send(String s) {
        Bukkit.getConsoleSender().sendMessage(NAME + " " + s);
    }

    public static void warn(String s) {Bukkit.getLogger().warning(NAME + " " + s);}

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
