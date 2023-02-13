package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class ActWarp extends CutsceneAction {

    @DataField(aliases = {"loc","l"},throwable = true)
    public String location;
    @DataField(aliases = "s")
    public String studio;

    private Function<LivingEntity,Location> function;
    private final CutsceneManager manager;
    public ActWarp(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        Location loc = manager.getLocations().getValue(location);
        if (loc == null) CutsceneMaker.warn("unable to find a location named \"" + location + "\"");
        else function = (studio != null) ? p -> {
            if (p instanceof Player) {
                return LocationStudio.getPlayerRecord((Player) p).map(r -> r.getLocation(studio)).orElse(loc);
            } return loc;
        } : p -> loc;
    }

    @Override
    protected void apply(LivingEntity entity) {
        entity.teleport(function.apply(entity));
    }
}
