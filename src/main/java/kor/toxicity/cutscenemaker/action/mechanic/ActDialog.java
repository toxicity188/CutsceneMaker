package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.quest.Quest;
import kor.toxicity.quest.tools.mechanics.Dialog;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class ActDialog extends CutsceneAction {

    @DataField(aliases = "n",throwable = true)
    public String name;
    @DataField(aliases = "s",throwable = true)
    public String sender;

    @DataField(aliases = "o")
    public boolean other = false;

    private Consumer<Player> consumer;

    public ActDialog(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (other) {
            Dialog dialog = Quest.pl.getGlobalDialog(name);
            if (dialog == null) CutsceneMaker.warn("The Dialog named \"" + name + "\" doesn't exists!");
            else consumer = p -> {
                if (dialog.isfinished(p)) dialog.start(p,sender);
            };
        } else {
            consumer = p -> QuestData.run(name,p,sender);
        }
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (consumer != null && entity instanceof Player) {
            consumer.accept((Player) entity);
        }
    }
}
