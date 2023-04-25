package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActDivVariable extends CutsceneAction {
    @DataField(aliases = "n", throwable = true)
    public FunctionPrinter name;
    @DataField(aliases = "v", throwable = true)
    public double value;


    public ActDivVariable(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player && value != 0D) {
            Vars v = manager.getVars((Player) entity,name.print(entity));
            if (v != null) v.setVar(Double.toString(v.getAsNum(0D).doubleValue() / value));

        }
    }
}
