package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.function.Predicate;

public class HandlerKill extends ActionHandler {

    @DataField
    public String type;
    @DataField(aliases = "n")
    public String name;
    @DataField(aliases = "w")
    public String world;

    private Predicate<EntityDeathEvent> check = e -> e.getEntity().getKiller() != null;

    private void build(Predicate<EntityDeathEvent> t) {
        check = check.and(t);
    }

    public HandlerKill(ActionContainer container) {
        super(container);
    }


    @Override
    protected void initialize() {
        if (type != null) {
            try {
                EntityType t = EntityType.valueOf(type.toUpperCase());
                build(e -> e.getEntityType() == t);
            } catch (Exception ignored) {}
        }
        if (name != null) build(e -> TextUtil.getEntityName(e.getEntity()).equals(name));
        if (world != null) {
            World w = Bukkit.getWorld(world);
            if (w != null) build(e -> e.getEntity().getWorld().equals(w));
        }
        type = null;
        world = null;
    }

    @EventHandler
    public void kill(EntityDeathEvent e) {
        if (check.test(e)) apply(e.getEntity().getKiller());
    }
}
