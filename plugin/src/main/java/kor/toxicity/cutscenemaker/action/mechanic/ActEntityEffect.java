package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.action.RepeatableAction;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;

public class ActEntityEffect extends RepeatableAction {

    @DataField(throwable = true)
    public String type;

    private EntityEffect effect;

    public ActEntityEffect(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            effect = EntityEffect.valueOf(type.toUpperCase());
        } catch (Exception e) {
            effect = EntityEffect.HURT;
        }
    }

    @Override
    protected void initialize(LivingEntity entity) {

    }

    @Override
    protected void update(LivingEntity entity) {
        entity.playEffect(effect);
    }

    @Override
    protected void end(LivingEntity end) {

    }
}
