package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.RegionUtil;
import kor.toxicity.cutscenemaker.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class HandlerWalk extends ActionHandler {

    @DataField(aliases = "loc")
    public String location;
    @DataField
    public double range = 0.5;
    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "b")
    public String block;
    @DataField(aliases = "r")
    public String region;

    private Predicate<PlayerMoveEvent> check;
    private final Set<Player> toggle = new HashSet<>();

    public HandlerWalk(ActionContainer container) {
        super(container);
    }

    private void build(Predicate<PlayerMoveEvent> t) {
        if (check != null) check = check.and(t);
        else check = t;
    }

    @Override
    protected void initialize() {
        if (location != null) {
            Function<LivingEntity, Location> function = TextUtil.getBlockLocation(world, location);
            if (function != null) build(e -> {
                Player p = e.getPlayer();
                Location loc = p.getLocation();
                Location loc2 = function.apply(p);
                return loc.getWorld().equals(loc2.getWorld()) && loc2.distance(loc) < range;
            });
        } else if (world != null) {
            World world1 = Bukkit.getWorld(world);
            if (world1 != null) build(e -> e.getPlayer().getWorld().equals(world1));
        }
        if (block != null) {
            try {
                Material material = Material.valueOf(block.toUpperCase());
                build(e -> e.getPlayer().getLocation().add(0,-1,0).getBlock().getType() == material);
            } catch (Exception ignored) {}
        }
        if (region != null) {
            build(e -> RegionUtil.inRegion(e.getPlayer(),region));
        }
        location = null;
        world = null;
        block = null;
    }

    @EventHandler
    public void walk(PlayerMoveEvent e) {
        if (check == null || check.test(e)) {
            if (!toggle.contains(e.getPlayer())) {
                toggle.add(e.getPlayer());
                apply(e.getPlayer());
            }
        } else toggle.remove(e.getPlayer());
    }
}
