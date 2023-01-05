package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestUtil {
    @Getter
    private static final QuestUtil instance = new QuestUtil();

    Dialog[] getDialog(List<String> list) {
        Dialog[] dialog = list.stream().map(l -> {
            Dialog d = QuestData.DIALOG_MAP.get(l);
            if (d == null) CutsceneMaker.warn("the Dialog named \"" + l + "\" doesn't exist!");
            return d;
        }).filter(Objects::nonNull).toArray(Dialog[]::new);
        return (dialog.length > 0) ? dialog : null;
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
    Consumer<Player> getVarsConsumer(String key, String value, String change) {
        switch (change) {
            case "set":
            case "=":
                return p -> CutsceneMaker.getVars(p,key).setVar(value);
            case "remove":
            case "delete":
            case "del":
                return p -> CutsceneMaker.removeVars(p,key);
            default:
                try {
                    double d = Double.parseDouble(value);
                    switch (change) {
                        default:
                        case "+":
                        case "add":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() + d));
                            };
                        case "-":
                        case "sub":
                        case "subtract":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() - d));
                            };
                        case "*":
                        case "mul":
                        case "multiply":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() * d));
                            };
                        case "/":
                        case "div":
                        case "divide":
                            return p -> {
                                Vars vars = CutsceneMaker.getVars(p,key);
                                vars.setVar(Double.toString(vars.getAsNum(0).doubleValue() / d));
                            };
                    }
                } catch (Exception e) {
                    CutsceneMaker.warn("The value \"" + value + "\" is not a number!");
                    return null;
                }
        }
    }
}
