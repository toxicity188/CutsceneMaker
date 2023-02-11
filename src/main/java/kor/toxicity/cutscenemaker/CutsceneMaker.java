package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.data.*;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.databases.CutsceneDB;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.CallbackManager;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class CutsceneMaker extends JavaPlugin {

    public static final String NAME = "[CutsceneMaker]";
    private static final List<Runnable> LATE_CHECK = new ArrayList<>();
    public void addLateCheck(Runnable runnable) {
        LATE_CHECK.add(runnable);
    }

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;
    private static boolean debug;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEnable() {
        this.getDataFolder().mkdir();
        new File(this.getDataFolder().getAbsolutePath() + "\\User").mkdir();
        setupDB();

        CutsceneCommand command = new CutsceneCommand(this);
        manager = new CutsceneManager(this);
        reload.add(command::unregister);
        reload.add(new GuiRegister(this));
        reload.add(new CallbackManager(this));
        reload.add(() -> {
            CutsceneConfig.getInstance().load(this);
            debug = CutsceneConfig.getInstance().isDebug();
        });
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new LocationData(this));
        reload.add(new QuestData(this));
        reload.add(new ActionData(this));
        getCommand("cutscene").setExecutor(command);

        EntityManager.getInstance().setExecutor(this);

        load(t -> send("Plugin enabled."));
    }
    private void setupDB() {
        ConfigLoad load = readResourceFile("database");
        switch (load.getString("using","csv")) {
            default:
            case "csv":
                CutsceneDB.setToDefault();
                break;
            case "mysql":
                CutsceneDB.useMySql(
                        load.getString("mysql-host","localhost"),
                        load.getString("mysql-database-name","CutsceneMaker"),
                        load.getString("mysql-user-name","root"),
                        load.getString("mysql-user-password",null)
                );
                break;
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            VarsContainer container = manager.getVars(p);
            if (container != null) CutsceneDB.save(p,this,container);
        });
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
    public static void send(CommandSender sender, String s) {
        sender.sendMessage(ChatColor.AQUA + NAME + ChatColor.WHITE + " " + s);
    }
    public static void warn(String s) {
        Bukkit.getLogger().warning(NAME + " " + s);
    }
    public static void debug(String s) {
        if (debug) Bukkit.getLogger().info(NAME + " Debug: " + s);
    }

    public CutsceneManager getManager() {
        return manager;
    }

    public ConfigLoad readResourceFile(String file) {
        try {
            if (!new File(getDataFolder().getAbsolutePath() + "\\" + file + ".yml").exists()) saveResource(file + ".yml",false);
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
    public static boolean isSet(Player player, String key) {
        return manager.isSet(player,key);
    }

}
