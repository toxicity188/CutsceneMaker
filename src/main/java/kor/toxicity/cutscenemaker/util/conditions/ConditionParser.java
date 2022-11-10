package kor.toxicity.cutscenemaker.util.conditions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.JsonMethod;
import kor.toxicity.cutscenemaker.util.RegionUtil;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class ConditionParser<T> {

    private static final JsonParser parser = new JsonParser();

    public static final ConditionParser<LivingEntity> LIVING_ENTITY = new ConditionParser<>();
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
                return RegionUtil.getInstance().inRegion(e,j.get(0).getAsString(),j.size() > 1 ? j.get(1).getAsString() : null);
            }
            return false;
        });

    }

    public ActionPredicate<T> getByString(String[] t) {
        if (t.length >= 3) {
            switch (t[2]) {
                default:
                    try {
                        double d = Double.parseDouble(t[2]);
                        return NUMBER.parse(NUMBER.getAsFunc(t[0],0),t[1],d);
                    } catch (Exception e) {
                        return STRING.parse(STRING.getAsFunc(t[0],t[0]), t[1],t[2]);
                    }
                case "true":
                case "false":
                    return BOOL.parse(BOOL.getAsFunc(t[0],Boolean.parseBoolean(t[0])), t[1],Boolean.parseBoolean(t[2]));
            }
        } else return null;
    }
    public Function<T,?> getAsFunc(String t) {
        Function<T,?> f = BOOL.getAsFunc(t,null);
        if (f == null) f = NUMBER.getAsFunc(t,null);
        if (f == null) f = STRING.getAsFunc(t, null);
        return f;

    }

    private ConditionParser() {

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
        STRING.addFunction("str", (e,j) -> {
            if (j.size() == 0 || !(e instanceof Player)) return "<none>";
            return CutsceneMaker.getVars((Player) e,j.get(0).getAsString()).getVar();
        });
        BOOL.addFunction("bool", (e,j) -> {
            if (j.size() == 0 || !(e instanceof Player)) return false;
            return CutsceneMaker.getVars((Player) e,j.get(0).getAsString()).getAsBool();
        });
    }

    public class ConditionContainer<R> {

        private final Map<String, ComparisonOperator<R>> comp;
        private final Map<String, JsonMethod<T, R>> func;

        private ConditionContainer() {
            comp = new HashMap<>();
            func = new HashMap<>();
        }

        public ActionPredicate<T> parse(Function<T,R> f, String action, R t) {
            ComparisonOperator<R> c = comp.get(action.toLowerCase());
            if (f == null && c == null) return null;
            return parse(f,c,t);
        }
        public ActionPredicate<T> parse(Function<T,R> f, ComparisonOperator<R> c, R t) {
            return q -> {
                try {
                    return c.get(f.apply(q), t);
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


        public Function<T,R> getAsFunc(String s, R def) {
            if (s.contains("[") && s.contains("]")) {
                String clazz = a(s);
                JsonMethod<T, R> m = func.get(clazz);
                String p = b(s.substring(clazz.length()));
                JsonElement e = (!p.equals("")) ? parser.parse(p) : new JsonArray();
                if (m != null && e != null && e.isJsonArray()) {
                    return m.getAsFunction(e.getAsJsonArray());
                }
            }
            return (def != null) ? t -> def : null;
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
