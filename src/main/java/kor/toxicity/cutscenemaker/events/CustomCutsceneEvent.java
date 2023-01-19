package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class CustomCutsceneEvent extends PlayerEvent implements ICutsceneEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final String key;

    public CustomCutsceneEvent(Player who, String key) {
        super(who);
        this.key = key;
        CutsceneMaker.debug( "CustomCutsceneEvent invoked by " + who.getName() + ".");
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
