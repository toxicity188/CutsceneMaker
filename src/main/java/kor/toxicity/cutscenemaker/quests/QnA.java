package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.util.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class QnA {
    private final Map<Integer,Button> buttonMap = new HashMap<>();

    public QnA(ConfigurationSection section) {

    }
    @RequiredArgsConstructor
    private static class Button {
        private final int slot;
        private final ItemBuilder builder;
        private final Dialog[] dialogs;
    }
}
