package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.handlers.DelayedHandler;
import kor.toxicity.cutscenemaker.handlers.enums.EventClickType;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.function.Predicate;

public class HandlerBlockClick extends ActionHandler implements DelayedHandler {

    @DataField
    public String type;
    @DataField(aliases = {"loc","l"})
    public String location;
    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "c")
    public boolean cancel = false;
    @DataField(aliases = {"a", "act"})
    public String action;
    @DataField(aliases = "s")
    public boolean sneaking = false;

    private final Map<Player,Long> time = new WeakHashMap<>();
    private Predicate<PlayerInteractEvent> check = e -> {
        if (!e.hasBlock()) return false;
        Long d = time.put(e.getPlayer(), System.currentTimeMillis());
        return ((d == null ? - 200 : d) + 200 <= System.currentTimeMillis());
    };

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
        if (action != null) {
            try {
                List<Action> act = new ArrayList<>();
                Arrays.stream(TextUtil.split(action, "or")).map(s -> {
                    try {
                        return EventClickType.valueOf(s.toUpperCase());
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).forEach(e -> act.addAll(Arrays.asList(e.getAct())));
                if (act.size() > 0) build(e -> act.contains(e.getAction()));
            } catch (Exception ignored) {}
        }
        if (sneaking) build(e -> e.getPlayer().isSneaking());
        build(e -> CutsceneManager.onDelay(e.getPlayer()));
        type = null;
        location = null;
        world = null;
        action = null;
    }

    @EventHandler
    public void BlockBreak(PlayerInteractEvent e) {
        if (check.test(e)) {
            if (cancel) e.setCancelled(true);
            apply(e.getPlayer());
        }
    }

    @Override
    public Map<Player, Long> getTimeMap() {
        return time;
    }
}
