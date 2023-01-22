package kor.toxicity.cutscenemaker.util.blockanims;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BlockAnimation {
    private static final int DATA_LENGTH = 15; //2(material) + 1(data) + 4(x) + 4(y) + 4(z)
    final AnimationData[] data;

    private AnimationPacket normalPacket;
    private AnimationPacket airPacket;

    private BlockAnimation(int size) {
        data = new AnimationData[size];
    }

    public static BlockAnimation read(File file, World world) {
        try (FileInputStream fs = new FileInputStream(file); BufferedInputStream stream = new BufferedInputStream(fs)) {

            byte[] dataArray = new byte[DATA_LENGTH];

            int available = stream.available();
            if (available % 15 != 0) {
                CutsceneMaker.warn("Invalid File data: " + file.getName());
                return null;
            }
            int size = Math.floorDiv(available,DATA_LENGTH);
            BlockAnimation animation = new BlockAnimation(size);

            int buffer;
            int i = 0, g = 0;
            try {
                while ((buffer = stream.read()) != -1) {
                    if (i >= DATA_LENGTH) {
                        i = 0;
                        animation.data[g++] = getData(world, dataArray);
                    }
                    dataArray[i++] = (byte) buffer;
                }
                animation.data[g] = getData(world, dataArray);
                return animation;
            } catch (Exception e) {
                CutsceneMaker.warn("Invalid block data found in \"" + file.getName() + "\". maybe the minecraft version is incorrect.");
            }
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to load the file \"" + file.getName() + "\".");
        }
        return null;
    }
    private static final Material[] MATERIAL_VALUES = Material.values();
    private static AnimationData getData(World world, byte[] dataArray) {
        AnimationData anim = new AnimationData();
        anim.world = world;
        anim.material = MATERIAL_VALUES[getShortFromArray(dataArray[0],dataArray[1])];
        anim.data = dataArray[2];
        anim.x = getIntFromArray(dataArray[3],dataArray[4],dataArray[5],dataArray[6]);
        anim.y = getIntFromArray(dataArray[7],dataArray[8],dataArray[9],dataArray[10]);
        anim.z = getIntFromArray(dataArray[11],dataArray[12],dataArray[13],dataArray[14]);
        return anim;
    }
    private static int getShortFromArray(byte one, byte two) {
        return ((0xFF & one) << 8) | (0xFF & two);
    }
    private static int getIntFromArray(byte one, byte two, byte three, byte four) {
        return ((0xFF & one) << 24) | ((0xFF & two) << 16) | ((0xFF & three) << 8) | (0xFF & four);
    }
    public AnimationPacket toPacket() {
        if (normalPacket == null) normalPacket = AnimationPacket.createData(this);
        return normalPacket;
    }
    public AnimationPacket toAirPacket() {
        if (airPacket == null) airPacket = AnimationPacket.createAirData(this);
        return airPacket;
    }
    @SuppressWarnings("deprecation")
    public static BlockAnimation get(World world, Vector from, Vector to) {
        int fromX = from.getBlockX();
        int fromY = from.getBlockY();
        int fromZ = from.getBlockZ();

        int toX = to.getBlockX();
        int toY = to.getBlockY();
        int toZ = to.getBlockZ();

        int xSize = Math.abs(toX - fromX) + 1;
        int ySize = Math.abs(toY - fromY) + 1;
        int zSize = Math.abs(toZ - fromZ) + 1;

        List<AnimationData> animations = new ArrayList<>();
        int t1 = (fromX <= toX) ? 1 : -1;
        int t2 = (fromY <= toY) ? 1 : -1;
        int t3 = (fromZ <= toZ) ? 1 : -1;
        for (int x = 0; x < xSize && x > -xSize; x += t1) {
            for (int y = 0; y < ySize && y > -ySize; y += t2) {
                for (int z = 0; z < zSize && z > -zSize; z += t3) {
                    int r1 = fromX + x, r2 = fromY + y, r3 = fromZ + z;
                    Block block = world.getBlockAt(r1,r2,r3);
                    if (block.getType() != Material.AIR) {
                        AnimationData data1 = new AnimationData();
                        data1.world = world;
                        data1.x = r1;
                        data1.y = r2;
                        data1.z = r3;
                        data1.material = block.getType();
                        data1.data = block.getData();
                        animations.add(data1);
                    }
                }
            }
        }
        BlockAnimation anim = new BlockAnimation(animations.size());
        int i = 0;
        for (AnimationData animation : animations) {
            anim.data[i++] = animation;
        }
        return anim;
    }
    public void write(File file) {
        try (FileOutputStream fs = new FileOutputStream(file); BufferedOutputStream stream = new BufferedOutputStream(fs)) {
            byte[] array = new byte[data.length * DATA_LENGTH];
            int i = 0;
            for (AnimationData datum : data) {
                short m = (short) datum.material.ordinal();
                array[i++] = (byte) (m >>> 8);
                array[i++] = (byte) ((m << 8) >>> 8);
                array[i++] = datum.data;
                writeArray(array, i, datum.x);
                writeArray(array, i += 4, datum.y);
                writeArray(array, i += 4, datum.z);
                i += 4;
            }
            stream.write(array);
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to save the file \"" + file.getName() + "\"");
        }
    }
    public void set() {
        for (AnimationData datum : data) {
            datum.set();
        }
    }
    public void setToAir() {
        for (AnimationData datum : data) {
            datum.setToAir();
        }
    }
    private static void writeArray(byte[] array, int length, int i) {
        array[length++] = (byte) (i >>> 24);
        array[length++] = (byte) ((i << 8) >>> 24);
        array[length++] = (byte) ((i << 16) >>> 24);
        array[length] = (byte) ((i << 24) >>> 24);
    }

    @ToString
    @EqualsAndHashCode
    static class AnimationData {
        World world;
        int x;
        int y;
        int z;
        Material material;
        byte data;

        @SuppressWarnings("deprecation")
        private void set() {
            BlockState state = world.getBlockAt(x,y,z).getState();
            state.setType(material);
            state.setRawData(data);
            state.update(true);
        }
        private void setToAir() {
            BlockState state = world.getBlockAt(x,y,z).getState();
            state.setType(Material.AIR);
            state.update(true);
        }
    }
}
