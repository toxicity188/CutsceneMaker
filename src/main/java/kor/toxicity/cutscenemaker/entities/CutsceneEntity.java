package kor.toxicity.cutscenemaker.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CutsceneEntity {
    private static final String METADATA_KEY = "CutsceneMob";
    @Getter
    private final LivingEntity entity;

    public void kill() {
        entity.remove();
    }

    public void setBound(Player player) {
        //TODO make a player bound code
    }

}
