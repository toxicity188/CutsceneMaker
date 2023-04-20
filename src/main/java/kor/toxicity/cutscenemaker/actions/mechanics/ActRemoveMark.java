package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import org.bukkit.entity.LivingEntity;

public class ActRemoveMark extends CutsceneAction {
    public ActRemoveMark(CutsceneManager pl) {
        super(pl);
    }

    @Override
    protected void apply(LivingEntity entity) {
        ActMark.LOCATION.remove(entity);
    }
}
