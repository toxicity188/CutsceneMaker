package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class UserDataLoadEvent extends PlayerEvent implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();

    public UserDataLoadEvent(Player who) {
        super(who);
        CutsceneMaker.debug( who.getName() + " joined.");
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
