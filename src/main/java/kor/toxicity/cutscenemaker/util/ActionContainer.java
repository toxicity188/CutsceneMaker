package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.mechanics.ActAction;
import kor.toxicity.cutscenemaker.actions.mechanics.ActEntry;
import kor.toxicity.cutscenemaker.events.ActionCancelEvent;
import kor.toxicity.cutscenemaker.events.enums.CancelCause;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ActionContainer {

    private final CutsceneMaker pl;
    private final List<CutsceneAction> actions;
    private Predicate<LivingEntity> conditions;
    @Getter
    private int record;

    private BukkitTask delay;
    private Consumer<LivingEntity> run;
    public List<Consumer<Map<String,ActionContainer>>> lateCheck;

    private final Map<LivingEntity,ActionRunning> tasks = new WeakHashMap<>();

    public ActionContainer(CutsceneMaker pl) {
        this.pl = pl;
        actions = new ArrayList<>();
    }

    public void add(CutsceneAction act) {
        actions.add(act);
        record += act.delay;

        if (act instanceof ActAction) {
            ActAction action = (ActAction) act;
            addCheckTask(m -> {
                if (!m.containsKey(action.name)) {
                    CutsceneMaker.warn("the action \"" + action.name + "\"does not exists!");
                    actions.remove(act);
                } else {
                    record += m.get(action.name).record;
                }
            });
        }
        if (act instanceof ActEntry) {
            ActEntry action = (ActEntry) act;
            addCheckTask(m -> {
                if (!m.containsKey(action.callback)) {
                    CutsceneMaker.warn("the action \"" + action.callback + "\"does not exists!");
                    actions.remove(act);
                } else {
                    record += Math.max(action.wait,1)*20;
                }
            });
        }
    }
    private void addCheckTask(Consumer<Map<String,ActionContainer>> check) {
        if (lateCheck == null) lateCheck = new ArrayList<>();
        lateCheck.add(check);
    }
    public void confirm() {
        if (record == 0) run = e -> {
            for (CutsceneAction action : actions) action.call(e);
        };
        else run = e -> {
            if (tasks.containsKey(e)) tasks.get(e).kill();
            tasks.put(e,new ActionRunning(e));
        };
    }

    public int size() {return actions.size();}
    public void setConditions(Predicate<LivingEntity> cond) {
        conditions = cond;
    }

    public void setCoolDown(int coolDown) {
        this.record = Math.max(coolDown * 20,4);
    }

    public boolean run(LivingEntity entity) {
        if ((conditions != null && !conditions.test(entity)) || delay != null) return false;
        delay = Bukkit.getScheduler().runTaskLater(pl,() -> delay = null,Math.max(record,4L));
        run.accept(entity);
        return true;
    }

    private class ActionRunning implements Listener {

        private BukkitTask task;
        private int loop;
        private final LivingEntity player;



        private ActionRunning(LivingEntity player) {
            this.player = player;
            EvtUtil.register(pl,this);
            load();
        }
        @EventHandler
        public void quit(PlayerQuitEvent e) {
            if (e.getPlayer().equals(player)) {
                kill();
                EvtUtil.call(new ActionCancelEvent(player,ActionContainer.this, CancelCause.QUIT));
            }
        }
        @EventHandler
        public void death(EntityDeathEvent e) {
            if (e.getEntity().equals(player)) {
                kill();
                EvtUtil.call(new ActionCancelEvent(player,ActionContainer.this, CancelCause.DEATH));
            }
        }


        private void kill() {
            if (task != null) task.cancel();
            EvtUtil.unregister(this);
            tasks.remove(player);
        }
        private void load() {
            if (loop < actions.size()) {
                CutsceneAction action = actions.get(loop);
                action.call(player);
                loop++;
                if (action.delay == 0) {
                    task = null;
                    load();
                }
                else task = Bukkit.getScheduler().runTaskLater(pl, this::load,action.delay);
            } else {
                kill();
            }
        }

    }

}