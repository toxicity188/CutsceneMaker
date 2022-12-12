package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.quest.Quest;
import kor.toxicity.quest.events.DialogCompleteEvent;
import org.bukkit.event.EventHandler;

import java.util.function.Predicate;

public class HandlerDialogComplete extends ActionHandler {

    @DataField(aliases = "n")
    public String name;

    private Predicate<DialogCompleteEvent> check;

    public HandlerDialogComplete(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (name != null) check = d -> d.getDialog() == Quest.pl.getGlobalDialog(name);
    }

    @EventHandler
    public void complete(DialogCompleteEvent e) {
        if (check == null || check.test(e)) apply(e.getPlayer());
    }
}
