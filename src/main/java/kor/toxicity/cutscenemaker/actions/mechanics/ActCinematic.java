package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import kor.toxicity.cutscenemaker.util.DataContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ActCinematic extends RepeatableAction {

    @DataField(aliases = {"f","s"})
    public String from;
    @DataField(aliases = "e")
    public String to;

    @DataField(aliases = "n")
    public String node;

    private final Map<LivingEntity,Integer> loops = new WeakHashMap<>();;
    private final CutsceneManager manager;
    private Location first;
    private Location last;

    private Location[] locations;

    private Consumer<LivingEntity> init;
    private Consumer<LivingEntity> update;
    private Consumer<LivingEntity> stop;

    public ActCinematic(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        DataContainer<Location> loc = manager.getLocations();
        if (node != null) {
            if (!loc.containsNodeKey(node)) {
                CutsceneMaker.warn("unable to find location node. (" + node + ")");
                return;
            } else {
                List<Location> collect = new ArrayList<>(loc.getValues(node));
                if (collect.size() < 2) {
                    CutsceneMaker.warn("a size of location node must be at least 2. (" + node + ")");
                    return;
                }
                List<Location> locationList = new ArrayList<>();
                int size = collect.size() - 1;

                ticks = (int) Math.ceil(((double) ticks / (double) size));
                int t = ticks;
                for (int i = 0; i < size; i++) {
                    first = collect.get(i);
                    last = collect.get(i + 1);
                    if (!first.getWorld().equals(last.getWorld())) {
                        CutsceneMaker.warn("world mismatched. (location number " + i + ")");
                        return;
                    }
                    locationList.addAll(Arrays.asList(b()));
                }
                ticks = locationList.size();
                stop = loops::remove;
                init = entity -> loops.put(entity, 0);
                update = entity -> {
                    int i = loops.get(entity);
                    Location q = locationList.get(i);
                    if (i % t == 0 && !q.getChunk().isLoaded()) q.getChunk().load();
                    entity.teleport(q, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    loops.put(entity, i + 1);
                };
            }
            return;
        }


        if (loc.containsKey(from) && loc.containsKey(to)) {

            DataContainer<Location> a = manager.getLocations();
            first = a.getValue(from);
            last = a.getValue(to);
            if (!first.getWorld().equals(last.getWorld())) {
                CutsceneMaker.warn("world mismatched. (" + from + ", " + to + ")");
                return;
            }

            locations = b();
            stop = loops::remove;
            update = entity -> {
                if (loops.containsKey(entity)) {
                    int i = loops.get(entity);
                    entity.teleport(locations[i], PlayerTeleportEvent.TeleportCause.PLUGIN);
                    loops.put(entity,i + 1);
                }
            };
            init = entity -> {
                if (first != null && last != null) {
                    loops.put(entity,0);
                    if (!first.getChunk().isLoaded()) first.getChunk().load();
                    if (!last.getChunk().isLoaded()) last.getChunk().load();
                }
            };
        } else CutsceneMaker.warn("unable to find location. (" + from + ", " + to + ")");
    }
    private float a(float f) {
        return ((f < 0) ? 1 : -1) * (360 - Math.abs(f));
    }
    private Location[] b() {

        int loop = ticks * interval;

        double x = (last.getX() - first.getX())/loop;
        double y = (last.getY() - first.getY())/loop;
        double z = (last.getZ() - first.getZ())/loop;
        float pitch = (last.getPitch() - first.getPitch())/loop;

        float yawPositive = last.getYaw() - first.getYaw();
        float yawNegative = a(yawPositive);
        float yaw = ((Math.abs(yawNegative) > Math.abs(yawPositive)) ? yawPositive : yawNegative) / loop;

        return IntStream.range(0,ticks).mapToObj(i -> {
            float v2 = (float) i;

            double x1 = x * (double) i;
            double y1 = y * (double) i;
            double z1 = z * (double) i;
            float pitch1 = pitch * v2;
            float yaw1 = yaw * v2;

            Location ret = new Location(first.getWorld(),first.getX() + x1, first.getY() + y1, first.getZ() + z1);
            ret.setPitch(first.getPitch() + pitch1);
            ret.setYaw(first.getYaw() + yaw1);
            return ret;
        }).toArray(Location[]::new);
    }

    @Override
    protected void initialize(LivingEntity entity) {
        if (init != null) init.accept(entity);
    }

    @Override
    protected void update(LivingEntity entity) {
        if (update != null) update.accept(entity);
    }

    @Override
    protected void end(LivingEntity end) {
        if (stop != null) stop.accept(end);
    }
}

