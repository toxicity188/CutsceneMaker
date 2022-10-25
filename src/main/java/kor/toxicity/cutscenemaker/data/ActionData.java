package kor.toxicity.cutscenemaker.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.DataField;
import kor.toxicity.cutscenemaker.actions.mechanics.*;
import kor.toxicity.cutscenemaker.exceptions.NoActionFoundException;
import kor.toxicity.cutscenemaker.exceptions.NoValueFoundException;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.conditions.ConditionParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class ActionData extends CutsceneData {

    private static final Map<String,Class<? extends CutsceneAction>> actions = new HashMap<>();
    private static final Map<String, ActionContainer> actionContainer = new HashMap<>();
    private final JsonParser parser = new JsonParser();

    static {
        actions.put("teleport", ActTeleport.class);
        actions.put("dummy", ActDummy.class);
        actions.put("effectlib", ActEffectLib.class);
        actions.put("message", ActMessage.class);
        actions.put("give", ActGiveItem.class);
    }
    public ActionData(CutsceneMaker pl) {
        super(pl);
    }

    @Override
    public void reload() {
        actionContainer.clear();
        ConfigLoad config = getPlugin().read("Action");
        config.getAllFiles().forEach(s -> {
            List<String> key = config.getStringList(s + ".Actions");
            List<String> condition = config.getStringList(s + ".Conditions");
            ActionContainer container = new ActionContainer(getPlugin());
            if (key != null) {
                key.forEach(k -> {
                    try {
                        CutsceneAction a = a(k);
                        container.add(a);
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.WARNING,"[CutsceneMaker] Error: " + e.getMessage() + " (Action " + s + ")");
                    }
                });
            }
            if (container.size() > 0) actionContainer.put(s,container);
            else return;
            if (condition != null) {

                Predicate<LivingEntity> cond = null;
                for (String t : condition) {
                    String[] get = t.split(" ");
                    if (cond != null) cond = cond.and(ConditionParser.LIVING_ENTITY.getByString(get));
                    else cond = ConditionParser.LIVING_ENTITY.getByString(get);
                }
                container.setConditions(cond);
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(actionContainer.size()) + " actions successfully loaded.");
    }
    private CutsceneAction a(String k) throws NoActionFoundException {
        String clazz = getFirst(k);
        if (!clazz.equals("") && actions.containsKey(clazz)) {
            try {
                Class<? extends CutsceneAction> c = actions.get(clazz);
                CutsceneAction a = c.getDeclaredConstructor(CutsceneManager.class).newInstance(getPlugin().getManager());
                JsonElement e = parser.parse(k.substring(clazz.length()).replaceAll("=",":"));
                List<Field> adapt = Arrays.stream(c.getFields()).filter(f -> b(f) != null).collect(Collectors.toList());
                if (e != null) {
                    je(e,(key,value) -> {
                        Field set = adapt.stream().filter(g -> key.equals(g.getName()) || Arrays.asList(b(g).aliases()).contains(key)).findFirst().orElse(null);
                        if (set != null) fieldset(a).accept(set,value);
                    });
                }
                Field thr = adapt.stream().filter(q -> {
                    try {
                        return b(q).throwable() && q.get(a) == null;
                    } catch (Exception ex) {
                        return true;
                    }
                }).findFirst().orElse(null);
                if (thr != null) throw new NoValueFoundException("Class \"" + clazz + "\" must set value \"" + thr.getName() + "\"");
                a.initialize();
                return a;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NoActionFoundException("Unable to load Action \"" + clazz + "\".");
        }
    }
    private DataField b(Field f) {
        return f.getDeclaredAnnotation(DataField.class);
    }
    private BiConsumer<Field,JsonElement> fieldset(Object a) {
        return (f,j) -> {
            try {
                Object p = null;
                if (f.getType() == Integer.TYPE) p = j.getAsInt();
                if (f.getType() == Double.TYPE) p = j.getAsDouble();
                if (f.getType() == Float.TYPE) p = j.getAsFloat();
                if (f.getType() == Boolean.TYPE) p = j.getAsBoolean();
                if (f.getType() == String.class) p = j.getAsString();
                if (f.getType() == JsonObject.class && j.isJsonObject()) p = j.getAsJsonObject();
                f.set(a, p);
            } catch (IllegalAccessException ignored) {}
        };
    }

    private void je(JsonElement e, BiConsumer<String,JsonElement> action) {
        if (!e.isJsonObject()) return;
        e.getAsJsonObject().entrySet().forEach(s -> action.accept(s.getKey(),s.getValue()));
    }
    private void ja(JsonElement e, Consumer<JsonElement> action) {
        if (!e.isJsonArray()) return;
        e.getAsJsonArray().forEach(action);
    }

    private String getFirst(String s) {
        StringBuilder ret = new StringBuilder();
        int loop = 0;
        while (loop < s.length() && !String.valueOf(s.charAt(loop)).equals("{")) {
            ret.append(s.charAt(loop));
            loop ++;
        }
        return ret.toString().replaceAll(" ","");
    }

    public static void addAction(String name, Class<? extends CutsceneAction> action) {
        if (!actions.containsKey(name)) actions.put(name,action);
    }
    public static boolean start(String name, LivingEntity entity) {
        if (!actionContainer.containsKey(name)) return false;
        return actionContainer.get(name).run(entity);
    }
}
