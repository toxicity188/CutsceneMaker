package kor.toxicity.cutscenemaker.skript.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.quests.Dialog;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.quests.QuestUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public class EffRunDialog extends CutsceneEffect {

    private Expression<String> stringExpression;
    private Expression<Player> playerExpression;
    private Expression<String> talker;
    private Expression<String> sound;

    @Override
    protected void execute(@NotNull Event e) {
        String s = stringExpression.getSingle(e);
        Player player = playerExpression.getSingle(e);
        String t = talker.getSingle(e);
        if (s != null && player != null && t != null) {
            Dialog d = QuestData.getDialog(s);
            if (d != null) d.run(player,t, Optional.ofNullable(sound).map(q -> q.getSingle(e)).map(QuestUtil::getSoundPlay).orElse(null));
        }
    }

    @NotNull
    @Override
    public String toString(Event e, boolean debug) {
        return "run some action to some living entity: " +
                stringExpression.toString(e,debug)
                + ", " + playerExpression.toString(e,debug)
                + ", " + talker.toString(e,debug)
                + ", " + ((sound != null) ? sound.toString(e,debug) : "null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull SkriptParser.ParseResult parseResult) {
        stringExpression = (Expression<String>) exprs[0];
        playerExpression = (Expression<Player>) exprs[1];
        talker = (Expression<String>) exprs[2];
        sound = (exprs.length > 3) ? (Expression<String>) exprs[3] : null;
        return true;
    }
}
