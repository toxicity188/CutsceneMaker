package kor.toxicity.cutscenemaker.util.vars;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VarsContainer {

    private final Player player;
    private BukkitTask task;
    private static final Map<String, Vars> global = new ConcurrentHashMap<>();
    @Getter
    private final Map<String,Vars> vars = new ConcurrentHashMap<>();

    public VarsContainer(Player player) {
        this.player = player;
    }

    public void autoSave(JavaPlugin pl, long delay) {
        delay *= 20;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(pl,() -> save(pl),delay,delay);
    }
    public void stop() {
        if (task != null) task.cancel();
    }

    public void save(JavaPlugin pl) {
        String name = pl.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv";
        try (FileWriter file = new FileWriter(name); CSVWriter writer = new CSVWriter(file)) {
            writer.writeAll(vars.entrySet().stream().filter(e -> !e.getKey().startsWith("_")).map(e -> new String[] {e.getKey(),e.getValue().getVar()}).collect(Collectors.toList()));
            CutsceneMaker.debug(player.getName() + "'s user data saved.");
        } catch (Exception e) {
            e.printStackTrace();
            CutsceneMaker.warn("Unable to save the user data of " + player.getName());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void load(JavaPlugin pl) {
        String name = pl.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv";
        File create = new File(name);
        if (!create.exists()) {
            try {
                create.createNewFile();
                CutsceneMaker.debug("unable to find " + player.getName() + "'s data file. so create a new data file.");
            } catch (IOException ignored) {}
        }
        try (FileReader file = new FileReader(name); CSVReader reader = new CSVReader(file)) {
            reader.forEach(t -> {
                if (t.length >= 2) vars.put(t[0],new Vars(t[1]));
            });
            CutsceneMaker.debug(player.getName() + "'s data loaded.");
        } catch (Exception e) {
            e.printStackTrace();
            CutsceneMaker.warn("Unable to load the user data of " + player.getName());
        }
    }

    public synchronized Vars get(String key) {
        Vars v;
        if (key.charAt(0) == '*') {
            v = global.get(key);
            if (v == null) {
                v = new Vars("<none>");
                global.put(key, v);
            }
        } else {
            v = vars.get(key);
            if (v == null) {
                v = new Vars("<none>");
                vars.put(key, v);
            }
        }
        return v;
    }
    public synchronized boolean contains(String key) {
        if (key.charAt(0) == '*') {
            return global.containsKey(key);
        } else {
            return vars.containsKey(key);
        }
    }
    public void remove(String key) {
        vars.remove(key);
    }
}
