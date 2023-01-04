package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.mechanics.ActEntry;
import kor.toxicity.cutscenemaker.events.ActionCancelEvent;
import kor.toxicity.cutscenemaker.events.enums.CancelCause;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ActionContainer {

    private final CutsceneMaker pl;
    private final List<CutsceneAction> actions;
    private Predicate<LivingEntity> conditions;
    @Getter
    private int record;

    private BukkitTask delay;
    private BiConsumer<LivingEntity,Map<String,String>> run;
    public List<Consumer<Map<String,ActionContainer>>> lateCheck;

    private final Map<LivingEntity,ActionRunning> tasks = new WeakHashMap<>();

    public ActionContainer(CutsceneMaker pl) {
        this.pl = pl;
        actions = new ArrayList<>();
    }

    public void add(CutsceneAction act) {
        actions.add(act);
        record += act.delay;

        if (act instanceof ActEntry) {
            ActEntry action = (ActEntry) act;
            addCheckTask(m -> {
                if (!m.containsKey(action.callback)) {
                    CutsceneMaker.warn("the Action \"" + action.callback + "\" doesn't exist!");
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
        if (record == 0) run = (e,m) -> {
            if (m != null && e instanceof Player) {
                Player p = (Player) e;
                m.forEach((k,v) -> pl.getManager().getVars(p).get(k).setVar(v));
                for (CutsceneAction action : actions) action.call(p);
                m.forEach((k,v) -> pl.getManager().getVars(p).remove(k));
            } else {
                for (CutsceneAction action : actions) action.call(e);
            }

        };
        else run = (e,m) -> {
            if (tasks.containsKey(e)) tasks.get(e).kill();
            if (m != null && e instanceof Player) {
                tasks.put(e, new ActionRunning((Player) e,m));
            } else {
                tasks.put(e, new ActionRunning(e));
            }
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
        return run(entity,null);
    }
    public boolean run(LivingEntity entity, Map<String,String> localVariables) {
        if ((conditions != null && !conditions.test(entity)) || delay != null) return false;
        delay = Bukkit.getScheduler().runTaskLater(pl,() -> delay = null, record);
        run.accept(entity,localVariables);
        return true;
    }

    private class ActionRunning implements Listener {

        private BukkitTask task;
        private int loop;
        private final LivingEntity player;

        private final Map<String,String> localVars;

        private ActionRunning(LivingEntity entity) {
            this.player = entity;
            localVars = null;
            EvtUtil.register(pl,this);
            load();
        }
        private ActionRunning(Player player, Map<String,String> localVariables) {
            this.player = player;
            localVars = localVariables;
            localVars.forEach((k,v) -> pl.getManager().getVars(player).get(k).setVar(v));
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
            if (localVars != null) localVars.forEach((k,v) -> pl.getManager().getVars((Player) player).remove(k));
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