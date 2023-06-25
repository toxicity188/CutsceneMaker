package kor.toxicity.cutscenemaker.skript.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class EffGiveQuest extends CutsceneEffect {

    private Expression<QuestSet> questSetExpression;
    private Expression<Player> playerExpression;

    @Override
    protected void execute(@NotNull Event e) {
        QuestSet questSet = questSetExpression.getSingle(e);
        Player player = playerExpression.getSingle(e);
        if (questSet == null || player == null) return;
        questSet.give(player);
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "quest some QuestSet to some player: " + questSetExpression.toString(e,debug) + ", " + playerExpression.toString(e,debug);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(@NotNull Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        questSetExpression = (Expression<QuestSet>) exprs[0];
        playerExpression = (Expression<Player>) exprs[1];
        return true;
    }
}
