package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.events.enums.CancelCause;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

/**
 * ActionCancelEvent
 * This event might be called when Action is stopped unsafely, like quitting or dying.
 */
public class ActionCancelEvent extends EntityEvent implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final ActionContainer container; //Cancelled Action
    @Getter
    private final CancelCause cause; //A reason of cancellation

    public ActionCancelEvent(Entity what, ActionContainer container, CancelCause cause) {
        super(what);
        this.container = container;
        this.cause = cause;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
