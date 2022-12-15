package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActDeleteVariable extends CutsceneAction {

    @DataField(aliases = "n", throwable = true)
    public String name;

    public ActDeleteVariable(CutsceneManager pl) {
        super(pl);
        this.pl = pl;
    }

    private final CutsceneManager pl;

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            pl.getVars((Player) entity).remove(name);

        }
    }
}
