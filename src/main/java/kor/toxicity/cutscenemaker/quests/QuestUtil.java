package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.TextUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class QuestUtil {
    @Getter
    private static final QuestUtil instance = new QuestUtil();

    Dialog[] getDialog(List<String> list) {
        return list.stream().map(l -> {
            Dialog d = QuestData.DIALOG_MAP.get(l);
            if (d == null) CutsceneMaker.warn("the dialog named \"" + l + "\" doesn't exists!");
            return d;
        }).filter(Objects::nonNull).toArray(Dialog[]::new);
    }
    Consumer<Player> getSoundPlay(String s) {
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
}
