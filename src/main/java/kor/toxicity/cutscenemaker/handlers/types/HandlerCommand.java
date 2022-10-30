package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.function.Predicate;

public class HandlerCommand extends ActionHandler {

    @DataField(aliases = {"cmd","c"})
    public String command;

    private Predicate<PlayerCommandPreprocessEvent> check;

    public HandlerCommand(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (command != null) {
            if (!command.contains("/")) command = "/" + command;
            check = e -> e.getMessage().equals(command);
        }
        else check = e -> true;
    }

    @EventHandler
    public void chat(PlayerCommandPreprocessEvent e) {
        if (check.test(e)) {
            e.setCancelled(true);
            apply(e.getPlayer());
        }
    }
}
