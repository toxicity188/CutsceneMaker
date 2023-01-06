package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.events.CustomCutsceneEvent;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActCallEvent extends CutsceneAction {
    @DataField(aliases = "k",throwable = true)
    public String key;

    public ActCallEvent(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) EvtUtil.call(new CustomCutsceneEvent((Player) entity,key));
    }
}
