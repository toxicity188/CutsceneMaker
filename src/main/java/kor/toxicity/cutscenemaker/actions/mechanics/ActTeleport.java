package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;

public class ActTeleport extends RepeatableAction {

    @DataField(aliases = "x")
    public double xCord = 0;
    @DataField(aliases = "y")
    public double yCord = 0;
    @DataField(aliases = "z")
    public double zCord = 0;
    @DataField(aliases = "p")
    public float pitch;
    @DataField
    public float yaw;

    @DataField(aliases = "sxo")
    public double startX;
    @DataField(aliases = "syo")
    public double startY;
    @DataField(aliases = "szo")
    public double startZ;

    @DataField(aliases = "w")
    public String world;

    @DataField(aliases = "sp")
    public double setPitch = -91;
    @DataField(aliases = "sy")
    public double setYaw = -181;

    @DataField(aliases = "o")
    public boolean orient = false;
    @DataField(aliases = "op")
    public boolean orientPitch = false;
    @DataField(aliases = "abs")
    public boolean absolute = false;
    @DataField(aliases = "s")
    public boolean surface = false;

    public ActTeleport(CutsceneManager pl) {
        super(pl);
        task = new HashMap<>();
    }


    private final Map<Entity,TeleportRecord> task;
    @Override
    public void initialize(LivingEntity entity) {
        TeleportRecord record = new TeleportRecord();
        record.loc = (absolute) ? new Location((world != null && Bukkit.getWorld(world) != null) ? Bukkit.getWorld(world) : entity.getWorld(), startX,startY,startZ) : entity.getLocation().clone().add(startX,startY,startZ);
        if (Math.abs(setPitch) <= 90) record.loc.setPitch(-(float) setPitch);
        if (Math.abs(setYaw) < 180) record.loc.setYaw((float) setYaw);
        if (orient) {
            double t = Math.toRadians(-record.loc.getYaw());

            record.x = (Math.sin(t) * xCord - Math.sin(t + Math.PI/2) * zCord) * getInterval();
            record.y = yCord * getInterval();
            record.z = (Math.cos(t) * xCord - Math.cos(t + Math.PI/2) * zCord) * getInterval();

            if (orientPitch) {
                double p = Math.toRadians(-record.loc.getPitch());
                double r = Math.sqrt(Math.cos(p));

                record.x *= r;
                record.z *= r;
                record.y *= Math.sin(p);
            }
        } else {
            record.x = xCord * getInterval();
            record.y = yCord * getInterval();
            record.z = zCord * getInterval();
        }
        record.pitch = -pitch * (float) getInterval();
        record.yaw = yaw * (float) getInterval();
        task.put(entity,record);
    }
    @Override
    public void update(LivingEntity player) {
        TeleportRecord record = task.get(player);
        record.loc.add(record.x,record.y,record.z);
        record.loc.setPitch(record.loc.getPitch() + record.pitch);
        record.loc.setYaw(record.loc.getYaw() + record.yaw);
        if (surface) while (record.loc.getBlock().getType() != Material.AIR) record.loc.add(0,1,0);
        player.teleport(record.loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    protected void end(LivingEntity end) {
        task.remove(end);
    }

    private static class TeleportRecord {
        private Location loc;
        private double x;
        private double y;
        private double z;
        private float pitch;
        private float yaw;
    }
}
