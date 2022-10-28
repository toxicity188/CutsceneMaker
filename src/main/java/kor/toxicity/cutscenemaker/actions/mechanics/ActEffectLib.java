package kor.toxicity.cutscenemaker.actions.mechanics;

import com.google.gson.JsonObject;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;

public class ActEffectLib extends CutsceneAction {

    @DataField(aliases = "p",throwable = true)
    public JsonObject parameters;
    @DataField(aliases = {"c", "class"},throwable = true)
    public String effectClass;
    @DataField(aliases = "f")
    public boolean follow = false;

    private final EffectManager manager;
    private ConfigurationSection section;

    public ActEffectLib(CutsceneManager pl) {
        super(pl);
        manager = pl.getEffectLib();
    }

    @Override
    public void initialize() {
        section = new MemoryConfiguration();
        parameters.entrySet().forEach(e -> section.set(e.getKey(),e.getValue().getAsString().replaceAll("PI",Double.toString(Math.PI))));
    }

    @Override
    public void apply(LivingEntity entity) {
        if (manager != null) manager.start(effectClass,section,entity.getLocation(),(follow) ? entity : null);
        section.getKeys(false).forEach(s -> System.out.println(s + " : " + section.get(s)));
    }
}
