package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.conditions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.MethodInterpreter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ActIf extends CutsceneAction {

    @DataField(aliases = "var", throwable = true)
    public MethodInterpreter variable;
    @DataField(aliases = {"cond","parameter","para","c","p"},throwable = true)
    public String condition;

    @DataField(throwable = true)
    public MethodInterpreter then;
    @DataField(aliases = "else",throwable = true)
    public MethodInterpreter not;

    private Consumer<Player> apply;
    private final CutsceneManager manager;

    public ActIf(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        String[] str = TextUtil.getInstance().split(condition, " ");
        if (str.length < 3) {
            a();
        } else {
            Predicate<LivingEntity> predicate = ConditionBuilder.LIVING_ENTITY.find(str);
            if (predicate == null) {
                a();
            } else {
                apply = e -> {
                    Optional<Vars> optional = Optional.ofNullable(manager.getVars(e,variable.print(e)));
                    if (predicate.test(e)) optional.ifPresent(v -> v.setVar(then.print(e)));
                    else optional.ifPresent(v -> v.setVar(not.print(e)));
                };
            }
        }
    }
    private void a() {
        CutsceneMaker.warn("unable to read condition \"" + condition + "\"");
        apply = e -> {};
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) apply.accept((Player) entity);
    }
}
