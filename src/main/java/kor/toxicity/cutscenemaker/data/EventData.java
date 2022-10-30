package kor.toxicity.cutscenemaker.data;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;

import java.util.HashSet;
import java.util.Set;

public class EventData extends CutsceneData{

    private final Set<ActionHandler> handlers = new HashSet<>();
    private static EventData instance;

    public EventData(CutsceneMaker pl) {
        super(pl);
        instance = this;
    }

    @Override
    public void reload() {
        handlers.forEach(ActionHandler::unregister);
        handlers.clear();
    }

    static void addListener(ActionContainer container, String name, JsonObject obj) {
        ActionHandler handler = ActionHandler.addListener(container,name,obj,instance.getPlugin());
        if (handler != null) instance.handlers.add(handler);
    }
}
