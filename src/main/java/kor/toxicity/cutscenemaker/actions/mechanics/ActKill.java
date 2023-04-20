package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActKill extends CutsceneAction {

    @DataField(aliases = "k",throwable = true)
    public String key;
    @DataField
    public boolean bound = true;

    public ActKill(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            EntityManager m = manager.getEntityManager();
            if (bound) m.remove((Player) entity,key);
            else m.remove(key);
        }
    }
}
