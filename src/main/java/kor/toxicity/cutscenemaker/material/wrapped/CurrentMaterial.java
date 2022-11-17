package kor.toxicity.cutscenemaker.material.wrapped;

import kor.toxicity.cutscenemaker.material.CutsceneMaterial;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class CurrentMaterial implements CutsceneMaterial {
    private final Material sign = Material.valueOf("BIRCH_SIGN");
}
