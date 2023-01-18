package kor.toxicity.cutscenemaker.util.blockanims;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class AnimationPacket {

    private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private final PacketContainer[] containers;
    private static WrappedBlockData airData;


    static AnimationPacket createData(BlockAnimation animation) {
        return new AnimationPacket(animation,false);
    }
    static AnimationPacket createAirData(BlockAnimation animation) {
        if (airData == null) airData = WrappedBlockData.createData(Material.AIR);
        return new AnimationPacket(animation,true);
    }
    public void send(Player player) {
        try {
            for (PacketContainer container : containers) {
                manager.sendServerPacket(player,container);
            }
        } catch (Exception ignored) {}
    }

    private AnimationPacket(BlockAnimation animation, boolean air) {
        containers = Arrays.stream(animation.data).map(d -> {
            PacketContainer container = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            container.getBlockData().write(0,(air) ? airData : WrappedBlockData.createData(d.material,d.data));
            container.getBlockPositionModifier().write(0,new BlockPosition(d.x,d.y,d.z));
            return container;
        }).toArray(PacketContainer[]::new);
    }
}
