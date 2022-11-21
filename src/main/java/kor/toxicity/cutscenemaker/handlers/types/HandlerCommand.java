package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.CutsceneCommand;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.function.Predicate;

public class HandlerCommand extends ActionHandler {

    @DataField(aliases = {"cmd","c"}, throwable = true)
    public String command;

    public HandlerCommand(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        CutsceneCommand.createCommand(command,(sender, command1, label, args) -> {
            if (sender instanceof LivingEntity) apply((LivingEntity) sender);
            return true;
        });
    }

    //@Override
    //protected void initialize() {
    //    if (command != null) {
    //        if (!command.contains("/")) command = "/" + command;
    //        check = e -> e.getMessage().equals(command);
    //    }
    //    else check = e -> true;
    //}

    //@EventHandler
    //public void chat(PlayerCommandPreprocessEvent e) {
    //    if (check.test(e)) {
    //        e.setCancelled(true);
    //        apply(e.getPlayer());
    //    }
    //}
}
