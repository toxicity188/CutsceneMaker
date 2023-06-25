package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.Predicate;

public class HandlerChat extends ActionHandler {

    @DataField(aliases = {"msg","m"})
    public String message;
    @DataField(aliases = "c")
    public boolean cancel = false;

    private Predicate<AsyncPlayerChatEvent> check;

    public HandlerChat(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (message != null) check = e -> e.getMessage().equals(message);
        else check = e -> true;
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        if (check == null || check.test(e)) {
            apply(e.getPlayer());
            e.setCancelled(cancel);
        }
    }
}
