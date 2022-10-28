package kor.toxicity.cutscenemaker.util.vars;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class VarsContainer {

    private final Player player;
    private final Map<String,Vars> vars = new HashMap<>();

    public VarsContainer(Player player) {
        this.player = player;
    }

    public void save(JavaPlugin pl) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(pl.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv"));
        writer.writeAll(vars.entrySet().stream().map(e -> new String[] {e.getKey(),e.getValue().getVar()}).collect(Collectors.toList()));
        writer.close();
    }

    public void load(JavaPlugin pl) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(pl.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv"));
        reader.forEach(t -> {
            if (t.length >= 2) vars.put(t[0],new Vars(t[1]));
        });
        reader.close();
    }

    public Vars get(String key) {
        if (!vars.containsKey(key)) vars.put(key,new Vars("<none>"));
        return vars.get(key);
    }

    public boolean register(JavaPlugin pl) {
        try {
            return new File(pl.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv").createNewFile();
        } catch (Exception e) {
            return false;
        }
    }
    public void remove(String key) {
        vars.remove(key);
    }
}
