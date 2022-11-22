package kor.toxicity.cutscenemaker.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;

@RequiredArgsConstructor
public class CutsceneEntity {
    private static final String METADATA_KEY = "CutsceneMob";
    @Getter
    private final LivingEntity entity;

    public void kill() {
        entity.remove();
    }

}
