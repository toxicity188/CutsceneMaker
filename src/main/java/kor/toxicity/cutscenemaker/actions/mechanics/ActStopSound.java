package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class ActStopSound extends CutsceneAction {

    @DataField(aliases = "s",throwable = true)
    public String sound;
    @DataField(aliases = "c")
    public String category;

    private Consumer<Player> stopSound;

    public ActStopSound(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (category != null) {
            SoundCategory ct = tryCategory();
            stopSound = p -> p.stopSound(sound,ct);
        } else stopSound = p -> p.stopSound(sound);
    }
    private SoundCategory tryCategory() {
        SoundCategory ct;
        try {
            ct = SoundCategory.valueOf(category.toUpperCase());
        } catch (Exception e) {
            ct = SoundCategory.MASTER;
        }
        return ct;
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) stopSound.accept((Player) entity);
    }
}
