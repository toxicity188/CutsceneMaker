package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextParser {

    private static final TextParser instance = new TextParser();

    public static TextParser getInstance() {
        return instance;
    }

    public String toSimpleLoc(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public String colored(String s) {
        return s.replaceAll("&","§");
    }
}
