package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.commands.CommandListener;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class CutsceneAPI {

    @NotNull
    private final JavaPlugin pl;

    public void addAction(@NotNull String name,@NotNull Class<? extends CutsceneAction> action) {
        ActionData.addAction(name,action);
    }
    public void registerCommand(@NotNull CommandListener listener) {
        CutsceneCommand.register(pl,listener);
    }

    public void addEventHandler(@NotNull String string,@NotNull Class<? extends ActionHandler> clazz) {
        ActionHandler.addHandler(string,clazz);
    }

    public void runAction(@NotNull String name,@NotNull LivingEntity entity) {
        ActionData.start(name,entity);
    }

}
