package kor.toxicity.cutscenemaker.skript.effects;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import kor.toxicity.cutscenemaker.data.ActionData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class EffRunAction extends CutsceneEffect {

    private Expression<String> stringExpression;
    private Expression<Entity> entityExpression;

    @Override
    protected void execute(@NotNull Event e) {
        String s = stringExpression.getSingle(e);
        Entity entity = entityExpression.getSingle(e);
        if (s != null && entity instanceof LivingEntity) {
            ActionData.start(s,(LivingEntity) entity);
        }
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "run some action to some living entity: " + stringExpression.toString(e,debug) + ", " + entityExpression.toString(e,debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        stringExpression = (Expression<String>) exprs[0];
        entityExpression = (Expression<Entity>) exprs[1];
        return true;
    }
}
