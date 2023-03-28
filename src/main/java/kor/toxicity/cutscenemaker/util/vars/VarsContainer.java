package kor.toxicity.cutscenemaker.util.vars;

import kor.toxicity.cutscenemaker.util.StorageItem;
import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VarsContainer {

    private static final Vars EMPTY = new Vars("<none>") {
        @Override
        public void setVar(String var) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getAsBool() {
            return false;
        }

        @Override
        public Number getAsNum() {
            return 0;
        }
    };
    private final List<BukkitTask> tasks = new ArrayList<>();
    public void addTask(BukkitTask task) {
        tasks.add(task);
    }
    public void stopTask() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }
    private static final Map<String, Vars> global = new ConcurrentHashMap<>();
    private final Map<String,Vars> vars = new ConcurrentHashMap<>();

    public Map<String, Vars> getVars() {
        return vars;
    }

    private final List<StorageItem> tempStorage = new ArrayList<>();

    public List<StorageItem> getTempStorage() {
        return tempStorage;
    }

    public synchronized @NotNull Vars get(String key) {
        if (key == null || key.length() == 0) return EMPTY;
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
        if (key == null || key.length() == 0) return false;
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
