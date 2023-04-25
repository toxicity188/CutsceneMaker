package kor.toxicity.cutscenemaker.action;


import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.entity.CutsceneEntity;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class CutsceneAction {

    @DataField(aliases = "d")
    public int delay = 0;
    @DataField
    public String target;
    @DataField(aliases = "if")
    public String If;

    private Consumer<LivingEntity> invoke;

    protected final CutsceneManager manager;
    public CutsceneAction(CutsceneManager pl) {
        manager = pl;
    }
    protected void lateCheck(Runnable runnable) {
        manager.addLateCheck(runnable);
    }

    public final void call(LivingEntity entity) {
        invoke.accept(entity);
    }
    protected abstract void apply(LivingEntity entity);
    public void initialize() {
        if (delay < 0) delay = 0;
        if (target != null) {
            invoke = e -> {
                if (e instanceof Player) Optional.ofNullable(manager.getEntityManager().get((Player) e,target)).map(CutsceneEntity::getEntity).ifPresent(this::apply);
            };
        } else {
            invoke = this::apply;
        }
        if (If != null) {
            String[] state = TextUtil.split(If.replace(':','=')," ");
            if (state.length >= 3) {
                Predicate<LivingEntity> predicate = ConditionBuilder.LIVING_ENTITY.find(state);
                if (predicate != null) invoke = e -> {
                    if (predicate.test(e)) invoke.accept(e);
                };
                else CutsceneMaker.warn("unable to load if statement \"" + If + "\"");
            } else CutsceneMaker.warn("unable to load if statement \"" + If + "\"");
            If = null;
        }
    }

}
