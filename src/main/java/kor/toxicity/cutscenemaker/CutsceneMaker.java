package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.data.*;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.events.ActionReloadEndEvent;
import kor.toxicity.cutscenemaker.events.ActionReloadStartEvent;
import kor.toxicity.cutscenemaker.quests.DialogData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class CutsceneMaker extends JavaPlugin {

    public static final String NAME = "[CutsceneMaker]";

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;

    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "\\User").mkdir();

        CutsceneCommand command = new CutsceneCommand(this);
        manager = new CutsceneManager(this);
        reload.add(command::unregister);
        reload.add(() -> CutsceneConfig.getInstance().load(this));
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new LocationData(this));
        reload.add(new ActionData(this));
        reload.add(new DialogData(this));
        getCommand("cutscene").setExecutor(command);

        EntityManager.getInstance().setExecutor(this);

        load(t -> send("Plugin enabled."));
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> Optional.of(manager.getVars(p)).ifPresent(f -> {
            try {
                f.save(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        send("Plugin disabled.");
    }

    void load(Consumer<Long> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this,() -> {
            long time = System.currentTimeMillis();
            reload.forEach(Reloadable::reload);
            if (callback != null) callback.accept(System.currentTimeMillis() - time);
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
