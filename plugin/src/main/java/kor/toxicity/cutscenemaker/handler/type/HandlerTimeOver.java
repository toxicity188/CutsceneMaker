package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.event.QuestTimeOverEvent;
import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;

import java.util.function.Predicate;

public class HandlerTimeOver extends ActionHandler {

    @DataField(aliases = "n")
    public String name;
    @DataField(aliases = "t")
    public String type;

    private Predicate<QuestSet> predicate;

    public HandlerTimeOver(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (name != null) build(p -> p.getName().equals(name));
        if (type != null) build(p -> p.getType().equals(type));
        name = null;
        type = null;
    }
    private void build(Predicate<QuestSet> predicate) {
        if (this.predicate == null) this.predicate = predicate;
        else this.predicate = this.predicate.and(predicate);
    }

    @EventHandler
    public void timeOut(QuestTimeOverEvent e) {
        if (predicate == null || predicate.test(e.getQuestSet())) apply(e.getPlayer());
    }
}
