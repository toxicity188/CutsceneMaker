package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.CutsceneAction;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.quests.enums.QuestAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActQuest extends CutsceneAction {
    @DataField(throwable = true, aliases = "n")
    public String name;
    @DataField(throwable = true)
    public String type;

    private QuestAction task;

    public ActQuest(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            task = QuestAction.valueOf(type.toUpperCase());
        } catch (Exception e) {
            task = QuestAction.GIVE;
        }
    }

    @Override
    protected void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            if (!QuestData.applyQuest((Player) entity,name,task)) {
                CutsceneMaker.warn("The QuestSet named \"" + name +"\" doesn't exist!");
            }
        }
    }
}
