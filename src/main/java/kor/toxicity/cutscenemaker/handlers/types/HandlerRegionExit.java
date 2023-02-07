package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.shaded.mewin.WGRegionEvents.events.RegionLeaveEvent;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;

import java.util.function.Predicate;

public class HandlerRegionExit extends ActionHandler {

    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "n",throwable = true)
    public String name;

    private Predicate<RegionLeaveEvent> check;

    public HandlerRegionExit(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        build(e -> e.getRegion().getId().equals(name));
        if (world != null) {
            World world1 = Bukkit.getWorld(world);
            if (world1 != null) build(e -> e.getPlayer().getWorld().equals(world1));
        }
        world = null;
    }

    private void build(Predicate<RegionLeaveEvent> check) {
        if (this.check == null) this.check = check;
        else this.check = this.check.and(check);
    }

    @EventHandler
    public void enter(RegionLeaveEvent e) {
        if (check == null || check.test(e)) apply(e.getPlayer());
    }
}
