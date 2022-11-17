package kor.toxicity.cutscenemaker.util.functions;

import kor.toxicity.cutscenemaker.util.TextParser;
import kor.toxicity.cutscenemaker.util.conditions.ConditionParser;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodInterpreter {

    public static final String PERCENT = "%";
    private final List<Function<LivingEntity,String>> print;
    MethodInterpreter(String s) {
        print = new ArrayList<>();
        int loop = 0;
        for (String t : TextParser.getInstance().split(s,PERCENT)) {
            loop ++;
            if (Math.floorMod(loop,2) == 0) {
                if (t.equals("")) print.add(q -> PERCENT);
                else {
                    Function<LivingEntity, ?> f = ConditionParser.LIVING_ENTITY.getAsFunc(t);
                    if (f != null) print.add(e -> {
                        Object o = f.apply(e);
                        if (o instanceof Number) return printNumber(((Number) o).doubleValue());
                        else return o.toString();
                    });
                }
            } else print.add(q -> t);
        }
    }

    public String print(LivingEntity e) {
        StringBuilder t = new StringBuilder();
        for (Function<LivingEntity, String> f : print) {
            t.append(f.apply(e));
        }
        return t.toString();
    }

    private String printNumber(double d) {
        return (d == Math.floor(d)) ? Integer.toString((int) d) : String.format("%.2f", d);
    }
}
