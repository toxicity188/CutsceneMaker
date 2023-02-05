package kor.toxicity.cutscenemaker.util.vars;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VarsContainer {

    @Getter
    @Setter
    private BukkitTask task;
    private static final Map<String, Vars> global = new ConcurrentHashMap<>();
    @Getter
    private final Map<String,Vars> vars = new ConcurrentHashMap<>();

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
