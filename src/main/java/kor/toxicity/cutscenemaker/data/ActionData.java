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

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActionData extends CutsceneData {
    private static final Pattern ACTION_PATTERN = Pattern.compile("^(?<name>\\w+)(?<argument>\\{((\\w|\\W)*)})?$", Pattern.UNICODE_CHARACTER_CLASS);
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
        actions.put("multiply", ActMultiplyVariable.class);
        actions.put("divide", ActDivVariable.class);
        actions.put("entityeffect", ActEntityEffect.class);
        actions.put("potion", ActPotionEffect.class);
        actions.put("slate", ActReady.class);
        actions.put("action",ActAction.class);
        actions.put("mark",ActMark.class);
        actions.put("recall", ActRecall.class);
        actions.put("delete", ActDeleteVariable.class);
        actions.put("command", ActCommand.class);
        actions.put("cinematic", ActCinematic.class);
        actions.put("entry", ActEntry.class);
        actions.put("spawn", ActSpawn.class);
        actions.put("kill", ActKill.class);
        actions.put("if", ActIf.class);

        if (Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            actions.put("skript", ActSetSkriptVar.class);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            actions.put("addmoney", ActAddMoney.class);
            actions.put("removemoney", ActRemoveMoney.class);
        }
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
                        ActionPredicate<LivingEntity> add = ConditionParser.LIVING_ENTITY.find(get);
                        if (add != null) cond = cond.addAnd(getCond(add,get));
                    }
                    else cond = getCond(ConditionParser.LIVING_ENTITY.find(get),get);
                }
                container.setConditions(cond);
            }
            if (events != null) {
                events.forEach(e -> {
                    Matcher matcher = ACTION_PATTERN.matcher(e);
                    if (matcher.find()) EventData.addListener(container, matcher.group("name"), parser.parse(matcher.group("argument")).getAsJsonObject());
                });
            }
            container.confirm();
            if (config.isSet(s + ".Cooldown")) container.setCoolDown(config.getInt(s + ".Cooldown", -1));
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(actionContainer.size()) + " actions successfully loaded.");
    }
    private ActionPredicate<LivingEntity> getCond(ActionPredicate<LivingEntity> cond, String[] get) {
        if (cond != null && get.length > 4) switch (get[3]) {
            case "cast":
                return cond.cast(e -> start(get[4],e));
            case "castinstead":
                return cond.castInstead(e -> start(get[4],e));
        }
        return cond;
    }

    private CutsceneAction a(String k) throws NoActionFoundException {
        Matcher matcher = ACTION_PATTERN.matcher(k);
        if (!matcher.find()) throw new IllegalStateException("Unable to load statement \"" + k + "\".");

        String clazz = matcher.group("name");
        if (actions.containsKey(clazz)) {
            try {
                Class<? extends CutsceneAction> c = actions.get(clazz);
                CutsceneAction a = c.getDeclaredConstructor(CutsceneManager.class).newInstance(getPlugin().getManager());

                DataObject<CutsceneAction> obj = new DataObject<>(a);
                String arg = matcher.group("argument");
                if (arg != null) {
                    JsonElement e = parser.parse(arg.replaceAll("=", ":"));
                    obj.apply(e.getAsJsonObject());
                }
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

    public static void addAction(String name, Class<? extends CutsceneAction> action) {
        actions.putIfAbsent(name,action);
    }
    public static boolean start(String name, LivingEntity entity) {
        if (!actionContainer.containsKey(name)) return false;
        return actionContainer.get(name).run(entity);
    }
}
