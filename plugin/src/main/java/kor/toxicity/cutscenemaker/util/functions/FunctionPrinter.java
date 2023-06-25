package kor.toxicity.cutscenemaker.util.functions;

import kor.toxicity.cutscenemaker.util.TextUtil;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@EqualsAndHashCode
public class FunctionPrinter implements Comparable<FunctionPrinter>, CharSequence {
    public static final String PERCENT = "%";
    @EqualsAndHashCode.Exclude
    private final Function<LivingEntity,String> apply;
    @EqualsAndHashCode.Exclude
    private final boolean anyMatch;
    private final String text;

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return text.subSequence(start,end);
    }

    @Override
    public String toString() {
        return text;
    }
    public FunctionPrinter(@NotNull String text) {
        this.text = Objects.requireNonNull(text);
        String[] split = TextUtil.split(text,PERCENT);
        if (split.length < 2 || text.chars().filter(c -> c == '%').sum() % 2 == 1) {
            anyMatch = false;
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
            anyMatch = true;
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
    public boolean anyMatch() {
        return anyMatch;
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

    @Override
    public int compareTo(@NotNull FunctionPrinter o) {
        return text.compareTo(o.text);
    }
}
