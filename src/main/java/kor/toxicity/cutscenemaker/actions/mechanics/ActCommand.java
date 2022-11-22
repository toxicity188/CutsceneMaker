package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.util.DataField;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class ActCommand extends CutsceneAction {

    @DataField(aliases = "c", throwable = true)
    public String command;
    @DataField(aliases = "o")
    public boolean op;
    @DataField(aliases = "b")
    public boolean console;

    private Consumer<Player> consumer;

    public ActCommand(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (console) consumer = e -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);
        else {
            if (op) {
                consumer = e -> {
                    if (!e.isOp()) {
                        e.setOp(true);
                        e.performCommand(command);
                        e.setOp(false);
                    } else e.performCommand(command);
                };
            } else consumer = e -> e.performCommand(command);
        }
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) consumer.accept((Player) entity);
    }
}
