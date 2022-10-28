package kor.toxicity.cutscenemaker.handlers;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.util.DataObject;
import org.bukkit.EntityEffect;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActionListener implements Listener {

    private static final Map<Class<? extends Event>, Set<ActionHandler>> handlers = new HashMap<>();

    public static void addListener(ActionHandler listener, JsonObject j) {
        Class<? extends Event> c = listener.getEventClass();
        if (PlayerEvent.class.isAssignableFrom(c)||EntityEffect.class.isAssignableFrom(c)) {
            DataObject<ActionHandler> d = new DataObject<>(listener);
            d.apply(j);
            if (d.isLoaded()) {
                if (!handlers.containsKey(c)) handlers.put(c, new HashSet<>());
                handlers.get(c).add(listener);
            }
        }
    }

    @EventHandler
    public void entity(EntityEvent e) {

    }

    @EventHandler
    public void player(PlayerEvent e) {

    }

}
