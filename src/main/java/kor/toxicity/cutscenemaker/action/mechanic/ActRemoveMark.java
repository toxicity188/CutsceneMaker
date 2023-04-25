package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
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
