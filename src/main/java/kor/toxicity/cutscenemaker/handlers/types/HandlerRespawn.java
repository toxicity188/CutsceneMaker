package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;

public class HandlerRespawn extends ActionHandler {
    public HandlerRespawn(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {

    }
    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        apply(e.getPlayer());
    }
}
