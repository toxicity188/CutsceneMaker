package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.function.Predicate;

public class HandlerBlockClick extends ActionHandler {

    @DataField
    public String type;
    @DataField(aliases = {"loc","l"})
    public String location;
    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "c")
    public boolean cancel = false;

    private Predicate<PlayerInteractEvent> check = PlayerInteractEvent::hasBlock;

    public HandlerBlockClick(ActionContainer container) {
        super(container);
    }

    private void build(Predicate<PlayerInteractEvent> t) {
        check = check.and(t);
    }

    @Override
    protected void initialize() {
        if (type != null) {
            try {
                Material m = Material.valueOf(type.toUpperCase());
                build(e -> e.getClickedBlock().getType() == m);
            } catch (Exception ignored) {}
        }
        if (location != null && location.contains(",")) {
            try {
                double[] d = Arrays.stream(location.split(",")).mapToDouble(Double::parseDouble).map(Math::floor).toArray();
                if (d.length >= 3) {
                    if (world != null && Bukkit.getWorld(world) != null) {
                        Location loc = new Location(Bukkit.getWorld(world), d[0], d[1], d[2]);
                        build(e -> e.getClickedBlock().getLocation().equals(loc));
                    } else {
                        build(e -> {
                            Location loc = e.getClickedBlock().getLocation();
                            return (loc.getX() == d[0] && loc.getY() == d[1] && loc.getZ() == d[2]);
                        });
                    }
                }
            } catch (Exception ignored) {}
        }
        type = null;
        location = null;
        world = null;
    }

    @EventHandler
    public void BlockBreak(PlayerInteractEvent e) {
        if (check.test(e)) {
            if (cancel) e.setCancelled(true);
            apply(e.getPlayer());
        }
    }
}
