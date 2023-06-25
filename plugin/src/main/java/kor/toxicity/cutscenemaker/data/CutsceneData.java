package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;

public abstract class CutsceneData implements Reloadable {
    private final CutsceneMaker pl;

    public CutsceneData(CutsceneMaker pl) {
        this.pl = pl;
    }

    protected final CutsceneMaker getPlugin() {
        return pl;
    }
}
