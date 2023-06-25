package kor.toxicity.cutscenemaker.nms;

import com.mojang.authlib.GameProfile;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface NMSHandler {
    NPC getFakePlayer(@NotNull GameProfile profile, @NotNull Location location);
}
