package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ActIf extends CutsceneAction {

    @DataField(aliases = "var", throwable = true)
    public FunctionPrinter variable;
    @DataField(aliases = {"cond","parameter","para","c","p"},throwable = true)
    public String condition;

    @DataField(throwable = true)
    public FunctionPrinter then;
    @DataField(aliases = "else",throwable = true)
    public FunctionPrinter not;

    private Consumer<Player> apply;

    public ActIf(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        String[] str = TextUtil.split(condition, " ");
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
