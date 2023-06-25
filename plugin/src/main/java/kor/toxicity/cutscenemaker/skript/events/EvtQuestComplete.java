package kor.toxicity.cutscenemaker.skript.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import kor.toxicity.cutscenemaker.event.QuestCompleteEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class EvtQuestComplete extends SkriptEvent implements ICutsceneSkriptEvent {

    private Literal<String> stringLiteral;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult) {
        stringLiteral = (Literal<String>) args[0];
        return true;
    }

    @Override
    public boolean check(@NotNull Event e) {
        if (stringLiteral != null) {
            String name = ((QuestCompleteEvent) e).getQuestSet().getName();
            return stringLiteral.check(e, name::equals);
        }
        return true;
    }

    @Override
    public @NotNull String toString(Event e, boolean debug) {
        return "Quest Complete Event: " + ((stringLiteral != null) ? stringLiteral.toString(e,debug): "null");
    }
}
