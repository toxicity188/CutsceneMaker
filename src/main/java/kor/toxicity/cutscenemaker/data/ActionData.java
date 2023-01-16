package kor.toxicity.cutscenemaker.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.mechanics.*;
import kor.toxicity.cutscenemaker.exceptions.NoActionFoundException;
import kor.toxicity.cutscenemaker.exceptions.NoValueFoundException;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.reflect.DataObject;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ActionPredicate;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActionData extends CutsceneData {
    private static final Pattern ACTION_PATTERN = Pattern.compile("^(?<name>\\w+)(?<argument>\\{((\\w|\\W)*)})?$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DELAY_PATTERN = Pattern.compile("^delay(\\s*)(?<ticks>[0-9]+$)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Map<String, Class<? extends CutsceneAction>> actions = new HashMap<>();
    private static final Map<String, ActionContainer> actionContainer = new HashMap<>();
    private static final JsonParser PARSER = new JsonParser();

    static {
        actions.put("teleport", ActTeleport.class);
        actions.put("dummy", ActDummy.class);
        actions.put("effectlib", ActEffectLib.class);
        actions.put("message", ActMessage.class);
        actions.put("item", ActItem.class);
        actions.put("sound", ActSound.class);
        actions.put("set",ActSetVariable.class);
        actions.put("add",ActAddVariable.class);
        actions.put("multiply", ActMultiplyVariable.class);
        actions.put("divide", ActDivVariable.class);
        actions.put("entityeffect", ActEntityEffect.class);
        actions.put("potion", ActPotionEffect.class);
        actions.put("slate", ActSlate.class);
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
        actions.put("warp", ActWarp.class);
        actions.put("leap", ActLeap.class);
        actions.put("dialog", ActDialog.class);
        actions.put("quest", ActQuest.class);
        actions.put("open", ActOpen.class);
        actions.put("callevent", ActCallEvent.class);
        actions.put("static", ActStatic.class);

        if (Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            actions.put("skript", ActSetSkriptVar.class);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            actions.put("addmoney", ActAddMoney.class);
            actions.put("removemoney", ActRemoveMoney.class);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("MagicSpells")) {
            actions.put("cast", ActCast.class);
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
                        CutsceneMaker.warn("Error: " + e.getMessage() + " (Action " + s + ")");
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
                        ActionPredicate<LivingEntity> add = ConditionBuilder.LIVING_ENTITY.find(get);
                        if (add != null) cond = cond.addAnd(getCond(add,get));
                    }
                    else cond = getCond(ConditionBuilder.LIVING_ENTITY.find(get),get);
                }
                container.setConditions(cond);
            }
            if (events != null) {
                events.forEach(e -> addHandler(e,container));
            }
            container.confirm();
            if (config.isSet(s + ".Cooldown")) container.setCoolDown(config.getInt(s + ".Cooldown", -1));
        });
        actionContainer.forEach((a,b) -> {
            if (b.lateCheck != null) {
                b.lateCheck.forEach(c -> c.accept(actionContainer));
                b.lateCheck = null;
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(actionContainer.size()) + " Actions successfully loaded.");
    }
    public static boolean addHandler(String parameter, ActionContainer container) {
        Matcher matcher = ACTION_PATTERN.matcher(parameter);
        boolean result = matcher.find();
        if (result) {
            String arg = matcher.group("argument");
            EventData.addListener(container, matcher.group("name"), (arg != null) ? PARSER.parse(arg).getAsJsonObject() : new JsonObject());
        } else CutsceneMaker.warn("Unable to load statement \"" + parameter + "\".");
        return result;
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
        Matcher delay = DELAY_PATTERN.matcher(k.toLowerCase());
        if (delay.find()) {
            CutsceneAction action = new ActDummy(getPlugin().getManager());
            action.delay = Integer.parseInt(delay.group("ticks"));
            action.initialize();
            return action;
        }

        Matcher matcher = ACTION_PATTERN.matcher(k);
        if (!matcher.find()) throw new IllegalStateException("Unable to load statement \"" + k + "\".");

        String clazz = matcher.group("name");
        if (actions.containsKey(clazz)) {
            try {
                Class<? extends CutsceneAction> c = actions.get(clazz);
                CutsceneAction a = c.getDeclaredConstructor(CutsceneManager.class).newInstance(getPlugin().getManager());

                DataObject obj = new DataObject(a,"Action " + clazz);
                Optional.ofNullable(matcher.group("argument")).ifPresent(arg -> {
                    JsonElement e = PARSER.parse(arg.replaceAll("=", ":"));
                    obj.apply(e.getAsJsonObject());
                });
                if (!obj.isLoaded()) throw new NoValueFoundException("Class \"" + clazz + "\" must set value \"" + TextUtil.getInstance().toSingleText(obj.getErrorField()) + "\"");
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
