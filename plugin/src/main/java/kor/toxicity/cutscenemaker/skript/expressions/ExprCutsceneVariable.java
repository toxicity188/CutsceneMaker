package kor.toxicity.cutscenemaker.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ExprCutsceneVariable extends SimpleExpression<String> implements ICutsceneExpression {

    private Expression<Player> playerExpression;
    private Expression<String> stringExpression;

    @Override
    protected String[] get(@NotNull Event e) {
        Player player = playerExpression.getSingle(e);
        String string = stringExpression.getSingle(e);
        if (player != null && string != null) return new String[] {
                CutsceneMaker.getVars(player,string).getVar()
        };
        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @NotNull
    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public void change(@NotNull Event e, Object[] delta, @NotNull Changer.ChangeMode mode) {
        Object obj = (delta != null && delta.length > 0) ? delta[0] : null;
        Player player = playerExpression.getSingle(e);
        String string = stringExpression.getSingle(e);
        if (player == null || string == null) return;
        switch (mode) {
            case ADD:
                if (obj instanceof Number) {
                    Vars vars = CutsceneMaker.getVars(player,string);
                    vars.setVar(Double.toString(vars.getAsNum().doubleValue() + ((Number) obj).doubleValue()));
                }
                break;
            case DELETE:
                CutsceneMaker.removeVars(player,string);
                break;
            case SET:
                if (obj == null) CutsceneMaker.removeVars(player,string);
                else CutsceneMaker.getVars(player,string).setVar(obj.toString());
                break;
        }
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD:
                return CollectionUtils.array(Number.class);
            case SET:
            case DELETE:
                return CollectionUtils.array(
                        String.class,
                        Character.class,
                        Boolean.class,

                        Number.class
                );
        }
        return null;
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "Get cutscene variable: " + playerExpression.toString(e,debug) + ", " + stringExpression.toString(e,debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(@NotNull Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        stringExpression = (Expression<String>) exprs[0];
        playerExpression = (Expression<Player>) exprs[1];
        return true;
    }
}
