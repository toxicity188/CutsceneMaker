package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActAddVariable extends CutsceneAction {
    @DataField(aliases = "n", throwable = true)
    public String name;
    @DataField(aliases = "v", throwable = true)
    public double value;

    private final CutsceneManager pl;

    public ActAddVariable(CutsceneManager pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Vars v = pl.getVars((Player) entity,name);
            if (v != null) v.setVar(Double.toString(v.getAsNum(0D).doubleValue() + value));

        }
    }
}
