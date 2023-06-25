package kor.toxicity.cutscenemaker.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ExprCutsceneVariableArray extends SimpleExpression<String> implements ICutsceneExpression {

    private Expression<Player> playerExpression;
    private Expression<String> stringExpression;

    @Override
    protected String[] get(@NotNull Event e) {
        Player player = playerExpression.getSingle(e);
        String string = stringExpression.getSingle(e);
        if (player != null && string != null) return Arrays.stream(CutsceneMaker.getVarsArray(player,string)).map(Vars::getVar).toArray(String[]::new);
        return null;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @NotNull
    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
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
