package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextParser {

    private static final TextParser instance = new TextParser();

    public final String comma = ",";

    public static TextParser getInstance() {
        return instance;
    }

    public String toSimpleLoc(Location loc) {
        return loc.getX() + comma + loc.getY() + comma + loc.getZ();
    }

    public String colored(String s) {
        return s.replaceAll("&","§");
    }

    public String toSingleText(List<String> text) {
        StringBuilder a = new StringBuilder();
        int l = 0;
        for (String s : text) {
            a.append(s);
            l ++;
            if (l < text.size()) a.append(comma);
        }
        return a.toString();
    }
}
