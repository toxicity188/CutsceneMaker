package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.quests.Navigator;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActNavigate extends CutsceneAction {

    @DataField(aliases = {"loc","l"},throwable = true)
    public String location;
    @DataField(aliases = "n",throwable = true)
    public String name;

    private Location loc;

    public ActNavigate(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        loc = manager.getLocations().getValue(location);
        if (loc == null) CutsceneMaker.warn("The Location named \"" + location + " doesn't exist!");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (loc != null && entity instanceof Player) Navigator.startNavigate((Player) entity, name, loc);
    }
}
