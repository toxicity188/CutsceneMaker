package kor.toxicity.cutscenemaker.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.mechanics.*;
import kor.toxicity.cutscenemaker.exceptions.NoActionFoundException;
import kor.toxicity.cutscenemaker.exceptions.NoValueFoundException;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.DataObject;
import kor.toxicity.cutscenemaker.util.TextParser;
import kor.toxicity.cutscenemaker.util.conditions.ActionPredicate;
import kor.toxicity.cutscenemaker.util.conditions.ConditionParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;

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
        actions.put("sound", ActSound.class);
        actions.put("set",ActSetVariable.class);
        actions.put("add",ActAddVariable.class);
        actions.put("entityeffect", ActEntityEffect.class);
        actions.put("potion", ActPotionEffect.class);
        actions.put("slate", ActReady.class);
        actions.put("action",ActAction.class);
        actions.put("mark",ActMark.class);
        actions.put("recall", ActRecall.class);
        actions.put("delete", ActDeleteVariable.class);
        actions.put("command", ActCommand.class);
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
            List<String> events = config.getStringList(s + ".Events");

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

                ActionPredicate<LivingEntity> cond = null;
                for (String t : condition) {
                    String[] get = t.split(" ");
                    if (cond != null) {
                        ActionPredicate<LivingEntity> add = ConditionParser.LIVING_ENTITY.getByString(get);
                        if (add != null) cond = cond.addAnd(add);
                    }
                    else cond = ConditionParser.LIVING_ENTITY.getByString(get);
                    assert cond != null;
                    if (get.length > 4) switch (get[3]) {
                        case "cast":
                            cond = cond.cast(e -> start(get[4],e));
                            break;
                        case "castinstead":
                            cond = cond.castInstead(e -> start(get[4],e));
                            break;
                    }
                }
                container.setConditions(cond);
            }
            if (events != null) {
                events.forEach(e -> {
                    String t = getFirst(e);
                    EventData.addListener(container,t,parser.parse(getLast(e,t)).getAsJsonObject());
                });
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

                DataObject<CutsceneAction> obj = new DataObject<>(a);

                obj.apply(e.getAsJsonObject());
                if (!obj.isLoaded()) throw new NoValueFoundException("Class \"" + clazz + "\" must set value \"" + TextParser.getInstance().toSingleText(obj.getErrorField()) + "\"");
                a.initialize();
                return a;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NoActionFoundException("Unable to load Action \"" + clazz + "\".");
        }
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
    private String getLast(String t, String first) {
        return t.substring(first.length()).replaceAll("=",":");
    }

    public static void addAction(String name, Class<? extends CutsceneAction> action) {
        actions.putIfAbsent(name,action);
    }
    public static boolean start(String name, LivingEntity entity) {
        if (!actionContainer.containsKey(name)) return false;
        return actionContainer.get(name).run(entity);
    }
}
