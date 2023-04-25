package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import org.bukkit.entity.LivingEntity;

public class ActDisguise extends CutsceneAction {

    @DataField(aliases = "k",throwable = true)
    public String key;
    private Disguise type;

    public ActDisguise(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            type = DisguiseAPI.getCustomDisguise(key);
            if (type == null) CutsceneMaker.warn("The custom disguise \"" + key + "\" doesn't exist!");
        } catch (Throwable throwable) {
            CutsceneMaker.warn("LibsDisguise not found.");
        }
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (type != null) {
            try {
                DisguiseAPI.disguiseToAll(entity,type);
            } catch (Throwable throwable) {
                CutsceneMaker.warn("LibsDisguise not found.");
            }
        }
    }
}
