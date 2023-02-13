package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.LocationStudio;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActQuitStudio extends CutsceneAction {

    @DataField(aliases = "b")
    public boolean back;

    public ActQuitStudio(CutsceneManager pl) {
        super(pl);
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            if (back) LocationStudio.quit((Player) entity);
            else LocationStudio.quitWithoutBack((Player) entity);
        }
    }
}
