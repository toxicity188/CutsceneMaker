package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActStudio extends CutsceneAction {

    @DataField(aliases = "n", throwable = true)
    public String name;
    @DataField(aliases = "loc", throwable = true)
    public String location;

    @DataField(aliases = "s",throwable = true)
    public FunctionPrinter success;
    @DataField(aliases = "f",throwable = true)
    public FunctionPrinter fail;


    public ActStudio(CutsceneManager pl) {
        super(pl);
    }


    private LocationStudio studio;
    @Override
    public void initialize() {
        super.initialize();
        studio = manager.getStudioMap().get(name);
        if (studio == null) CutsceneMaker.warn("The Studio named \"" + name + "\" doesn't exist!");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (studio != null && entity instanceof Player) {
            if (!studio.join((Player) entity,location)) ActionData.start(fail.print(entity),entity);
            else ActionData.start(success.print(entity),entity);
        }
    }
}
