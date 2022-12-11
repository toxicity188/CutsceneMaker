package kor.toxicity.cutscenemaker.util.functions;

import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.conditions.ConditionBuilder;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodInterpreter {

    public static final String PERCENT = "%";
    private final Function<LivingEntity,String> apply;
    public final boolean ANY_MATCH;

    public MethodInterpreter(String s) {
        List<Function<LivingEntity,String>> print = new ArrayList<>();
        int loop = 0;
        for (String t : TextUtil.getInstance().split(s,PERCENT)) {
            String colored = TextUtil.getInstance().colored(t);
            loop ++;
            if (Math.floorMod(loop,2) == 0) {
                Function<LivingEntity,String> function = get(colored);
                if (function != null) print.add(function);
            } else print.add(q -> colored);
        }
        if (print.size() == 1) {
            ANY_MATCH = false;
            apply = print.get(0);
        } else {
            ANY_MATCH = true;
            apply = e -> {
                StringBuilder t = new StringBuilder();
                for (Function<LivingEntity, String> f : print) {
                    t.append(f.apply(e));
                }
                return t.toString();
            };
        }
    }

    public String print(LivingEntity e) {
        return apply.apply(e);
    }

    private String printNumber(double d) {
        return (d == Math.floor(d)) ? TextUtil.getInstance().applyComma(d) : String.format("%.2f", d);
    }

    private Function<LivingEntity,String> get(String t) {
        if (t.equals("")) return q -> PERCENT;
        else {
            Function<LivingEntity, ?> f = ConditionBuilder.LIVING_ENTITY.getAsFunc(t);
            if (f != null) return e -> {
                Object o = f.apply(e);
                if (o instanceof Number) return printNumber(((Number) o).doubleValue());
                else return o.toString();
            };
        }
        return null;
    }
}
