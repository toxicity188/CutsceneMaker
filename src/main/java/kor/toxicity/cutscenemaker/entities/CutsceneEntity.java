package kor.toxicity.cutscenemaker.entities;

import kor.toxicity.cutscenemaker.CutsceneManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CutsceneEntity {
    static final String METADATA_KEY = "cutscene_mob_bound";
    @Getter
    private final LivingEntity entity;

    public void kill() {
        entity.remove();
    }

    public void setBound(CutsceneManager manager, Player player) {
        entity.setMetadata(METADATA_KEY,manager.createMetaData(player));
    }

}
