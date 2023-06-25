package kor.toxicity.cutscenemaker.event;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.quests.Dialog;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class DialogStartEvent extends PlayerEvent implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final Dialog dialog;

    public DialogStartEvent(Player who, Dialog dialog) {
        super(who);
        CutsceneMaker.debug( "Dialog started by " + who.getName() + ".");
        this.dialog = dialog;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
