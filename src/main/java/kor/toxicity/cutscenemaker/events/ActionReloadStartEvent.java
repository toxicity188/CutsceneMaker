package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class ActionReloadStartEvent extends Event implements ICutsceneEvent {

    private final List<Runnable> task = new ArrayList<>();
    public ActionReloadStartEvent() {
        CutsceneMaker.debug("try to start reloading...");
    }
    public void addTask(Runnable runnable) {
        task.add(runnable);
    }
    public void run() {
        for (Runnable runnable : task) {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        task.clear();
    }
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
