package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class HandlerProcess extends ActionHandler {
    public HandlerProcess(ActionContainer container) {
        super(container);
    }

    @DataField(aliases = "n")
    public String name;
    @DataField(aliases = "c")
    public boolean cancel = false;

    @Override
    protected void initialize() {

    }

    @EventHandler
    public void process(PlayerCommandPreprocessEvent e) {
        if (name == null || e.getMessage().equals(name)) {
            apply(e.getPlayer());
            e.setCancelled(cancel);
        }
    }
}
