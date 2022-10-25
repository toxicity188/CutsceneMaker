package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;

abstract class CutsceneData implements Reloadable {
    private final CutsceneMaker pl;

    CutsceneData(CutsceneMaker pl) {
        this.pl = pl;
    }

    protected final CutsceneMaker getPlugin() {
        return pl;
    }
}
