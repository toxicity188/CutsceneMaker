package kor.toxicity.cutscenemaker.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ActionReloadEndEvent extends Event implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
