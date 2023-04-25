package kor.toxicity.cutscenemaker.event;

import kor.toxicity.cutscenemaker.quests.QuestSet;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class QuestTimeOverEvent extends PlayerEvent implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final QuestSet questSet;
    public QuestTimeOverEvent(Player who, QuestSet questSet) {
        super(who);
        this.questSet = questSet;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
