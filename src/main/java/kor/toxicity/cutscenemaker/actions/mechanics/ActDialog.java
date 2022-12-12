package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.quest.Quest;
import kor.toxicity.quest.tools.mechanics.Dialog;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActDialog extends CutsceneAction {

    @DataField(aliases = "n",throwable = true)
    public String name;
    @DataField(aliases = "s",throwable = true)
    public String sender;

    private Dialog dialog;

    public ActDialog(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        dialog = Quest.pl.getGlobalDialog(name);
        if (dialog == null) CutsceneMaker.warn("the dialog \"" + name + "\" doesn't exists.");
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (dialog != null && entity instanceof Player) {
            Player p = (Player) entity;
            if (dialog.isfinished(p)) dialog.start(p,sender);
        }
    }
}
