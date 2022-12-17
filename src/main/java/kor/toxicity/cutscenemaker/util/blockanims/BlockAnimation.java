package kor.toxicity.cutscenemaker.util.blockanims;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BlockAnimation {
    private final List<BlockData3D> animations = new ArrayList<>();
    private final CutsceneManager manager;
    private final World world;
    private long delay = 10;

    public void add(Location from, Location to) {
        try {
            animations.add((animations.size() > 0) ? BlockData3D.overrider(animations.get(animations.size() - 1),from,to) : BlockData3D.of(from,to));
        } catch (Exception e) {
            CutsceneMaker.warn("unable to add block data.");
        }
    }

    public void send(Player player) {
        new AnimationRuntime(player);
    }
    public void init(Player player) {
        if (animations.size() > 0) manager.runTaskLaterAsynchronously(() -> animations.get(0).send(player),0);
    }

    public void setDelay(long delay) {
        this.delay = Math.max(delay,1);
    }

    private class AnimationRuntime implements Runnable {
        private int i = 0;
        private final Player player;
        private final BukkitTask task;

        private AnimationRuntime(Player player) {
            this.player = player;
            task = manager.runTaskTimerAsynchronously(this,delay,0);
        }
        @Override
        public void run() {
            if (player.isOnline() && !player.isDead() && i < animations.size()) {
                animations.get(i).send(player);
                i ++;
            } else {
                task.cancel();
            }
        }
    }

    public ConfigurationSection getSection() {
        ConfigurationSection section = new MemoryConfiguration();
        ConfigurationSection a = new MemoryConfiguration();
        int i = 1;
        for (BlockData3D animation : animations) {
            StringBuilder builder = new StringBuilder();
            for (BlockData datum : animation.getData()) {
                builder.append(datum.getData()).append("/");
            }
            a.set("anim" + i,builder.deleteCharAt(builder.length() -1).toString());
            i++;
        }
        section.set("world",world.getName());
        section.set("delay",delay);
        section.set("animations",a);
        return section;
    }
    public static BlockAnimation fromConfig(CutsceneManager manager, ConfigurationSection section) {
        try {
            World world = Bukkit.getWorld(section.getString("world"));

            BlockAnimation animation = new BlockAnimation(manager, world);

            ConfigurationSection section1 = section.getConfigurationSection("animations");
            for (String key : section1.getKeys(false)) {
                animation.animations.add(BlockData3D.fromString(world,section1.getString(key)));
            }
            animation.delay = section.getLong("delay");
            return animation;
        } catch (Exception e) {
            CutsceneMaker.warn("Unable to load block animation.");
            return null;
        }
    }
}
