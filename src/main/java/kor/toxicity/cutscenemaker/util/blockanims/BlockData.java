package kor.toxicity.cutscenemaker.util.blockanims;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.Getter;
import org.bukkit.Location;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class BlockData {
    @Getter
    private final Location location;

    @Getter
    private final Material block;

    private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private final PacketContainer packet;

    public BlockData(Location loc) {

        location = loc;
        block = loc.getBlock().getType();

        packet = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0,new BlockPosition(loc.toVector()));
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
}
