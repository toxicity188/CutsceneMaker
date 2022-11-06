package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.Map;

public final class LocationData extends CutsceneData {

    public LocationData(CutsceneMaker pl) {
        super(pl);
    }

    @Override
    public void reload() {
        Map<String, Location> loc = getPlugin().getManager().getLocations();
        loc.clear();
        ConfigLoad config = getPlugin().read("Locations");
        config.getAllFiles().forEach(s -> {
            double x = config.getDouble(s + ".x",0D);
            double y = config.getDouble(s + ".y",0D);
            double z = config.getDouble(s + ".z",0D);
            float pitch = config.getFloat(s + ".pitch",0F);
            float yaw = config.getFloat(s + ".yaw",0F);
            String world = config.getString(s + ".world","world");
            if (Bukkit.getWorld(world) != null) {
                Location save = new Location(Bukkit.getWorld(world),x,y,z);
                save.setPitch(pitch);
                save.setYaw(yaw);
                loc.put(s,save);
            } else CutsceneMaker.warn("unable to find world \""+ world +"\"");
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(loc.size()) + " locations successfully loaded.");
    }
}
