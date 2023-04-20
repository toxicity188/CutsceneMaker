package kor.toxicity.cutscenemaker.entities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EntityManager  {
    private final Map<String, CutsceneEntity> entityMap = new ConcurrentHashMap<>();
    private final Map<Player, PlayerBoundMob> boundMobMap = new ConcurrentHashMap<>();

    private final CutsceneManager manager;
    private final EntityHider hider;
    public EntityManager(CutsceneManager manager) {
        this.manager = manager;
        hider = new EntityHider(manager.getPlugin(), EntityHider.Policy.BLACKLIST);
        ListenerManager listener = new ListenerManager(manager.getPlugin());
        listener.add(new Listener() {
            @EventHandler
            public void join(PlayerJoinEvent e) {
                Player p = e.getPlayer();
                put(p);
                manager.runTaskAsynchronously(() -> boundMobMap.values().forEach(c -> c.entityMap.values().forEach(ce -> hider.hideEntity(p,ce.getEntity()))));
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
        });
        Bukkit.getOnlinePlayers().forEach(this::put);
    }
    public CutsceneEntity get(String key) {
        return entityMap.get(key);
    }
    public CutsceneEntity get(Player player, String key) {
        return Optional.ofNullable(boundMobMap.get(player)).map(b -> b.entityMap.get(key)).orElse(null);
    }

    private void put(Player p) {
        boundMobMap.put(p,new PlayerBoundMob());
    }

    public CutsceneEntity createMob(Player player, String key, EntityType type, Location location) {
        CutsceneEntity entity = a(type,location);
        Entity entity1 = entity.getEntity();
        manager.runTaskAsynchronously(() -> Bukkit.getOnlinePlayers().forEach(p -> {
            if (p != player) hider.hideEntity(p,entity1);
        }));
        c(player,m -> m.entityMap.put(key,entity));
        return entity;
    }
    public void createMob(String key, EntityType type, Location location) {
        entityMap.put(key,a(type,location));
    }

    public void createMythicMob(Player player, String key, String type, Location location) {
        CutsceneEntity entity = b(type,location);
        if (entity != null) {
            Entity entity1 = entity.getEntity();
            manager.runTaskAsynchronously(() -> Bukkit.getOnlinePlayers().forEach(p -> {
                if (p != player) hider.hideEntity(p,entity1);
            }));
            c(player,m -> m.entityMap.put(key,entity));
        }
    }
    public void createMythicMob(String key, String type, Location location) {
        CutsceneEntity entity = b(type,location);
        if (entity != null) entityMap.put(key,entity);
    }

    private CutsceneEntity a(EntityType type, Location location) {
        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) chunk.load();
        return new CutsceneEntity((LivingEntity) location.getWorld().spawnEntity(location,type));
    }

    private CutsceneEntity b(String key, Location location) {
        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) chunk.load();
        try {
            MythicMob mob = MythicMobs.inst().getMobManager().getMythicMob(key);
            if (mob == null) {
                CutsceneMaker.warn("The Mob named \"" + key + "\" doesn't exist!");
                return null;
            }
            return new CutsceneEntity((LivingEntity) mob.spawn(BukkitAdapter.adapt(location),1.0).getEntity().getBukkitEntity());
        } catch (Throwable e) {
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
