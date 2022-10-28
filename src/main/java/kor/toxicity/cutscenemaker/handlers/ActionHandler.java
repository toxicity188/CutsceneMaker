package kor.toxicity.cutscenemaker.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public interface ActionHandler {

    Class<? extends Event> getEventClass();
    boolean check(final Player player);
    void initialize();
}
