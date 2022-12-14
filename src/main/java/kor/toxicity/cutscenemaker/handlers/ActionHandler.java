package kor.toxicity.cutscenemaker.handlers;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.events.ActionStartEvent;
import kor.toxicity.cutscenemaker.handlers.types.*;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.reflect.DataObject;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public abstract class ActionHandler implements Listener {

    private static final Map<String,Class<? extends ActionHandler>> handlers = new HashMap<>();
    static {
        handlers.put("chat", HandlerChat.class);
        handlers.put("blockbreak", HandlerBlockBreak.class);
        handlers.put("blockclick", HandlerBlockClick.class);
        handlers.put("command", HandlerCommand.class);
        handlers.put("entityclick", HandlerEntityClick.class);
        handlers.put("kill", HandlerKill.class);
        handlers.put("walk",HandlerWalk.class);
        handlers.put("join", HandlerJoin.class);
        handlers.put("respawn", HandlerRespawn.class);
        handlers.put("custom", HandlerCustom.class);
        handlers.put("worldchange", HandlerChangeWorld.class);
        if (Bukkit.getPluginManager().isPluginEnabled("Quest")) {
            handlers.put("dialogcomplete", HandlerDialogComplete.class);
        }
    }

    private final ActionContainer container;
    protected final void apply(final LivingEntity entity) {
        apply(entity,null);
    }
    protected final void apply(final LivingEntity entity, Map<String,String> localVariables) {
        ActionStartEvent event = new ActionStartEvent(entity,container);
        EvtUtil.call(event);
        if (!event.isCancelled()) container.run(entity,localVariables);
    }

    protected abstract void initialize();

    public static void addHandler(String s,Class<? extends ActionHandler> c) {
        handlers.putIfAbsent(s,c);
    }

    public static ActionHandler addListener(ActionContainer container, String name, JsonObject obj, JavaPlugin plugin) {
        assert container != null && name != null && plugin != null;
        String nameCase = name.toLowerCase();
        if (handlers.containsKey(nameCase)) {
            try {
                ActionHandler handler = handlers.get(nameCase).getDeclaredConstructor(ActionContainer.class).newInstance(container);
                DataObject data = new DataObject(handler,"Event " + nameCase);
                data.apply(obj);
                if (data.isLoaded()) {
                    handler.initialize();
                    EvtUtil.register(plugin,handler);
                    return handler;
                }
            } catch (Exception ignored) {}
        } else CutsceneMaker.warn("unable to find the event name \"" + name + "\"");
        return null;
    }
    public final void unregister() {
        HandlerList.unregisterAll(this);
    }
}
