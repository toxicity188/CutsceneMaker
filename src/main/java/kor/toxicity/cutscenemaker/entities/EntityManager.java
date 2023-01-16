package kor.toxicity.cutscenemaker.entities;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntityManager implements Listener {
    @Getter
    private static final EntityManager instance = new EntityManager();

    private final Map<String, CutsceneEntity> entityMap = new ConcurrentHashMap<>();
    private final Map<Player, PlayerBoundMob> boundMobMap = new ConcurrentHashMap<>();

    static ListenerManager LISTENER;
    public void setExecutor(CutsceneMaker maker) {
        EvtUtil.register(maker, this);
        LISTENER = new ListenerManager(maker);
        Bukkit.getOnlinePlayers().forEach(this::put);
    }
    public CutsceneEntity get(String key) {
        return entityMap.get(key);
    }
    public CutsceneEntity get(Player player, String key) {
        return Optional.ofNullable(boundMobMap.get(player)).map(b -> b.entityMap.get(key)).orElse(null);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        put(e.getPlayer());
    }
    private void put(Player p) {
        boundMobMap.put(p,new PlayerBoundMob());
    }
    @EventHandler
    public void quit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PlayerBoundMob map = boundMobMap.get(p);
        if (map != null) {
            map.remove();
            boundMobMap.remove(p);
        }
    }
    @EventHandler
    public void death(PlayerDeathEvent e) {
        Player p = e.getEntity();
        PlayerBoundMob map = boundMobMap.get(p);
        if (map != null) {
            map.remove();
        }
    }

    public void createMob(Player player, String key, EntityType type, Location location) {
        CutsceneEntity entity = a(type,location);
        c(player,m -> m.entityMap.put(key,entity));
    }
    public void createMob(String key, EntityType type, Location location) {
        entityMap.put(key,a(type,location));
    }

    public void createMythicMob(Player player, String key, String type, Location location) {
        CutsceneEntity entity = b(type,location);
        if (entity != null) c(player,m -> m.entityMap.put(key,entity));
    }
    public void createMythicMob(String key, String type, Location location) {
        CutsceneEntity entity = b(type,location);
        if (entity != null) entityMap.put(key,entity);
    }

    private CutsceneEntity a(EntityType type, Location location) {
        return new CutsceneEntity((LivingEntity) location.getWorld().spawnEntity(location,type));
    }

    private CutsceneEntity b(String key, Location location) {
        try {
            MythicMob mob = MythicMobs.inst().getMobManager().getMythicMob(key);
            if (mob == null) {
                CutsceneMaker.warn("The Mob named \"" + key + "\" doesn't exist!");
                return null;
            }
            return new CutsceneEntity((LivingEntity) mob.spawn(BukkitAdapter.adapt(location),1.0).getEntity().getBukkitEntity());
        } catch (Exception e) {
            CutsceneMaker.warn("unable to find MythicMobs plugin.");
            return null;
        }
    }
    private void c(Player p, Consumer<PlayerBoundMob> e) {
        Optional.ofNullable(boundMobMap.get(p)).ifPresent(e);
    }

    public void remove(String key) {
        CutsceneEntity entity = entityMap.get(key);
        if (entity != null) {
            entity.kill();
            entityMap.remove(key);
        }
    }
    public void remove(Player player, String key) {
        c(player,e -> {
            CutsceneEntity entity = e.entityMap.get(key);
            if (entity != null) {
                entity.kill();
                e.entityMap.remove(key);
            }
        });
    }
    public void remove() {
        entityMap.values().forEach(CutsceneEntity::kill);
        entityMap.clear();
    }

    private static class PlayerBoundMob {
        private final Map<String,CutsceneEntity> entityMap = new WeakHashMap<>();

        public void remove() {
            entityMap.values().forEach(CutsceneEntity::kill);
            entityMap.clear();
        }
    }
}
