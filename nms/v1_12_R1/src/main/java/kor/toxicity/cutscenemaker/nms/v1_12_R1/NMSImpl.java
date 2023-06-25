package kor.toxicity.cutscenemaker.nms.v1_12_R1;

import com.mojang.authlib.GameProfile;
import kor.toxicity.cutscenemaker.nms.NMSHandler;
import kor.toxicity.cutscenemaker.nms.NPC;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftArmorStand;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

public class NMSImpl implements NMSHandler {

    @Override
    public NPC getFakePlayer(@NotNull GameProfile profile, @NotNull Location location) {
        WorldServer server = ((CraftWorld) location.getWorld()).getHandle();
        EntityArmorStand stand = new EntityArmorStand(server);
        stand.setLocation(location.getX(),location.getY(),location.getZ(), location.getYaw(), location.getPitch());

        server.addEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return new CraftNPC((CraftServer) Bukkit.getServer(),stand);
    }

    private static class CraftNPC extends CraftArmorStand implements NPC {

        public CraftNPC(CraftServer server, EntityArmorStand entity) {
            super(server, entity);
        }
    }
}
