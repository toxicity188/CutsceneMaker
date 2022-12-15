package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActSetVariable extends CutsceneAction {

    @DataField(aliases = "n", throwable = true)
    public FunctionPrinter name;
    @DataField(aliases = "v", throwable = true)
    public FunctionPrinter value;

    private final CutsceneManager pl;

    public ActSetVariable(CutsceneManager pl) {
        super(pl);
        this.pl = pl;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Vars v = pl.getVars((Player) entity,name.print(entity));
            if (v != null) v.setVar(value.print(entity));

        }
    }
}
