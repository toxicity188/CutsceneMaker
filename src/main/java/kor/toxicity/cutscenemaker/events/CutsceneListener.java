package kor.toxicity.cutscenemaker.events;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

public class CutsceneListener implements Listener {

    private static final Map<String,Class<? extends EntityEvent>> entity = new HashMap<>();
    private static final Map<String,Class<? extends PlayerEvent>> player = new HashMap<>();

    public CutsceneListener(CutsceneMaker pl) {
        Bukkit.getPluginManager().registerEvents(this,pl);
    }

    static {
        addPlayerEvent("join", PlayerJoinEvent.class);
        addPlayerEvent("command", PlayerCommandPreprocessEvent.class);
    }

    private final Map<Class<? extends Event>, ActionContainer> containers = new HashMap<>();

    @EventHandler
    public void Listen(EntityEvent e) {
        if (containers.containsKey(e.getClass()) && e.getEntity() instanceof LivingEntity && e.getEntity().isValid() && !e.getEntity().isDead()) {
            containers.get(e.getClass()).run((LivingEntity) e.getEntity());
        }
    }
    @EventHandler
    public void Listen(PlayerEvent e) {
        if (containers.containsKey(e.getClass()) && e.getPlayer().isValid() && e.getPlayer().isOnline() && !e.getPlayer().isDead()) {
            containers.get(e.getClass()).run(e.getPlayer());
        }
    }

    public static void addEntityEvent(String name, Class<? extends EntityEvent> event) {
        if (name == null || event == null) return;
        entity.put(name.toLowerCase(),event);
    }
    public static void addPlayerEvent(String name, Class<? extends PlayerEvent> event) {
        if (name == null || event == null) return;
        player.put(name.toLowerCase(),event);
    }

}
