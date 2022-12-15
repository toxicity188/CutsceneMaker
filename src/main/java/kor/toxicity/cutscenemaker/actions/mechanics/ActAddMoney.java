package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.MoneyUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ActAddMoney extends CutsceneAction {

    @DataField(aliases = {"money","m"}, throwable = true)
    public double amount;

    public ActAddMoney(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) MoneyUtil.getInstance().addMoney((Player) entity, amount);
    }
}
