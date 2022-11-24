package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.function.Function;
import java.util.function.Predicate;

public class HandlerWalk extends ActionHandler {

    @DataField(aliases = "loc")
    public String location;
    @DataField
    public int range = 1;
    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "b")
    public String block;

    private Predicate<PlayerMoveEvent> check;

    public HandlerWalk(ActionContainer container) {
        super(container);
    }

    private void build(Predicate<PlayerMoveEvent> t) {
        if (check != null) check = check.and(t);
        else check = t;
    }

    @Override
    protected void initialize() {
        Function<LivingEntity, Location> function = TextParser.getInstance().getBlockLocation(world,location);
        if (function != null) build(e -> {
            Player p = e.getPlayer();
            return function.apply(p).distance(p.getLocation()) < range;
        });
        if (block != null) {
            try {
                Material material = Material.valueOf(block);
                build(e -> e.getPlayer().getLocation().add(0,-1,0).getBlock().getType() == material);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void walk(PlayerMoveEvent e) {
        if (check.test(e)) apply(e.getPlayer());
    }
}
