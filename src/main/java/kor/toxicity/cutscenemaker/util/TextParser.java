package kor.toxicity.cutscenemaker.util;

import org.bukkit.Location;

public class TextParser {

    private static final TextParser instance = new TextParser();

    public static TextParser getInstance() {
        return instance;
    }
    private TextParser() {

    }

    public String toSimpleLoc(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public String colored(String s) {
        return s.replaceAll("&","§");
    }
}
