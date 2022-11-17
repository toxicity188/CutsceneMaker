package kor.toxicity.cutscenemaker.actions;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public abstract class RepeatableAction extends CutsceneAction{

    @DataField(aliases = "i")
    public int interval = 1;
    @DataField(aliases = "t")
    public int ticks = 1;
    @DataField(aliases = "async")
    public boolean asynchronous = true;

    private final Map<LivingEntity, RepeatableRun> tasks = new HashMap<>();
    private final CutsceneManager manager;

    public RepeatableAction(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void apply(LivingEntity entity) {
        initialize(entity);
        if (ticks <= 1) {
            update(entity);
            end(entity);
        }
        else {
            if (tasks.containsKey(entity)) tasks.get(entity).kill();
            new RepeatableRun(entity);
        }
    }

    public final void cancel(LivingEntity entity) {
        if (tasks.containsKey(entity)) tasks.get(entity).kill();
        tasks.remove(entity);
        end(entity);
    }

    protected abstract void initialize(LivingEntity entity);
    protected abstract void update(LivingEntity entity);
    protected abstract void end(LivingEntity end);

    private final class RepeatableRun implements Runnable {

        private final LivingEntity entity;
        private final BukkitTask task;
        private int loop;
        private RepeatableRun(LivingEntity entity) {
            this.entity = entity;
            if (asynchronous) task = manager.runTaskTimerAsynchronously(this,0,interval);
            else task = manager.runTaskTimer(this,0,interval);
            tasks.put(entity,this);
            initialize(entity);

        }

        @Override
        public void run() {
            loop ++;
            if (ticks < loop || entity.isDead() || !entity.isValid()) {
                kill();
                return;
            }
            update(entity);
        }

        private void kill() {
            task.cancel();
            tasks.remove(entity);
            end(entity);
        }
    }

    public final int getDelay() {
        return interval*ticks-1;
    }
    protected final double getInterval() {
        return (double) interval / (double) ticks;
    }
}
