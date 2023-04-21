package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.gui.InventoryGui;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActOpen extends CutsceneAction {
    @DataField(aliases = "n", throwable = true)
    public String name;

    private InventoryGui gui;

    public ActOpen(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        gui = ItemData.getGui(name);
        if (gui == null) CutsceneMaker.warn("The Inventory named \"" + name + "\" doesn't exist!");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (gui != null && entity instanceof Player) gui.open((Player) entity,manager);
    }
}
