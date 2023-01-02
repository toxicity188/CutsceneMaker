package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.quests.Dialog;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class DialogStartEvent extends PlayerEvent implements ICutsceneEvent, Cancellable {
    @Getter
    private static final HandlerList handlerList = new HandlerList();

    @Getter
    @Setter
    private boolean cancelled = false;
    @Getter
    private final Dialog dialog;

    public DialogStartEvent(Player who, Dialog dialog) {
        super(who);
        this.dialog = dialog;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
