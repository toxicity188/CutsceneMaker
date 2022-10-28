package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;

public class HandlerJoin implements ActionHandler {
    @Override
    public Class<? extends Event> getEventClass() {
        return PlayerJoinEvent.class;
    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean check(Player player) {
        return false;
    }
}
