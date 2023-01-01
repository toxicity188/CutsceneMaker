package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.CutsceneCommand;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class HandlerCommand extends ActionHandler {

    @DataField(aliases = {"cmd","c"}, throwable = true)
    public String command;

    public HandlerCommand(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        String[] vars = TextUtil.getInstance().split(command," ");
        if (vars.length == 1) {
            CutsceneCommand.createCommand(vars[0], (sender, command1, label, args) -> {
                if (sender instanceof LivingEntity) apply((LivingEntity) sender);
                return true;
            });
        } else {
            CutsceneCommand.createCommand(vars[0], (sender, command1, label, args) -> {
                if (sender instanceof LivingEntity) {
                    Map<String,String> localVars = new WeakHashMap<>();
                    for (int i = 1; i <= Math.min(args.length,vars.length -1); i++) {
                        localVars.put(vars[i],args[i -1]);
                    }
                    apply((LivingEntity) sender,localVars);
                }
                return true;
            });
        }
    }
}
