package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.ConditionBuilder;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestUtil {
    @Getter
    private static final QuestUtil instance = new QuestUtil();

    Dialog[] getDialog(List<String> list) {
        Dialog[] dialog = list.stream().map(this::getDialog).filter(Objects::nonNull).toArray(Dialog[]::new);
        return (dialog.length > 0) ? dialog : null;
    }
    QnA[] getQnA(List<String> list) {
        QnA[] qna = list.stream().map(s -> getFromMap(s,QuestData.QNA_MAP,"QnA")).filter(Objects::nonNull).toArray(QnA[]::new);
        return (qna.length > 0) ? qna : null;
    }
    Present[] getPresent(List<String> list) {
        Present[] presents = list.stream().map(s -> getFromMap(s,QuestData.PRESENT_MAP,"Present")).filter(Objects::nonNull).toArray(Present[]::new);
        return (presents.length > 0) ? presents : null;
    }
    Dialog getDialog(String s) {
        return getFromMap(s,QuestData.DIALOG_MAP,"Dialog");
    }
    private static <V> V getFromMap(String s, Map<String,V> targetMap, String name) {
        V v = targetMap.get(s);
        if (v == null) CutsceneMaker.warn("the " + name + " named \"" + s + "\" doesn't exist!");
        return v;
    }

    public Consumer<Player> getSoundPlay(String s) {
        String[] sounds = TextUtil.getInstance().split(s," ");
        String sound = sounds[0];
        final float volume, pitch;
        volume = (sounds.length > 1) ? getFloat(sounds[1]) : 1;
        pitch = (sounds.length > 2) ? getFloat(sounds[2]) : 1;
        return p -> p.playSound(p.getLocation(),sound,volume,pitch);
    }
    private float getFloat(String target) {
        try {
            return Float.parseFloat(target);
        } catch (Exception e) {
            return (float) 1;
        }
    }
    ItemBuilder[] getItemBuilders(List<String> names) {
        ItemBuilder[] builders = names.stream().map(s -> {
            String[] t = TextUtil.getInstance().split(s, " ");
            ItemBuilder builder = InvUtil.getInstance().toName(t[0]);
            if (builder == null) CutsceneMaker.warn("The item named \"" + t[0] + "\" doesn't exist!");
            if (builder != null && t.length > 1) {
                try {
                    return builder.setAmount(Integer.parseInt(t[1]));
                } catch (Exception e) {
                    CutsceneMaker.warn("Invalid number format: " + t[1]);
                }
            }
            return builder;
        }).filter(Objects::nonNull).toArray(ItemBuilder[]::new);
        return (builders.length > 0) ? builders : null;
    }
    Consumer<Player> getVarsConsumer(String key, String value, String change) {
        String lower = change.toLowerCase();
        switch (lower) {
            case "set":
            case "=":
                Function<LivingEntity,?> function = ConditionBuilder.LIVING_ENTITY.getAsFunc(value);
                return (function != null) ? p -> CutsceneMaker.getVars(p,key).setVar(function.apply(p).toString()) : null;
            case "remove":
            case "delete":
            case "del":
                return p -> CutsceneMaker.removeVars(p,key);
            default:
                Function<LivingEntity,Number> numberFunction = ConditionBuilder.LIVING_ENTITY.NUMBER.getAsFunc(value);
                if (numberFunction != null) {
                    switch (lower) {
                        default:
                            CutsceneMaker.warn("The variable operator \"" + lower + "\" doesn't exist!");
                            CutsceneMaker.warn("So it changed to \"+\" automatically.");
                        case "+":
                        case "add":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() + numberFunction.apply(p).doubleValue()));
                            };
                        case "-":
                        case "sub":
                        case "subtract":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() - numberFunction.apply(p).doubleValue()));
                            };
                        case "*":
                        case "mul":
                        case "multiply":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() * numberFunction.apply(p).doubleValue()));
                            };
                        case "/":
                        case "div":
                        case "divide":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() / numberFunction.apply(p).doubleValue()));
                            };
                    }
                } else {
                    CutsceneMaker.warn("The value \"" + value + "\" is not a number!");
                    return null;
                }
        }
    }
}
