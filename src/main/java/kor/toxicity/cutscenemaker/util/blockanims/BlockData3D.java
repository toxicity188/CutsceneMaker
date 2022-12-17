package kor.toxicity.cutscenemaker.util.blockanims;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BlockData3D {
    @Getter
    private final Set<BlockData> data;

    public static BlockData3D of(Location from, Location to) {
        if (!from.getWorld().equals(to.getWorld())) throw new IllegalStateException();
        return new BlockData3D(Objects.requireNonNull(from), Objects.requireNonNull(to));
    }
    public static BlockData3D overrider(BlockData3D def, Location from, Location to) {
        BlockData3D data3D = of(from, to);
        for (BlockData datum : def.data) {
            data3D.data.remove(datum);
        }
        if (data3D.data.isEmpty()) throw new IllegalStateException();
        return data3D;
    }

    private BlockData3D(int size) {
        data = new HashSet<>(size);
    }
    private BlockData3D(Location from, Location to) {
        World world = from.getWorld();

        int fromX = (int) Math.floor(from.getX());
        int fromY = (int) Math.floor(from.getY());
        int fromZ = (int) Math.floor(from.getZ());

        int toX = (int) Math.floor(to.getX());
        int toY = (int) Math.floor(to.getY());
        int toZ = (int) Math.floor(to.getZ());

        int xSize = Math.abs(toX - fromX) + 1;
        int ySize = Math.abs(toY - fromY) + 1;
        int zSize = Math.abs(toZ - fromZ) + 1;
        data = new HashSet<>(xSize * ySize * zSize);

        for (int x = 0; x < xSize; x += (fromX <= toX) ? 1 : -1) {
            for (int y = 0; y < ySize; y += (fromY <= toY) ? 1 : -1) {
                for (int z = 0; z < zSize; z += (fromZ <= toZ) ? 1 : -1) {
                    data.add(new BlockData(new Location(world,fromX + x,fromY + y,fromZ + z)));
                }
            }
        }
    }

    public void blockSet() {
        for (BlockData datum : data) {
            datum.set();
        }
    }
    public void send(Player player) {
        try {
            for (BlockData datum : data) {
                datum.send(player);
            }
        } catch (Exception e) {
            CutsceneMaker.warn("unable to send block animation.");
        }
    }
    public boolean contains(BlockData value) {
        return data.contains(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof BlockData3D) {
            BlockData3D var = (BlockData3D) obj;
            return (data.containsAll(var.data));
        } else return false;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (BlockData datum : data) {
            hash = 31 * hash + datum.hashCode();
        }
        return hash;
    }

    public static BlockData3D fromList(World world, List<String> list) {
        try {
            BlockData3D data3D = new BlockData3D(list.size());
            for (String s : list) {
                data3D.data.add(BlockData.fromString(world,s));
            }
            return data3D;
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to load 3D block data.");
            return null;
        }
    }
    public static BlockData3D fromString(World world, String string) {
        try {
            String[] t = string.split("/");
            BlockData3D data3D = new BlockData3D(t.length);
            for (String s : t) {
                data3D.data.add(BlockData.fromString(world,s));
            }
            return data3D;
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to load 3D block data.");
            return null;
        }
    }
}
