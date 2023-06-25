package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.shaded.com.mewin.WGRegionEvents.events.RegionEnterEvent;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;

import java.util.function.Predicate;

public class HandlerRegionEnter extends ActionHandler {

    @DataField(aliases = "w")
    public String world;
    @DataField(aliases = "n",throwable = true)
    public String name;

    private Predicate<RegionEnterEvent> check;

    public HandlerRegionEnter(ActionContainer container) {
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

    private void build(Predicate<RegionEnterEvent> check) {
        if (this.check == null) this.check = check;
        else this.check = this.check.and(check);
    }

    @EventHandler
    public void enter(RegionEnterEvent e) {
        if (check == null || check.test(e)) apply(e.getPlayer());
    }
}
