package kor.toxicity.cutscenemaker.actions.mechanics;

import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class ActParticle extends CutsceneAction {

    @DataField(aliases = "p",throwable = true)
    public String particle;
    private Particle pt;
    @DataField(aliases = "c")
    public String color;
    private Color cl;

    @DataField(aliases = "ox")
    public float offsetX;
    @DataField(aliases = "oy")
    public float offsetY;
    @DataField(aliases = "oz")
    public float offsetZ;
    @DataField(aliases = "spd")
    public float speed;
    @DataField(aliases = "a")
    public int amount = 1;
    @DataField
    public float size;
    @DataField(aliases = "m")
    public String material;
    private Material mt;

    @DataField(aliases = "md")
    public byte materialData;

    @DataField(aliases = "r")
    public double range;


    private final EffectManager manager;
    public ActParticle(CutsceneManager pl) {
        super(pl);
        manager = pl.getEffectLib();
    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            pt = Particle.valueOf(particle.toUpperCase());
        } catch (Exception e) {
            CutsceneMaker.warn("The particle type \"" + particle + "\" doesn't exist!");
            pt = Particle.REDSTONE;
        }
        if (color != null) {
            try {
                cl = Color.fromRGB(Integer.parseInt(color, 16));
            } catch (Exception e) {
                CutsceneMaker.warn("Invalid color format: " + color);
            }
        }
        if (material != null) {
            try {
                mt = Material.valueOf(particle.toUpperCase());
            } catch (Exception e) {
                CutsceneMaker.warn("The material type \"" + material + "\" doesn't exist!");
            }
        }
    }

    @Override
    protected void apply(LivingEntity entity) {
        manager.display(
                pt,
                entity.getLocation(),
                offsetX,
                offsetY,
                offsetZ,
                speed,
                amount,
                size,
                cl,
                mt,
                materialData,
                range,
                null
        );
    }
}
