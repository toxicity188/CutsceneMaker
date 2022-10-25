package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.commands.CommandListener;
import kor.toxicity.cutscenemaker.data.ActionData;
import org.bukkit.plugin.java.JavaPlugin;

public final class CutsceneAPI {

    private final JavaPlugin pl;

    public CutsceneAPI(JavaPlugin pl) {
        this.pl = pl;
    }

    public void addAction(String name, Class<? extends CutsceneAction> action) {
        ActionData.addAction(name,action);
    }
    public void registerCommand(CommandListener listener) {
        CutsceneCommand.register(pl,listener);
    }

}
