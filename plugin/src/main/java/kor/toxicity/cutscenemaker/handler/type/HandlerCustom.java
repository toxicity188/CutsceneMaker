package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.event.CustomCutsceneEvent;
import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;

public class HandlerCustom extends ActionHandler {
    public HandlerCustom(ActionContainer container) {
        super(container);
    }

    @DataField(aliases = "k", throwable = true)
    public String key;

    @Override
    protected void initialize() {

    }
    @EventHandler
    public void custom(CustomCutsceneEvent e) {
        if (e.getKey().equals(key)) apply(e.getPlayer());
    }
}
