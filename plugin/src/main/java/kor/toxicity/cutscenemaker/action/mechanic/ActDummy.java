package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import org.bukkit.entity.LivingEntity;

public class ActDummy extends CutsceneAction {
    public ActDummy(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        /*
          This is a dummy effect.
          this also can input "delay" parameter.
         */
    }
}

