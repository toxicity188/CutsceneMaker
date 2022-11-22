package kor.toxicity.cutscenemaker.actions;


import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.entities.CutsceneEntity;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class CutsceneAction {

    @DataField(aliases = "d")
    public int delay = 0;
    @DataField
    public String target;

    private Consumer<LivingEntity> invoke;

    public CutsceneAction(CutsceneManager pl) {
    }

    public void call(LivingEntity entity) {
        invoke.accept(entity);
    }
    protected abstract void apply(LivingEntity entity);
    public void initialize() {
        if (target != null) {
            invoke = e -> {
                if (e instanceof Player) Optional.of(EntityManager.getInstance().get((Player) e,target)).map(CutsceneEntity::getEntity).ifPresent(this::apply);
            };
        } else {
            invoke = this::apply;
        }
    }

}
