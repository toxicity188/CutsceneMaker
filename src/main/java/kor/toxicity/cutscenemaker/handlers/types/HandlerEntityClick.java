package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.util.function.Predicate;

public class HandlerEntityClick extends ActionHandler {

    @DataField(aliases = "t")
    public String type;
    @DataField(aliases = "n")
    public String name;

    private Predicate<PlayerInteractAtEntityEvent> check;

    public HandlerEntityClick(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (type != null) {
            try {
                EntityType t = EntityType.valueOf(type.toUpperCase());
                build(e -> e.getRightClicked().getType() == t);
            } catch (Exception ignored) {}
        }
        if (name != null) {
            String n = name.replaceAll("_"," ");
            build(e -> TextParser.getInstance().getEntityName(e.getRightClicked()).equals(n));
        }
        type = null;
        name = null;

    }
    private void build(Predicate<PlayerInteractAtEntityEvent> t) {
        if (check != null) check = check.and(t);
        else check = t;
    }

    @EventHandler
    public void interact(PlayerInteractAtEntityEvent e) {
        if (check.test(e)) apply(e.getPlayer());
    }
}
