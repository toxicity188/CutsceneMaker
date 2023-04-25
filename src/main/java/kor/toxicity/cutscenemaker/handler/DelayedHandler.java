package kor.toxicity.cutscenemaker.handler;

import org.bukkit.entity.Player;

import java.util.Map;

public interface DelayedHandler {
    Map<Player,Long> getTimeMap();
}
