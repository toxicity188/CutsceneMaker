package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ActSound extends RepeatableAction {

    @DataField(aliases = "s", throwable = true)
    public FunctionPrinter sound;
    @DataField(aliases = "v")
    public float volume = 1.0F;
    @DataField(aliases = "vs")
    public float volumeSpread = 0;

    @DataField(aliases = "p")
    public float pitch = 1.0F;
    @DataField(aliases = "ps")
    public float pitchSpread;

    @DataField(aliases = "g")
    public boolean global = false;

    @DataField(aliases = "c")
    public String category;


    private SoundPlay play;

    public ActSound(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        play = new SoundPlay();
    }

    @Override
    protected void initialize(LivingEntity entity) {
    }

    @Override
    protected void update(LivingEntity entity) {
        play.action(entity);
    }

    @Override
    protected void end(LivingEntity end) {

    }

    private class SoundPlay {
        private float v = volume;
        private float p = pitch;

        private final Random rand = ThreadLocalRandom.current();

        private SoundPlay() {
            SoundCategory cate = tryCategory();
            if (cate != null) {
                send = (global) ? e -> e.getWorld().playSound(e.getLocation(), sound.print(e), cate, v, p) : e -> {
                    if (e instanceof Player) ((Player) e).playSound(e.getLocation(), sound.print(e), cate, v, p);
                };
            } else {
                send = (global) ? e -> e.getWorld().playSound(e.getLocation(), sound.print(e), v, p) : e -> {
                    if (e instanceof Player) ((Player) e).playSound(e.getLocation(), sound.print(e), v, p);
                };
            }
        }
        private SoundCategory tryCategory() {
            if (category == null) return null;
            try {
                return SoundCategory.valueOf(category.toUpperCase());
            } catch (Exception e) {
                CutsceneMaker.warn("The SoundCategory named \"" + category + "\" doesn't exists!");
                return null;
            }
        }
        private final Consumer<LivingEntity> send;

        private void action(LivingEntity entity) {
            if (volumeSpread != 0) v = 2F*(rand.nextFloat()-0.5F)*volumeSpread+volume;
            if (pitchSpread != 0) p = 2F*(rand.nextFloat()-0.5F)*pitchSpread+pitch;
            send.accept(entity);
        }
    }
}
