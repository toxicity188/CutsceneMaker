package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.DataContainer;
import kor.toxicity.cutscenemaker.util.blockanims.BlockAnimation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.io.File;
import java.util.Map;

public final class LocationData extends CutsceneData {

    public LocationData(CutsceneMaker pl) {
        super(pl);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void reload() {
        DataContainer<Location> loc = getPlugin().getManager().getLocations();
        Map<String, BlockAnimation> animationMap = getPlugin().getManager().getAnimationMap();
        animationMap.clear();
        loc.clear();
        ConfigLoad config = getPlugin().read("Locations");
        config.forEach((f,n) -> n.forEach(s -> {
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
                loc.put(f,s,save);
            } else CutsceneMaker.warn("unable to find world \""+ world +"\"");
        }));
        String path = getPlugin().getDataFolder().getAbsolutePath();
        new File(path + "\\Animation").mkdir();
        Bukkit.getWorlds().forEach(w -> {
            File dir = new File(path + "\\Animation\\" + w.getName());
            dir.mkdir();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    BlockAnimation animation = BlockAnimation.read(file,w);
                    String name = file.getName();
                    if (!name.endsWith(".anim")) continue;
                    if (animation != null) animationMap.put(file.getName().substring(0,name.length() - ".anim".length()),animation);
                }
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(loc.size()) + " Locations successfully loaded.");
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(animationMap.size()) + " Block animations successfully loaded.");
    }
}
