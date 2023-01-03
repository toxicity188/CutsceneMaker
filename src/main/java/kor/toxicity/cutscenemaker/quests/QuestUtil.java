package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestUtil {
    @Getter
    private static final QuestUtil instance = new QuestUtil();
    private static final Pattern SIMPLE_ITEM_PATTERN = Pattern.compile("\\?(?<type>(\\w|_)+) (?<data>[0-9]+) (?<name>(\\w|\\W)+)", Pattern.UNICODE_CHARACTER_CLASS);

    Dialog[] getDialog(List<String> list) {
        return list.stream().map(l -> {
            Dialog d = QuestData.DIALOG_MAP.get(l);
            if (d == null) CutsceneMaker.warn("the Dialog named \"" + l + "\" doesn't exists!");
            return d;
        }).filter(Objects::nonNull).toArray(Dialog[]::new);
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
    ItemBuilder getBuilder(ConfigurationSection c, String s) {
        if (c.isItemStack(s)) return new ItemBuilder(c.getItemStack(s));
        else if (c.isString(s)) {
            Matcher matcher = SIMPLE_ITEM_PATTERN.matcher(c.getString(s));
            if (matcher.find()) {
                Material material;
                try {
                    material = Material.valueOf(matcher.group("type").toUpperCase());
                } catch (Exception e) {
                    material = Material.APPLE;
                }
                ItemStack itemStack = new ItemStack(material);
                itemStack.setDurability(Short.parseShort(matcher.group("data")));
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + TextUtil.getInstance().colored(matcher.group("name")));
                itemStack.setItemMeta(meta);
                return new ItemBuilder(itemStack);
            } else {
                ItemBuilder builder = ItemData.getItem(c.getString(s));
                if (builder == null) CutsceneMaker.warn("The item \"" + c.getString(s) + "\" doesn't exists!");
                return builder;
            }
        } else return null;
    }
    Consumer<Player> getVarsConsumer(String key, String value, String change) {
        if (change.equals("set") || change.equals("=")) return p -> CutsceneMaker.getVars(p,key).setVar(value);
        else {
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
