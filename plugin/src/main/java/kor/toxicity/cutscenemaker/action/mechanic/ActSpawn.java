package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.entity.EntityManager;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Function;

public class ActSpawn extends CutsceneAction {

    @DataField(aliases = "k",throwable = true)
    public String key;
    @DataField(aliases = "b")
    public boolean bound = true;
    @DataField(throwable = true)
    public String type;

    @DataField(aliases = "loc", throwable = true)
    public String location;
    @DataField(aliases = "mm")
    public boolean mythicMobs = false;

    private Consumer<Player> consumer;
    private final CutsceneManager manager;

    public ActSpawn(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        Location loc = manager.getLocations().getValue(location);
        EntityManager manager1 = manager.getEntityManager();
        if (loc == null) {
            CutsceneMaker.warn("location not found.");
        } else {
            Function<Player,Location> supplier = p -> LocationStudio.getPlayerRecord(p).map(r -> r.getLocation(location)).orElse(loc);
            if (mythicMobs) {
                consumer = (bound) ? p -> manager1.createMythicMob(p, key, type, supplier.apply(p)) : p -> manager1.createMythicMob(key, type, supplier.apply(p));
            } else {
                try {
                    EntityType t = EntityType.valueOf(type.toUpperCase());
                    consumer = (bound) ? p -> manager1.createMob(p, key, t, supplier.apply(p)) : p -> manager1.createMob(key, t, supplier.apply(p));
                } catch (Exception e) {
                    CutsceneMaker.warn("entity type \"" + type + "\" is not exists.");
                }
            }
        }
    }

    @Override
    public void apply(LivingEntity entity) {
        if (consumer != null && entity instanceof Player) consumer.accept((Player) entity);
    }
}
