package kor.toxicity.cutscenemaker.material;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.material.wrapped.CurrentMaterial;
import kor.toxicity.cutscenemaker.material.wrapped.LegacyMaterial;
import kor.toxicity.cutscenemaker.nms.NMSChecker;
import lombok.Getter;
import org.bukkit.Material;


public final class WrappedMaterial {
    private WrappedMaterial() {
        throw new RuntimeException();
    }
    @Getter
    private static CutsceneMaterial wrapper;
    @Getter
    private static WrappedBlockData signData;

    static  {
        try {
            if (NMSChecker.getVersion().isLegacy) {
                wrapper = new LegacyMaterial();
                try {
                    signData = WrappedBlockData.createData(wrapper.getSign());
                } catch (Exception e) {
                    signData = WrappedBlockData.createData(Material.valueOf("WALL_SIGN"));
                }
            } else {
                wrapper = new CurrentMaterial();
                signData = WrappedBlockData.createData(wrapper.getSign());
            }
        } catch (Exception e) {
            CutsceneMaker.warn("unable to load wrapped material.");
        }
    }
}
