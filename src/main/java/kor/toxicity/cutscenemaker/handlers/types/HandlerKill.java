package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.function.Predicate;

public class HandlerKill extends ActionHandler {

    @DataField
    public String type;
    @DataField
    public String name;

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
                EntityType t = EntityType.valueOf(type);
                build(e -> e.getEntityType() == t);
            } catch (Exception ignored) {}
        }
        if (name != null) build(e -> TextParser.getInstance().getEntityName(e.getEntity()).equals(name));
        type = null;
    }

    @EventHandler
    public void kill(EntityDeathEvent e) {
        if (check.test(e)) apply(e.getEntity().getKiller());
    }
}
