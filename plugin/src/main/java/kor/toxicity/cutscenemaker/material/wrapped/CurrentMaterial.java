package kor.toxicity.cutscenemaker.material.wrapped;

import kor.toxicity.cutscenemaker.material.CutsceneMaterial;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class CurrentMaterial implements CutsceneMaterial {
    private final Material sign = Material.valueOf("OAK_SIGN");
    private final Material commandBlock = Material.valueOf("COMMAND_BLOCK");
    private final Material monsterEgg = Material.valueOf("GHAST_SPAWN_EGG");
}
