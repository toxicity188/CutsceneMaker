package kor.toxicity.cutscenemaker.data;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.handler.DelayedHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

public final class EventData extends CutsceneData implements Listener {

    private final Set<ActionHandler> handlers = new HashSet<>();
    private static EventData instance;

    public EventData(CutsceneMaker pl) {
        super(pl);
        EvtUtil.register(pl,this);
        instance = this;
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        handlers.forEach(h -> {
            if (h instanceof DelayedHandler) ((DelayedHandler) h).getTimeMap().remove(e.getPlayer());
        });
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
