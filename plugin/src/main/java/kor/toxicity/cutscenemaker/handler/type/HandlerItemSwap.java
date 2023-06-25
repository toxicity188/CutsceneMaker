package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class HandlerItemSwap extends ActionHandler {
    public HandlerItemSwap(ActionContainer container) {
        super(container);
    }

    @DataField(aliases = "c")
    public boolean cancel = false;

    @Override
    protected void initialize() {

    }

    @EventHandler(ignoreCancelled = true)
    public void itemSwap(PlayerSwapHandItemsEvent e) {
        apply(e.getPlayer());
        e.setCancelled(cancel);
    }
}
