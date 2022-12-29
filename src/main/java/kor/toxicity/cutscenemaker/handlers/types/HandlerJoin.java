package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.events.UserDataLoadEvent;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import org.bukkit.event.EventHandler;

public class HandlerJoin extends ActionHandler {
    public HandlerJoin(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {

    }
    @EventHandler
    public void join(UserDataLoadEvent e) {
        apply(e.getPlayer());
    }
}
