package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class ActWarp extends CutsceneAction {

    @DataField(aliases = {"loc","l"},throwable = true)
    public String location;

    private Location loc;
    private final CutsceneManager manager;
    public ActWarp(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        loc = manager.getLocations().getValue(location);
        if (loc == null) CutsceneMaker.warn("unable to find a location named \"" + location + "\"");
    }

    @Override
    protected void apply(LivingEntity entity) {
        entity.teleport(loc);
    }
}
