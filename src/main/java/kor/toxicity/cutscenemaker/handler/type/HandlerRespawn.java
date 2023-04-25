package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
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
