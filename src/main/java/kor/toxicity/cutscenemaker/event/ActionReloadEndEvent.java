package kor.toxicity.cutscenemaker.event;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ActionReloadEndEvent extends Event implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();

    public ActionReloadEndEvent() {
        CutsceneMaker.debug("reload ended.");
    }
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
