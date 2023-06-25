package kor.toxicity.cutscenemaker.util;

import org.bukkit.entity.Entity;
import org.codemc.worldguardwrapper.WorldGuardWrapper;

public final class RegionUtil {
    private RegionUtil() {
        throw new RuntimeException();
    }

    public static boolean inRegion(Entity entity, String name) {
        return WorldGuardWrapper.getInstance().getRegions(entity.getLocation()).stream().anyMatch(l -> l.getId().equals(name));
    }

}
