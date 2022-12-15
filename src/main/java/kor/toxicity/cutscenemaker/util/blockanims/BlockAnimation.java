package kor.toxicity.cutscenemaker.util.blockanims;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BlockAnimation {
    private final List<BlockData3D> animations = new ArrayList<>();
    private final CutsceneManager manager;
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
}
