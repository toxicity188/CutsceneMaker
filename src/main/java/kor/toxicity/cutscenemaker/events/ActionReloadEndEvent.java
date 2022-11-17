package kor.toxicity.cutscenemaker.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ActionReloadEndEvent extends Event implements ICutsceneEvent {
    @Override
    public HandlerList getHandlers() {
        return ICutsceneEvent.HANDLER;
    }
}
