package kor.toxicity.cutscenemaker.action.mechanic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
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
        super.initialize();
        section = new MemoryConfiguration();
        parameters.entrySet().forEach(e -> {
            JsonElement element = e.getValue();
            try {
                section.set(e.getKey(),element.getAsDouble());
            } catch (Exception t) {
                section.set(e.getKey(),element.getAsString());
            }
        });

    }
    @Override
    public void apply(LivingEntity entity) {
        if (manager != null) {
            manager.start(effectClass,section,entity.getLocation(),(follow) ? entity : null);
        }
        else CutsceneMaker.warn("EffectLib doesn't exists.");
    }
}
