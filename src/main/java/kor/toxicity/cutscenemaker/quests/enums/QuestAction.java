package kor.toxicity.cutscenemaker.quests.enums;

import kor.toxicity.cutscenemaker.quests.QuestSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public enum QuestAction {
    GIVE(QuestSet::give), //give quest
    COMPLETE(QuestSet::complete), //remove quest without any rewards.
    REMOVE(QuestSet::remove), //remove quest and give all rewards to player.
    ;
    @Getter
    private final BiConsumer<QuestSet, Player> task;
}
