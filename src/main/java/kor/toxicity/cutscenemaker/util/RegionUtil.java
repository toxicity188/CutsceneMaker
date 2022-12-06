package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Entity;
import org.codemc.worldguardwrapper.WorldGuardWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionUtil {
    @Getter
    private static final RegionUtil instance = new RegionUtil();

    public boolean inRegion(Entity entity, String name) {
        return WorldGuardWrapper.getInstance().getRegions(entity.getLocation()).stream().anyMatch(l -> l.getId().equals(name));
    }

}
