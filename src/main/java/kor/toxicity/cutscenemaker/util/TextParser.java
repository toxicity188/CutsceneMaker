package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class
TextParser {
    @Getter
    private static final TextParser instance = new TextParser();

    public final String comma = ",";


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
    public String[] split(String t, String index) {
        return t.contains(index) ? t.split(index) : new String[] {t};
    }

    public String getEntityName(Entity e) {
        return (e.getName() != null && !e.getName().equals("")) ? uncolored(e.getName()) : e.getType().toString().toLowerCase();
    }

    public String uncolored(String s) {
        return ChatColor.stripColor(s);
    }
}
