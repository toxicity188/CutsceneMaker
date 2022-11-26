package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.events.ActionCancelEvent;
import kor.toxicity.cutscenemaker.events.ActionStartEvent;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class ActReady extends CutsceneAction {

    private static final Set<Player> toggle = new HashSet<>();

    private static ListenerManager manager;

    public ActReady(CutsceneManager pl) {
        super(pl);
        if (manager == null) manager = pl.register(new Listener() {
            @EventHandler
            public void move(PlayerMoveEvent e) {
                if (toggle.contains(e.getPlayer())) e.setCancelled(true);
            }
            @EventHandler
            public void quit(PlayerQuitEvent e) {
                if (toggle.contains(e.getPlayer())) off(e.getPlayer());
            }
            @EventHandler
            public void start(ActionStartEvent e) {
                if (e.getEntity() instanceof Player && toggle.contains((Player) e.getEntity())) e.setCancelled(true);
            }
            @EventHandler(priority = EventPriority.HIGHEST)
            public void interact(PlayerInteractEvent e) {
                if (toggle.contains(e.getPlayer())) e.setCancelled(true);
            }
            @EventHandler(priority = EventPriority.HIGHEST)
            public void command(PlayerCommandPreprocessEvent e) {
                if (toggle.contains(e.getPlayer())) e.setCancelled(true);
            }
            @EventHandler
            public void attack(EntityDamageByEntityEvent e) {
                if (e.getDamager() instanceof Player && toggle.contains((Player) e.getDamager())) e.setCancelled(true);
            }
            @EventHandler
            public void teleport(PlayerTeleportEvent e) {
                if (toggle.contains(e.getPlayer())) {
                    switch (e.getCause()) {
                        default:
                            e.setCancelled(true);
                        case PLUGIN:
                        case UNKNOWN:
                    }
                }
            }
        });
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (toggle.contains(player)) off(player);
            else on(player);
        }
    }
    private static void on(Player player) {
        toggle.add(player);
        player.setGameMode(GameMode.SPECTATOR);

        if (!ActMark.LOCATION.containsKey(player)) ActMark.LOCATION.put(player,player.getLocation());
    }
    private static void off(Player player) {
        toggle.remove(player);
        player.setGameMode(GameMode.SURVIVAL);

        ActMark.LOCATION.remove(player);
    }
}
