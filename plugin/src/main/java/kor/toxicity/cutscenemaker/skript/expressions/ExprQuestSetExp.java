package kor.toxicity.cutscenemaker.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import kor.toxicity.cutscenemaker.event.QuestCompleteEvent;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ExprQuestSetExp extends SimpleExpression<Double> implements ICutsceneExpression {

    private Expression<QuestSet> questSetExpression;

    @Override
    protected Double[] get(@NotNull Event e) {
        QuestSet set = questSetExpression.getSingle(e);
        if (set != null) {
            if (e instanceof QuestCompleteEvent) {
                QuestCompleteEvent event = (QuestCompleteEvent) e;
                if (event.getQuestSet() == set) return new Double[] {
                        event.getExp()
                };
            }
            return new Double[] {
                    set.getExp()
            };
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @NotNull
    @Override
    public Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @Override
    public void change(@NotNull Event e, Object[] delta, @NotNull Changer.ChangeMode mode) {
        double d = (delta != null) ? ((Number) delta[0]).doubleValue() : 0;
        if (e instanceof QuestCompleteEvent) {
            QuestCompleteEvent event = (QuestCompleteEvent) e;
            switch (mode) {
                case SET:
                    event.setExp(d);
                    break;
                case ADD:
                    event.setExp(event.getExp() + d);
                    break;
            }
        }
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET:
            case ADD:
                return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "A money of some QuestSet: " + questSetExpression.toString(e,debug);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(@NotNull Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        questSetExpression = (Expression<QuestSet>) exprs[0];
        return true;
    }
}
