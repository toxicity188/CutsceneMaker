package kor.toxicity.cutscenemaker.material.wrapped;

import kor.toxicity.cutscenemaker.material.CutsceneMaterial;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class LegacyMaterial implements CutsceneMaterial {
    private final Material sign = Material.valueOf("SIGN_POST");
    private final Material commandBlock = Material.valueOf("COMMAND");
    private final Material monsterEgg = Material.valueOf("MONSTER_EGG");
}
