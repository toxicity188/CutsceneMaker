package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;

public class ActAction extends CutsceneAction {
    @DataField(aliases = "n",throwable = true)
    public FunctionPrinter name;

    public ActAction(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        ActionData.start(name.print(entity),entity);
    }
}
