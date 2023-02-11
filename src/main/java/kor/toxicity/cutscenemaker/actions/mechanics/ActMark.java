package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActMark extends CutsceneAction {

    @DataField(aliases = "o")
    public boolean override = true;
    @DataField(aliases = {"l","loc"})
    public String location;
    @DataField(aliases = "w")
    public String world;

    static final Map<Entity, Location> LOCATION = new HashMap<>();
    private static ListenerManager manager;

    private Function<Entity,Location> function = Entity::getLocation;

    public ActMark(CutsceneManager pl) {
        super(pl);
        if (manager == null) manager = pl.register(new Listener() {
            private final ActRecall recall = new ActRecall(pl);

            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                recall.apply(e.getPlayer());
            }
        });
    }

    @Override
    public void initialize() {
        super.initialize();
        if (location != null) {
            String[] l = TextUtil.split(location,",");
            if (l.length >= 3) {
                double[] d = Arrays.stream(l).mapToDouble(s -> {
                    try {
                        return Double.parseDouble(s);
                    } catch (Exception e) {
                        return 0D;
                    }
                }).toArray();
                World wd = (world != null) ? Bukkit.getWorld(world) : null;
                function = (wd != null) ? e -> new Location(wd,d[0],d[1],d[2]) : e -> new Location(e.getWorld(),d[0],d[1],d[2]);
            }
        }
        location = null;
        world = null;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (override || !LOCATION.containsKey(entity)) LOCATION.put(entity,function.apply(entity));
    }
}
