package kor.toxicity.cutscenemaker.nms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NMSChecker {
    @Getter
    @NotNull
    private static final Version version;

    public static @Nullable NMSHandler getHandler() {
        return handler;
    }

    private static final NMSHandler handler;
    static {
        version = parseVersion();
        handler = parseHandler();
    }
    private static Version parseVersion() {
        try {
            return Version.valueOf(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);
        } catch (Exception e) {
            return Version.UNSUPPORTED;
        }
    }
    private static NMSHandler parseHandler() {
        try {
            return (NMSHandler) Class.forName("kor.toxicity.cutscenemaker.nms." + version + ".NMSImpl").getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            return null;
        }
    }
    private NMSChecker() {
        throw new RuntimeException();
    }

    @RequiredArgsConstructor
    public enum Version {
        UNSUPPORTED(-1, -1,false),
        v1_12_R1(12, 1,true),
        v1_13_R1(13, 1,false),
        v1_13_R2(13, 2,false),
        v1_14_R1(14, 1,false),
        v1_15_R1(15, 1,false),
        v1_16_R1(16, 1,false),
        v1_16_R2(16, 2,false),
        v1_17_R1(17, 1,false),
        v1_18_R1(18, 1,false),
        v1_18_R2(18, 2,false),
        v1_19_R1(19, 1,false),
        v1_19_R2(19, 2,false),
        v1_19_R3(19, 3,false)
        ;

        public final int version;
        public final int release;
        public final boolean isLegacy;
    }
    
    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + version + "." + name);
    }
    public static Class<?> getCraftBukkitClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + version + "." + name);
    }
    public static Class<?> getPrimitiveDouble() {
        return Double.TYPE;
    }
    public static Class<?> getPrimitiveFloat() {
        return Float.TYPE;
    }
}
