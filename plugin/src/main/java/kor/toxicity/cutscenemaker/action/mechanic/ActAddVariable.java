package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActAddVariable extends CutsceneAction {
    @DataField(aliases = "n", throwable = true)
    public String name;
    @DataField(aliases = "v", throwable = true)
    public double value;


    public ActAddVariable(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Vars v = manager.getVars((Player) entity,name);
            if (v != null) v.setVar(Double.toString(v.getAsNum(0D).doubleValue() + value));

        }
    }
}
