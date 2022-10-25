package kor.toxicity.cutscenemaker.util;

import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MethodString {

    private static final MethodString instance = new MethodString();
    private MethodString() {
    }
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

                } else print.add(q -> s);
            }
        }

    }
}
