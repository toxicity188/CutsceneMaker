package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.events.ActionStartEvent;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ActSlate extends CutsceneAction {

    private static final Set<Player> toggle = new HashSet<>();
    private static final List<Consumer<Player>> tasksOn = new ArrayList<>();
    private static final List<Consumer<Player>> tasksOff = new ArrayList<>();
    public static void addSlateOnTask(Consumer<Player> task) {
        tasksOn.add(task);
    }
    public static void addSlateOffTask(Consumer<Player> task) {
        tasksOff.add(task);
    }

    private static ListenerManager manager;

    public ActSlate(CutsceneManager pl) {
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

    @DataField(aliases = "c")
    public boolean change = true;
    @DataField(aliases = "g")
    public boolean grounding = true;

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (toggle.contains(player)) off(player);
            else on(player,change,grounding);
        }
    }
    private static void on(Player player, boolean change, boolean grounding) {
        toggle.add(player);
        if (change) player.setGameMode(GameMode.SPECTATOR);
        else {
            if (grounding) {
                Location loc = player.getLocation();
                int y = (int) Math.floor(loc.getY());
                while (loc.getY() > -48 && loc.getBlock().getType() != Material.AIR) {
                    loc.setY(y - 1);
                }
                loc.setY(loc.getY() + 1);
                player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
        for (Consumer<Player> p : tasksOn) p.accept(player);

        if (!ActMark.LOCATION.containsKey(player)) ActMark.LOCATION.put(player,player.getLocation());
    }
    private static void off(Player player) {
        toggle.remove(player);
        player.setGameMode(CutsceneConfig.getInstance().getDefaultGameMode());
        for (Consumer<Player> p : tasksOff) p.accept(player);

        Location back = ActMark.LOCATION.get(player);
        if (back != null) player.teleport(back, PlayerTeleportEvent.TeleportCause.PLUGIN);
        ActMark.LOCATION.remove(player);
    }
}
