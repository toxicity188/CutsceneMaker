package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.commands.CommandListener;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class CutsceneAPI {

    private final JavaPlugin pl;

    public void addAction(String name, Class<? extends CutsceneAction> action) {
        ActionData.addAction(name,action);
    }
    public void registerCommand(CommandListener listener) {
        CutsceneCommand.register(pl,listener);
    }

    public void addEventHandler(String string, Class<? extends ActionHandler> clazz) {
        ActionHandler.addHandler(string,clazz);
    }

}
