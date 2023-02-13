package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.handlers.DelayedHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class HandlerEntityClick extends ActionHandler implements DelayedHandler {

    @DataField(aliases = "t")
    public String type;
    @DataField(aliases = "n")
    public String name;
    @DataField(aliases = "c")
    public boolean cancel = false;

    private final Map<Player,Long> time = new WeakHashMap<>();
    private Predicate<PlayerInteractAtEntityEvent> check = p -> {
        Long d = time.put(p.getPlayer(), System.currentTimeMillis());
        return ((d == null ? - 200 : d) + 200 <= System.currentTimeMillis());
    };

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
            build(e -> TextUtil.getEntityName(e.getRightClicked()).equals(n));
        }
        build(e -> CutsceneManager.onDelay(e.getPlayer()));
        type = null;
        name = null;

    }
    private void build(Predicate<PlayerInteractAtEntityEvent> t) {
        if (check != null) check = check.and(t);
        else check = t;
    }

    @EventHandler
    public void interact(PlayerInteractAtEntityEvent e) {
        if (check == null || check.test(e)) {
            apply(e.getPlayer());
            e.setCancelled(cancel);
        }
    }

    @Override
    public Map<Player, Long> getTimeMap() {
        return time;
    }
}
