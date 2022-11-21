package kor.toxicity.cutscenemaker.util.conditions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.JsonMethod;
import kor.toxicity.cutscenemaker.util.MoneyUtil;
import kor.toxicity.cutscenemaker.util.RegionUtil;
import kor.toxicity.cutscenemaker.util.TextParser;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class ConditionParser<T> {

    private static final JsonParser parser = new JsonParser();
    public static final ConditionParser<LivingEntity> LIVING_ENTITY = new ConditionParser<>();


    private final Set<ConditionContainer<?>> types = new LinkedHashSet<>();

    public final ConditionContainer<Number> NUMBER = new ConditionContainer<>();
    public final ConditionContainer<Boolean> BOOL = new ConditionContainer<>();
    public final ConditionContainer<String> STRING = new ConditionContainer<>();

    static {
        LIVING_ENTITY.STRING.addFunction("tool",(p,j) -> {
            if (p instanceof Player) {
                ItemStack i = ((Player) p).getInventory().getItemInMainHand();
                if (i != null && i.getItemMeta().getDisplayName() != null) return i.getItemMeta().getDisplayName();
            }
            return "<none>";
        });
        LIVING_ENTITY.STRING.addFunction("location",(e,j) -> TextParser.getInstance().toSimpleLoc(e.getLocation()));
        LIVING_ENTITY.NUMBER.addFunction("money",(e,j) -> {
            if (e instanceof Player) {
                return MoneyUtil.getInstance().getMoney((Player) e);
            }
            return 0;
        });
        LIVING_ENTITY.NUMBER.addFunction("healthpercentage",(e,j) -> e.getHealth()/e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        LIVING_ENTITY.NUMBER.addFunction("health",(e,j) -> e.getHealth());
        LIVING_ENTITY.NUMBER.addFunction("random",(e,j) -> {
            try {
                double d = (j.size() > 0) ? j.get(0).getAsDouble() : 0D;
                double r = (j.size() > 1) ? j.get(1).getAsDouble() : 1D;
                return ThreadLocalRandom.current().nextDouble(d,r);
            } catch (Exception t) {
                return 0;
            }
        });
        LIVING_ENTITY.NUMBER.addFunction("randomInt",(e,j) -> {
            try {
                int d = (j.size() > 0) ? j.get(0).getAsInt() : 0;
                int r = (j.size() > 1) ? j.get(1).getAsInt() : 1;
                return ThreadLocalRandom.current().nextInt(d,r);
            } catch (Exception t) {
                return 0;
            }
        });
        LIVING_ENTITY.STRING.addFunction("name",(e,j) -> {
            if (j.size() > 0) return Bukkit.getPlayer(j.get(0).getAsString()).getName();
            return e.getName();
        });
        LIVING_ENTITY.BOOL.addFunction("op",(e,j) -> e.isOp());
        LIVING_ENTITY.BOOL.addFunction("inregion",(e,j) -> {
            if (j.size() > 0) {
                return RegionUtil.getInstance().inRegion(e,j.get(0).getAsString());
            }
            return false;
        });

    }
    public Function<T,?> getAsFunc(String t) {
        ConditionContainer<?> parse = types.stream().filter(c -> c.getAsFunc(t) != null).findFirst().orElse(null);
        return (parse != null) ? parse.getAsFunc(t) : null;

    }

    public ActionPredicate<T> find(String[] s) {
        ConditionContainer<?> parse = types.stream().filter(c -> c.parse(s[0],s[1],s[2]) != null).findFirst().orElse(null);
        if (parse != null) {
            return parse.parse(s[0],s[1],s[2]);
        } else {
            CutsceneMaker.warn("unable to read condition \"" + s[0] + " " + s[1] + " " + s[2] + "\"");
            return null;
        }
    }

    private ConditionParser() {
        NUMBER.converter = Double::parseDouble;
        STRING.converter = s -> s;
        BOOL.converter = s -> {
            switch (s) {
                case "true":
                case "false":
                    return Boolean.parseBoolean(s);
                default:
                    return null;
            }
        };

        NUMBER.addOperator("==", (a,b) -> a.doubleValue() == b.doubleValue());
        NUMBER.addOperator(">=",(a,b) -> a.doubleValue() >= b.doubleValue());
        NUMBER.addOperator("<=",(a,b) -> a.doubleValue() <= b.doubleValue());
        NUMBER.addOperator(">",(a,b) -> a.doubleValue() > b.doubleValue());
        NUMBER.addOperator("<",(a,b) -> a.doubleValue() < b.doubleValue());
        NUMBER.addOperator("!=",(a,b) -> a.doubleValue() != b.doubleValue());

        BOOL.addOperator("==",(a,b) -> a == b);
        BOOL.addOperator("!=",(a,b) -> a != b);

        STRING.addOperator("==", String::equals);
        STRING.addOperator("!=", (a,b) -> !a.equals(b));

        NUMBER.addFunction("num", (e,j) -> {
            if (j.size() == 0 || !(e instanceof Player)) return 0;
            return CutsceneMaker.getVars((Player) e,j.get(0).getAsString()).getAsNum();
        });
        BOOL.addFunction("bool", (e,j) -> {
            if (j.size() == 0 || !(e instanceof Player)) return false;
            return CutsceneMaker.getVars((Player) e,j.get(0).getAsString()).getAsBool();
        });
        STRING.addFunction("str", (e,j) -> {
            if (j.size() == 0 || !(e instanceof Player)) return "<none>";
            return CutsceneMaker.getVars((Player) e,j.get(0).getAsString()).getVar();
        });
    }

    public class ConditionContainer<R> {

        private final Map<String, ComparisonOperator<R>> comp;
        private final Map<String, JsonMethod<T, R>> func;
        @Setter
        private Function<String, R> converter;

        private ConditionContainer() {
            comp = new HashMap<>();
            func = new HashMap<>();
            types.add(this);
        }

        public ActionPredicate<T> parse(String f, String action, String t) {
            Function<T,R> func1 = getAsFunc(f);
            Function<T,R> func2 = getAsFunc(t);
            ComparisonOperator<R> compare = comp.get(action.toLowerCase());
            return (func1 != null && func2 != null && compare != null) ? parse(func1,compare,func2) : null;
        }
        public ActionPredicate<T> parse(Function<T,R> f, ComparisonOperator<R> c, Function<T,R> t) {
            return q -> {
                try {
                    return c.get(f.apply(q), t.apply(q));
                } catch (Exception e) {
                    return false;
                }
            };
        }

        public void addOperator(String name,ComparisonOperator<R> operator) {
            comp.put(name.toLowerCase(),operator);
        }
        public void addFunction(String name, JsonMethod<T, R> function) {
            func.put(name.toLowerCase(),function);
        }

        public ComparisonOperator<R> getOperator(String name) {
            return comp.get(name.toLowerCase());
        }
        public JsonMethod<T,R> getFunction(String name) {
            return func.get(name.toLowerCase());
        }


        public Function<T,R> getAsFunc(String s) {
            if (s.contains("[") && s.contains("]")) {
                String clazz = a(s);
                JsonMethod<T, R> m = func.get(clazz);
                String p = b(s.substring(clazz.length()));
                JsonElement e = (!p.equals("")) ? parser.parse(p) : new JsonArray();
                if (m != null && e != null && e.isJsonArray()) {
                    return m.getAsFunction(e.getAsJsonArray());
                }
            }
            R type = tryConvert(s);
            return (type != null) ? t -> type : null;
        }
        private R tryConvert(String s) {
            try {
                return (converter != null) ? converter.apply(s) : null;
            } catch (Exception e) {
                return null;
            }
        }

        private String a(String s) {
            StringBuilder ret = new StringBuilder();
            int loop = 0;
            while (loop < s.length() && !String.valueOf(s.charAt(loop)).equals("[")) {
                ret.append(s.charAt(loop));
                loop ++;
            }
            return ret.toString().replaceAll(" ","");
        }
        private String b(String s) {
            StringBuilder ret = new StringBuilder("[");
            int loop = 1;
            while (loop  < s.length() && !String.valueOf(s.charAt(loop - 1)).equals("]")) {
                ret.append(s.charAt(loop ));
                loop ++;
            }
            return ret.toString().replaceAll(" ","");
        }
    }
}
