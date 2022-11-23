package kor.toxicity.cutscenemaker.util.functions;

import kor.toxicity.cutscenemaker.util.TextParser;
import kor.toxicity.cutscenemaker.util.conditions.ConditionParser;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodInterpreter {

    public static final String PERCENT = "%";
    private final Function<LivingEntity,String> apply;
    MethodInterpreter(String s) {
        List<Function<LivingEntity,String>> print = new ArrayList<>();
        int loop = 0;
        for (String t : TextParser.getInstance().split(s,PERCENT)) {
            loop ++;
            if (Math.floorMod(loop,2) == 0) {
                Function<LivingEntity,String> function = get(t);
                if (function != null) print.add(function);
            } else print.add(q -> t);
        }
        apply = (print.size() == 1) ? e -> print.get(0).apply(e) : e -> {
            StringBuilder t = new StringBuilder();
            for (Function<LivingEntity, String> f : print) {
                t.append(f.apply(e));
            }
            return t.toString();
        };
    }

    public String print(LivingEntity e) {
        return apply.apply(e);
    }

    private String printNumber(double d) {
        return (d == Math.floor(d)) ? Integer.toString((int) d) : String.format("%.2f", d);
    }

    private Function<LivingEntity,String> get(String t) {
        if (t.equals("")) return q -> PERCENT;
        else {
            Function<LivingEntity, ?> f = ConditionParser.LIVING_ENTITY.getAsFunc(t);
            if (f != null) return e -> {
                Object o = f.apply(e);
                if (o instanceof Number) return printNumber(((Number) o).doubleValue());
                else return o.toString();
            };
        }
        return null;
    }
}
