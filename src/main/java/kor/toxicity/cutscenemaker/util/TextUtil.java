package kor.toxicity.cutscenemaker.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class TextUtil {
    private TextUtil() {
        throw new RuntimeException();
    }
    public static final String COMMA = ",";


    public static String toSimpleLoc(Location loc) {
        return loc.getBlockX() + COMMA + loc.getBlockY() + COMMA + loc.getBlockZ();
    }

    public static String colored(String s) {
        return s.replace('&','§');
    }

    public static String toSingleText(List<String> text) {
        StringBuilder a = new StringBuilder();
        int l = 0;
        for (String s : text) {
            a.append(s);
            l ++;
            if (l < text.size()) a.append(COMMA);
        }
        return a.toString();
    }
    public static String[] split(String t, String index) {
        return t.contains(index) ? t.split(index) : new String[] {t};
    }

    public static String getEntityName(Entity e) {
        return (e.getName() != null && !e.getName().equals("")) ? uncolored(e.getName()) : e.getType().toString().toLowerCase();
    }
    public static String getItemName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        String display;
        return stack.getAmount() + " of " + (((display = meta.getDisplayName()) != null) ? display : stack.getType().toString().toLowerCase());
    }

    public static String uncolored(String s) {
        return ChatColor.stripColor(s);
    }

    public static String applyComma(double d) {
        return new DecimalFormat().format(d);
    }

    public static Function<LivingEntity,Location> getBlockLocation(String world, String location) {
        if (location != null && location.contains(",")) {
            try {
                double[] d = Arrays.stream(location.split(",")).mapToDouble(Double::parseDouble).toArray();
                if (d.length >= 3) {
                    if (world != null && Bukkit.getWorld(world) != null) {
                        Location loc = new Location(Bukkit.getWorld(world), d[0], d[1], d[2]);
                        return e -> loc;
                    } else {
                        return e -> new Location(e.getLocation().getWorld(),d[0],d[1],d[2]);
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
