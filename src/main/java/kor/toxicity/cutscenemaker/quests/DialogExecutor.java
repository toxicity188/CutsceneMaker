package kor.toxicity.cutscenemaker.quests;

import org.bukkit.entity.Player;

public interface DialogExecutor {
    void invoke(Player player, String message);
}
