package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.DataContainer;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.blockanims.BlockAnimation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.Map;

public final class LocationData extends CutsceneData implements Listener {

    public LocationData(CutsceneMaker pl) {
        super(pl);
        EvtUtil.register(pl,this);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        LocationStudio.quit(e.getPlayer());
    }
    @EventHandler
    public void death(PlayerDeathEvent e) {
        LocationStudio.quitWithoutBack(e.getEntity());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void reload() {
        CutsceneManager manager = getPlugin().getManager();
        //Location
        DataContainer<Location> loc = manager.getLocations();
        Map<String, BlockAnimation> animationMap = manager.getAnimationMap();
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
        //Studio
        Map<String,LocationStudio> studioMap = manager.getStudioMap();
        studioMap.clear();
        ConfigLoad load = getPlugin().read("Studio");
        load.getAllFiles().forEach(s -> {
            ConfigurationSection section = load.getConfigurationSection(s);
            if (section != null) {
                LocationStudio.fromConfig(s,manager,section).ifPresent(c -> studioMap.put(s,c));
            }
        });

        //Block Animation
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
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(studioMap.size()) + " Studios successfully loaded.");
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(animationMap.size()) + " Block animations successfully loaded.");
    }
}
