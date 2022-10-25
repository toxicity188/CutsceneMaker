package kor.toxicity.cutscenemaker.util.conditions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.util.JsonMethod;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

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
        LIVING_ENTITY.NUMBER.addFunction("health%",(e,j) -> e.getHealth()/e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        LIVING_ENTITY.NUMBER.addFunction("health",(e,j) -> e.getHealth());
        LIVING_ENTITY.STRING.addFunction("name",(e,j) -> {
            if (j.size() > 0) return Bukkit.getPlayer(j.getAsString()).getName();
            return e.getName();
        });
        LIVING_ENTITY.BOOL.addFunction("op",(e,j) -> e.isOp());

    }

    public Predicate<T> getByString(String[] t) {
        if (t.length >= 3) {
            switch (t[2]) {
                default:
                    try {
                        long d = Long.parseLong(t[2]);
                        return NUMBER.parse(NUMBER.getAsFunc(t[0],Double.parseDouble(t[0])),t[1],d);
                    } catch (Exception e) {
                        return STRING.parse(STRING.getAsFunc(t[0],t[0]), t[1],t[2]);
                    }
                case "true":
                case "false":
                    return BOOL.parse(BOOL.getAsFunc(t[0],Boolean.parseBoolean(t[0])), t[1],Boolean.parseBoolean(t[2]));
            }
        } else return null;
    }

    private ConditionParser() {

        NUMBER.addOperator("==", (a,b) -> a.longValue() == b.longValue());
        NUMBER.addOperator(">=",(a,b) -> a.longValue() >= b.longValue());
        NUMBER.addOperator("<=",(a,b) -> a.longValue() <= b.longValue());
        NUMBER.addOperator(">",(a,b) -> a.longValue() > b.longValue());
        NUMBER.addOperator("<",(a,b) -> a.longValue() < b.longValue());
        NUMBER.addOperator("!=",(a,b) -> a.longValue() != b.longValue());

        BOOL.addOperator("==",(a,b) -> a == b);
        BOOL.addOperator("!=",(a,b) -> a != b);

        STRING.addOperator("==", String::equals);
        STRING.addOperator("!=", (a,b) -> !a.equals(b));

        NUMBER.addFunction("num", (e,j) -> {
            if (j.size() == 0 || j.get(0).getAsNumber() == null) return 0;
            return j.get(0).getAsNumber();
        });
        STRING.addFunction("str", (e,j) -> {
            if (j.size() == 0 || j.get(0).getAsString() == null) return "<none>";
            return j.get(0).getAsString();
        });
        BOOL.addFunction("bool", (e,j) -> {
            if (j.size() == 0) return false;
            return j.get(0).getAsBoolean();
        });
    }

    public class ConditionContainer<R> {

        private final Map<String, ComparisonOperator<R>> comp;
        private final Map<String, JsonMethod<T, R>> func;

        private ConditionContainer() {
            comp = new HashMap<>();
            func = new HashMap<>();
        }

        public Predicate<T> parse(Function<T,R> f, String action, R t) {
            ComparisonOperator<R> c = comp.get(action.toLowerCase());
            if (f == null && c == null) return null;
            return parse(f,c,t);
        }
        public Predicate<T> parse(Function<T,R> f, ComparisonOperator<R> c, R t) {
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
            return t -> def;
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
