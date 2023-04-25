package kor.toxicity.cutscenemaker.action.mechanic;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActCast extends CutsceneAction {

    @DataField(aliases = "n",throwable = true)
    public String name;
    private Spell spell;

    @DataField(aliases = "p")
    public float power = 1.0F;

    public ActCast(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        spell = MagicSpells.getSpellByInGameName(name);
        if (spell == null) CutsceneMaker.warn("The Spell named \"" + name + "\" doesn't exist!");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (spell != null && entity instanceof Player) spell.cast((Player) entity,power,null);
    }
}
