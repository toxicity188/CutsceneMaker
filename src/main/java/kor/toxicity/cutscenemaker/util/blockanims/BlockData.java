package kor.toxicity.cutscenemaker.util.blockanims;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.Location;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class BlockData {
    @Getter
    private final Location location;

    @Getter
    private Material block;

    private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private PacketContainer packet;

    public BlockData(Location loc) {
        this(loc,true);
        createPacket();
    }
    public BlockData(Location loc, boolean setBlock) {
        location = loc;
        if (setBlock) block = loc.getBlock().getType();
    }
    private void createPacket() {
        packet = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0,new BlockPosition(location.toVector()));
        packet.getBlockData().write(0, WrappedBlockData.createData(block));
    }

    public void set() {
        location.getBlock().setType(block);
    }

    public void send(Player player) throws InvocationTargetException {
        manager.sendServerPacket(player,packet);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof BlockData) {
            BlockData var = (BlockData) obj;
            return location.equals(var.location) && block == var.block;
        } else return false;
    }

    @Override
    public int hashCode() {
        int hash = location.hashCode();
        hash = 31 * hash + block.ordinal();
        return hash;
    }

    public String getData() {
        return (int) location.getX() + "," + (int) location.getY() + "," + (int) location.getZ()  + "," + block.toString();
    }
    public static BlockData fromString(World world, String target) {
        try {
            String[] args = target.split(",");
            BlockData data = new BlockData(new Location(
                    world,
                    Double.parseDouble(args[1]),
                    Double.parseDouble(args[2]),
                    Double.parseDouble(args[3])),false);
            data.block = Material.getMaterial(args[5]);
            data.createPacket();
            return data;
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to load block data.");
            return null;
        }
    }
}
