package kor.toxicity.cutscenemaker.handlers.enums;

import lombok.Getter;
import org.bukkit.event.block.Action;

public enum EventClickType {

    LEFT(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK),
    RIGHT(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK)
    ;
    @Getter
    final Action[] act;

    EventClickType(Action... a) {
        act = a;
    }
}
