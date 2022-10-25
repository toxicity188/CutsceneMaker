package kor.toxicity.cutscenemaker.actions;


import kor.toxicity.cutscenemaker.CutsceneManager;
import org.bukkit.entity.LivingEntity;

public abstract class CutsceneAction {

    @DataField(aliases = "d")
    public int delay = 0;

    public CutsceneAction(CutsceneManager pl) {

    }

    public abstract void apply(LivingEntity entity);
    public void initialize() {}

}
