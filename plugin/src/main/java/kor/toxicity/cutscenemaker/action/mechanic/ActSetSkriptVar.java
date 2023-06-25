package kor.toxicity.cutscenemaker.action.mechanic;

import ch.njol.skript.variables.Variables;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActSetSkriptVar extends CutsceneAction {

    @DataField(aliases = "n",throwable = true)
    public FunctionPrinter name;

    @DataField(aliases = "v",throwable = true)
    public FunctionPrinter value;

    @DataField
    public String type = "str";

    private Function<String,Object> parser;

    private static final Map<String, Function<String,Object>> data = new HashMap<>();
    static {
        data.put("str",s -> s);
        data.put("int",s -> {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return null;
            }
        });
        data.put("byte",s -> {
            try {
                return Byte.parseByte(s);
            } catch (Exception e) {
                return null;
            }
        });
        data.put("short",s -> {
            try {
                return Short.parseShort(s);
            } catch (Exception e) {
                return null;
            }
        });
        data.put("long",s -> {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                return null;
            }
        });
        data.put("double",s -> {
            try {
                return Double.parseDouble(s);
            } catch (Exception e) {
                return null;
            }
        });
        data.put("bool",s -> {
            try {
                return Boolean.parseBoolean(s);
            } catch (Exception e) {
                return null;
            }
        });
    }
    public static void addDataType(String t, Function<String,Object> get) {
        assert t != null && get != null;
        data.put(t,get);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (data.containsKey(type)) parser = data.get(type);
        else parser = s -> s;
    }

    public ActSetSkriptVar(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Variables.setVariable(name.print(entity),parser.apply(value.print(entity)),null,false);
        }
    }
}
