package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class ActionStartEvent extends EntityEvent implements ICutsceneEvent, Cancellable {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final ActionContainer container;
    @Getter
    @Setter
    private boolean cancelled;

    public ActionStartEvent(Entity what, ActionContainer container) {
        super(what);
        CutsceneMaker.debug( what.getName() + "'s some action started.");
        this.container = container;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
