package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.data.*;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public final class CutsceneMaker extends JavaPlugin {

    public static final String NAME = "[CutsceneMaker]";
    private static final List<Runnable> LATE_CHECK = new ArrayList<>();
    public void addLateCheck(Runnable runnable) {
        LATE_CHECK.add(runnable);
    }

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;

    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "\\User").mkdir();

        if (!new File(getDataFolder().getAbsolutePath() + "\\quest.yml").exists()) saveResource("quest.yml",false);

        CutsceneCommand command = new CutsceneCommand(this);
        manager = new CutsceneManager(this);
        reload.add(command::unregister);
        reload.add(new GuiRegister(this));
        reload.add(() -> CutsceneConfig.getInstance().load(this));
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new LocationData(this));
        reload.add(new QuestData(this));
        reload.add(new ActionData(this));
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
            try {
                long time = System.currentTimeMillis();
                reload.forEach(Reloadable::reload);
                LATE_CHECK.forEach(Runnable::run);
                LATE_CHECK.clear();
                if (callback != null) callback.accept(System.currentTimeMillis() - time);
            } catch (Exception e) {
                warn("Error has occurred while reloading: " + e.getMessage());
            }
        });
    }

    public static void send(String s) {
        Bukkit.getConsoleSender().sendMessage(NAME + " " + s);
    }

    public static void warn(String s) {Bukkit.getLogger().warning(NAME + " " + s);}

    public CutsceneManager getManager() {
        return manager;
    }

    public ConfigLoad readSingleFile(String file) {
        try {
            return new ConfigLoad(this,file + ".yml","");
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    public ConfigLoad read(String dict) {
        return new ConfigLoad(new File(this.getDataFolder().getAbsolutePath() + "\\" + dict),"");
    }
    public static void removeVars(Player player, String key) {
        manager.getVars(player).remove(key);
    }
    public static Vars getVars(Player player, String key) {
        return manager.getVars(player,key);
    }

}
