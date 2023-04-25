package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActDeleteVariable extends CutsceneAction {

    @DataField(aliases = "n", throwable = true)
    public String name;

    public ActDeleteVariable(CutsceneManager pl) {
        super(pl);
    }


    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            manager.getVars((Player) entity).remove(name);

        }
    }
}
