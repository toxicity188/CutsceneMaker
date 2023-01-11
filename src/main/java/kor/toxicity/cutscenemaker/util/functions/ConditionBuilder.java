package kor.toxicity.cutscenemaker.util.functions;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.*;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConditionBuilder<T> {

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?<name>\\w+)(?<argument>\\[((\\w|,|\\s|\")*)])", Pattern.UNICODE_CHARACTER_CLASS);
    private static final JsonParser PARSER = new JsonParser();
    public static final ConditionBuilder<LivingEntity> LIVING_ENTITY = new ConditionBuilder<>();
    private final Set<ConditionContainer<?>> types = new LinkedHashSet<>();

    public final ConditionContainer<Number> NUMBER = new ConditionContainer<>();
    public final ConditionContainer<Boolean> BOOL = new ConditionContainer<>();
    public final ConditionContainer<String> STRING = new ConditionContainer<>();

    static {
        LIVING_ENTITY.STRING.addFunction("colon",(p,j) -> ":");
        LIVING_ENTITY.STRING.addFunction("tool",(p,j) -> {
            if (p instanceof Player) {
                ItemStack i = ((Player) p).getInventory().getItemInMainHand();
                if (i != null && i.getItemMeta().getDisplayName() != null) return i.getItemMeta().getDisplayName();
            }
            return "<none>";
        });
        LIVING_ENTITY.STRING.addFunction("location",(e,j) -> TextUtil.getInstance().toSimpleLoc(e.getLocation()));
        LIVING_ENTITY.NUMBER.addFunction("money",(e,j) -> {
            if (e instanceof Player) {
                return MoneyUtil.getInstance().getMoney((Player) e);
            }
            return 0;
        });
        LIVING_ENTITY.NUMBER.addFunction("healthpercentage",(e,j) -> e.getHealth()/e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        LIVING_ENTITY.NUMBER.addFunction("health",(e,j) -> e.getHealth());
        LIVING_ENTITY.NUMBER.addFunction("random",new CheckableFunction<LivingEntity, Number>() {
            private final ThreadLocalRandom random = ThreadLocalRandom.current();
            @Override
            public boolean check(JsonArray array) {
                for (int i = 0; i < array.size(); i++) {
                    try {
                        array.get(i).getAsDouble();
                    } catch (Exception e) {
                        return false;
                    }
                }
                return true;
            }
            @Override
            public Number apply(LivingEntity entity, JsonArray p) {
                switch (p.size()) {
                    case 0:
                        return random.nextDouble();
                    case 1:
                        return random.nextDouble(p.get(0).getAsDouble());
                    default:
                        return random.nextDouble(p.get(0).getAsDouble(),p.get(1).getAsDouble());
                }
            }
        });
        LIVING_ENTITY.NUMBER.addFunction("randomInt", new CheckableFunction<LivingEntity, Number>() {
            private final ThreadLocalRandom random = ThreadLocalRandom.current();
            @Override
            public boolean check(JsonArray array) {
                for (int i = 0; i < array.size(); i++) {
                    try {
                        array.get(i).getAsInt();
                    } catch (Exception e) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public Number apply(LivingEntity entity, JsonArray p) {
                switch (p.size()) {
                    case 0:
                        return random.nextInt();
                    case 1:
                        return random.nextInt(p.get(0).getAsInt() + 1);
                    default:
                        return random.nextInt(p.get(0).getAsInt(),p.get(1).getAsInt() + 1);
                }
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
        LIVING_ENTITY.BOOL.addFunction("hasitem",new CheckableFunction<LivingEntity,Boolean>() {
            @Override
            public Boolean apply(LivingEntity entity, JsonArray array) {
                if (!(entity instanceof Player)) return false;
                Player p = (Player) entity;
                return InvUtil.getInstance().has(p,ItemData.getItem(p,array.get(0).getAsString()));
            }

            @Override
            public boolean check(JsonArray array) {
                return array.size() > 0 && InvUtil.getInstance().toName(array.get(0).getAsString()) != null;
            }
        });

        LIVING_ENTITY.NUMBER.addFunction("storage",(e,j) -> {
            if (!(e instanceof Player)) return 0;
            Player p = (Player) e;
            return InvUtil.getInstance().storage(p,(j.size() > 0) ? ItemData.getItem(p,j.get(0).getAsString()) : null);
        });
        LIVING_ENTITY.NUMBER.addFunction("amount", new CompileFunction<LivingEntity, Number>() {
            @Override
            public FunctionCompiler<LivingEntity, Number> initialize() {
                return new FunctionCompiler<LivingEntity, Number>() {
                    private ItemBuilder builder;
                    @Override
                    public void initialize(JsonArray array) {
                        builder = ItemData.getItem(array.get(0).getAsString());
                    }

                    @Override
                    public Function<LivingEntity, Number> compile() {
                        return e -> {
                            if (!(e instanceof Player)) return 0;
                            Player p = (Player) e;
                            return InvUtil.getInstance().getTotalAmount(p,builder.get(p));
                        };
                    }
                };
            }

            @Override
            public boolean check(JsonArray array) {
                return array.size() > 0 && InvUtil.getInstance().toName(array.get(0).getAsString()) != null;
            }
        });


        LIVING_ENTITY.NUMBER.addFunction("emptyspace",(e,j) -> {
            if (!(e instanceof Player)) return 0;
            return InvUtil.getInstance().emptySpace((Player) e);
        });
        LIVING_ENTITY.NUMBER.addFunction("num", new CheckableFunction<LivingEntity, Number>() {
            @Override
            public boolean check(JsonArray array) {
                return array.size() > 0;
            }
            @Override
            public Number apply(LivingEntity entity, JsonArray p) {
                if (!(entity instanceof Player)) return 0;
                return CutsceneMaker.getVars((Player) entity,p.get(0).getAsString()).getAsNum();
            }
        });
        LIVING_ENTITY.BOOL.addFunction("bool", new CheckableFunction<LivingEntity, Boolean>() {
            @Override
            public boolean check(JsonArray array) {
                return array.size() > 0;
            }
            @Override
            public Boolean apply(LivingEntity entity, JsonArray array) {
                if (!(entity instanceof Player)) return false;
                return CutsceneMaker.getVars((Player) entity,array.get(0).getAsString()).getAsBool();
            }
        });
        LIVING_ENTITY.STRING.addFunction("str", new CheckableFunction<LivingEntity, String>() {
            @Override
            public boolean check(JsonArray array) {
                return array.size() > 0;
            }
            @Override
            public String apply(LivingEntity entity, JsonArray array) {
                if (!(entity instanceof Player)) return "<none>";
                return CutsceneMaker.getVars((Player) entity,array.get(0).getAsString()).getVar();
            }
        });

    }
    public static boolean isFunction(String target) {
        return FUNCTION_PATTERN.matcher(target).find();
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

    private ConditionBuilder() {
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

    }

    public class ConditionContainer<R> {

        private final Map<String, ComparisonOperator<R>> comp;
        private final Map<String, JsonFunction<T, R>> func;
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
        public void addFunction(String name, JsonFunction<T, R> function) {
            func.put(name.toLowerCase(),function);
        }

        public ComparisonOperator<R> getOperator(String name) {
            return comp.get(name.toLowerCase());
        }
        public JsonFunction<T,R> getFunction(String name) {
            return func.get(name.toLowerCase());
        }


        public Function<T,R> getAsFunc(String s) {
            if (s == null) return null;
            Matcher matcher = FUNCTION_PATTERN.matcher(s);

            if (matcher.find()) {
                JsonFunction<T, R> m = func.get(matcher.group("name").toLowerCase());
                JsonElement e = PARSER.parse(matcher.group("argument"));

                if (m != null && e != null && e.isJsonArray()) {
                    JsonArray array = e.getAsJsonArray();
                    if (m instanceof CheckableFunction && !((CheckableFunction<T, R>) m).check(array)) {
                        CutsceneMaker.warn("The argument \"" + matcher.group("argument") + "\" is not valid at the function \"" + matcher.group("name") + "\".");
                        return null;
                    }
                    return m.getAsFunction(array);
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

    }
}
