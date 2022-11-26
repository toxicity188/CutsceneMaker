package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.functions.MethodInterpreter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActDivVariable extends CutsceneAction {
    @DataField(aliases = "n", throwable = true)
    public MethodInterpreter name;
    @DataField(aliases = "v", throwable = true)
    public double value;

    private final CutsceneManager pl;

    public ActDivVariable(CutsceneManager pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player && value != 0D) {
            Vars v = pl.getVars((Player) entity,name.print(entity));
            if (v != null) v.setVar(Double.toString(v.getAsNum(0D).doubleValue() / value));

        }
    }
}
