package kor.toxicity.cutscenemaker.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ExprQuestSet extends SimpleExpression<QuestSet> implements ICutsceneExpression {

    private Expression<String> stringExpression;

    @Override
    protected QuestSet[] get(@NotNull Event e) {
        String s = stringExpression.getSingle(e);
        if (s == null) return null;
        QuestSet set = QuestData.getQuestSet(s);
        return (set != null) ? CollectionUtils.array(set) : null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @NotNull
    @Override
    public Class<? extends QuestSet> getReturnType() {
        return QuestSet.class;
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "get QuestSet: " + stringExpression.toString(e,debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(@NotNull Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        stringExpression = (Expression<String>) exprs[0];
        return true;
    }
}
