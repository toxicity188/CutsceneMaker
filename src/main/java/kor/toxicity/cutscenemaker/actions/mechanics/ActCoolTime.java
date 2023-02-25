package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import lombok.RequiredArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class ActCoolTime extends CutsceneAction {

    private static final Map<UUID,CoolTimeMap> COOL_DOWN_MAP = new WeakHashMap<>();


    @DataField(aliases = {"c","cd"})
    public int cooldown;
    @DataField(aliases = "k",throwable = true)
    public String key;

    @Override
    public void initialize() {
        super.initialize();
        if (cooldown < 1) cooldown = 1;
    }

    public ActCoolTime(CutsceneManager pl) {
        super(pl);
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            OfflinePlayer p = (Player) entity;
            getCoolTimeMap(p).put(key,new CurrentCoolTime(p));
        }
    }

    private static CoolTimeMap getCoolTimeMap(OfflinePlayer player) {
        CoolTimeMap map = COOL_DOWN_MAP.get(player.getUniqueId());
        if (map == null) {
            map = new CoolTimeMap();
            COOL_DOWN_MAP.put(player.getUniqueId(),map);
        }
        return map;
    }

    private static class CoolTimeMap {
        private final Map<String,CurrentCoolTime> coolTimeMap = new HashMap<>();

        private void put(String key, CurrentCoolTime coolTime) {
            CurrentCoolTime currentCoolTime = coolTimeMap.put(key,coolTime);
            if (currentCoolTime != null) currentCoolTime.task.cancel();
        }
        private int getCoolTime(String key) {
            CurrentCoolTime current;
            return (((current = coolTimeMap.get(key)) != null) ? current.time : 0);
        }
        private void remove(String key) {
            coolTimeMap.remove(key);
        }
        private int size() {
            return coolTimeMap.size();
        }
    }
    @RequiredArgsConstructor
    private class CurrentCoolTime {
        private final OfflinePlayer player;
        private int time = cooldown;
        private final BukkitTask task = manager.runTaskTimer(() -> {
            if (--time == 0) cancel();
        },20, 20);

        private void cancel() {
            task.cancel();
            CoolTimeMap map = COOL_DOWN_MAP.get(player.getUniqueId());
            map.remove(key);
            if (map.size() == 0) COOL_DOWN_MAP.remove(player.getUniqueId());
        }
    }

    public static int getCoolTime(OfflinePlayer player, String key) {
        CoolTimeMap map = COOL_DOWN_MAP.get(player.getUniqueId());
        return (map != null) ? map.getCoolTime(key) : 0;
    }

}
