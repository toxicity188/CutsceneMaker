package kor.toxicity.cutscenemaker.material;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.material.wrapped.CurrentMaterial;
import kor.toxicity.cutscenemaker.material.wrapped.LegacyMaterial;
import lombok.Getter;
import org.bukkit.Bukkit;

public class WrappedMaterial {

    @Getter
    private static final WrappedMaterial instance = new WrappedMaterial();
    @Getter
    private CutsceneMaterial wrapper;
    public WrappedMaterial() {
        try {
            int version = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].split("_")[1]);
            if (version <= 12) {
                wrapper = new LegacyMaterial();
            } else {
                wrapper = new CurrentMaterial();
            }
        } catch (Exception e) {
            CutsceneMaker.warn("unable to load wrapped material.");
        }
    }

}
