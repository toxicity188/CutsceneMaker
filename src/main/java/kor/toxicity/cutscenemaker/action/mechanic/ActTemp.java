package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActTemp extends CutsceneAction {

    @DataField(aliases = "n",throwable = true)
    public String name;

    private ItemBuilder builder;

    public ActTemp(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        builder = ItemData.getItem(name);
        if (builder == null) CutsceneMaker.warn("The item named \"" + name + "\" doesn't exist!");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            CutsceneMaker.addTempItem(player,builder.get(player));
        }
    }
}
