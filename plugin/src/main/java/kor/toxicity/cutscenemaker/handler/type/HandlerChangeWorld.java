package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.function.Predicate;

public class HandlerChangeWorld extends ActionHandler {

    @DataField
    public String from;
    @DataField
    public String to;

    private Predicate<PlayerChangedWorldEvent> check;

    public HandlerChangeWorld(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        if (to != null) {
            World world = Bukkit.getWorld(to);
            if (world != null) build(e -> e.getPlayer().getWorld().equals(world));
        }
        if (from != null) {
            World world = Bukkit.getWorld(to);
            if (world != null) build(e -> e.getFrom().equals(world));
        }
    }
    public void build(Predicate<PlayerChangedWorldEvent> t) {
        if (check == null) check = t;
        else check = check.and(t);
    }
    @EventHandler
    public void change(PlayerChangedWorldEvent e) {
        if (check == null || check.test(e)) apply(e.getPlayer());
    }
}
