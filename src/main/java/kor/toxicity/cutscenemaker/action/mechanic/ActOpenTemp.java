package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActOpenTemp extends CutsceneAction {
    public ActOpenTemp(CutsceneManager pl) {
        super(pl);
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) manager.openTempStorage((Player) entity);
    }
}
