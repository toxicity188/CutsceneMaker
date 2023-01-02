package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.quests.QuestSet;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class QuestCompleteEvent extends PlayerEvent implements ICutsceneEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    private final QuestSet questSet;
    @Getter
    @Setter
    private double money, exp;

    public QuestCompleteEvent(Player who, QuestSet questSet) {
        super(who);
        this.questSet = questSet;
        money = questSet.getMoney();
        exp = questSet.getExp();
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
