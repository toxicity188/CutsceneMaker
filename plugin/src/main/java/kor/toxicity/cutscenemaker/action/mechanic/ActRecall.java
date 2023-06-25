package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ActRecall extends CutsceneAction {

    @DataField(aliases = "d")
    public boolean delete = true;


    public ActRecall(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (ActMark.LOCATION.containsKey(entity)) {
            entity.teleport(ActMark.LOCATION.get(entity), PlayerTeleportEvent.TeleportCause.PLUGIN);
            if (delete) ActMark.LOCATION.remove(entity);
        }
    }
}
