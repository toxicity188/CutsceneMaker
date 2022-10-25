package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ActReady extends CutsceneAction {

    private static final Set<Player> toggle = new HashSet<>();

    public ActReady(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (toggle.contains(player)) {
                //TODO Make effect
            }

        }
    }
}
