package kor.toxicity.cutscenemaker.util;

import com.sk89q.worldguard.bukkit.WGBukkit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegionUtil {
    @Getter
    private static final RegionUtil instance = new RegionUtil();

    public boolean inRegion(Entity entity, String name, String world) {
        return WGBukkit.getRegionManager(world != null && Bukkit.getWorld(world) != null ? Bukkit.getWorld(world) : entity.getWorld()).getRegion(name) != null;
    }

}
