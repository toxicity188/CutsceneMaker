package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ActPotionEffect extends CutsceneAction {

    @DataField(throwable = true)
    public String type;
    @DataField(aliases = {"dur","d"})
    public int duration = 20;
    @DataField(aliases = {"amp","amplifier","s","str"})
    public int strength = 0;
    @DataField(aliases = {"amb","a"})
    public boolean ambient = true;
    @DataField(aliases = "p")
    public boolean particle = false;
    @DataField
    public boolean toggle = true;

    private PotionEffectType t;
    private PotionEffect effect;

    public ActPotionEffect(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        t = PotionEffectType.getByName(type);
        if (t == null) t = PotionEffectType.SPEED;
        effect = new PotionEffect(t,duration,strength,ambient,particle);

        type = null;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity.hasPotionEffect(t) && toggle) entity.removePotionEffect(t);
        else entity.addPotionEffect(effect);

    }

}
