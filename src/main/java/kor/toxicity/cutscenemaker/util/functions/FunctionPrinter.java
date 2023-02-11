package kor.toxicity.cutscenemaker.util.functions;

import kor.toxicity.cutscenemaker.util.TextUtil;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FunctionPrinter {

    public static final String PERCENT = "%";
    private final Function<LivingEntity,String> apply;
    public final boolean ANY_MATCH;

    public FunctionPrinter(String s) {
        String[] split = TextUtil.split(s,PERCENT);
        if (split.length < 2 || s.chars().filter(c -> c == '%').sum() % 2 == 1) {
            ANY_MATCH = false;
            String colored = TextUtil.colored(split[0]);
            apply = t -> colored;
        } else {
            List<Function<LivingEntity,String>> print = new ArrayList<>(split.length);
            int loop = 0;
            for (String t : split) {
                String colored = TextUtil.colored(t);
                loop ++;
                if (loop % 2 == 0) {
                    Function<LivingEntity,String> function = get(colored);
                    if (function != null) print.add(function);
                } else print.add(q -> colored);
            }
            ANY_MATCH = true;
            StringBuilder t = new StringBuilder();
            apply = e -> {
                t.setLength(0);
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
        return (d == Math.floor(d)) ? TextUtil.applyComma(d) : String.format("%.2f", d);
    }

    private Function<LivingEntity,String> get(String t) {
        if (t.equals("")) return q -> PERCENT;
        else return convert(t);
    }
    private Function<LivingEntity,String> convert(String t) {
        Function<LivingEntity, ?> f = ConditionBuilder.LIVING_ENTITY.getAsFunc(t);
        if (f != null) {
            return e -> {
                Object o = f.apply(e);
                if (o instanceof Number) return printNumber(((Number) o).doubleValue());
                else return o.toString();
            };
        } else return null;
    }
}
