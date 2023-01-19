package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ActionReloadStartEvent extends Event implements ICutsceneEvent {

    public ActionReloadStartEvent() {
        CutsceneMaker.debug("try to start reloading...");
    }
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
