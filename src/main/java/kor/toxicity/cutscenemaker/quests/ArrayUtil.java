package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArrayUtil {
    @Getter
    private static final ArrayUtil instance = new ArrayUtil();

    Dialog[] getDialog(List<String> list) {
        return list.stream().map(l -> {
            Dialog d = DialogData.DIALOG_MAP.get(l);
            if (d == null) CutsceneMaker.warn("the dialog named \"" + l + "\" doesn't exists!");
            return d;
        }).filter(Objects::nonNull).toArray(Dialog[]::new);
    }
}
