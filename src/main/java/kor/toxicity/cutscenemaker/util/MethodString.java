package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.util.conditions.ConditionParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodString {

    private static final MethodString instance = new MethodString();
    public static MethodString getInstance() {
        return instance;
    }

    public MethodInterpreter parse(String s) {
        return new MethodInterpreter(s);
    }

    public static class MethodInterpreter {

        private final List<Function<LivingEntity,String>> print;
        private MethodInterpreter(String s) {
            print = new ArrayList<>();
            int loop = 0;
            for (String t : (s.contains("%") ? s.split("%") : new String[] {s})) {
                loop ++;
                if (Math.floorMod(loop,2) == 0) {
                    Function<LivingEntity,?> f = ConditionParser.LIVING_ENTITY.getAsFunc(t);
                    if (f != null) print.add(e -> f.apply(e).toString());
                } else print.add(q -> s);
            }
        }

        public String print(LivingEntity e) {
            StringBuilder t = new StringBuilder();
            print.stream().map(f -> f.apply(e)).forEach(t::append);
            return t.toString();
        }

    }
}
