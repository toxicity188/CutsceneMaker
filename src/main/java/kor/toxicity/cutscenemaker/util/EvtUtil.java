package kor.toxicity.cutscenemaker.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class EvtUtil {

    public static void register(JavaPlugin plugin, Listener event) {
        Bukkit.getPluginManager().registerEvents(event,plugin);
    }
    public static void unregister(Listener event) {
        HandlerList.unregisterAll(event);
    }
    public static void call(Event evt) {
        Bukkit.getPluginManager().callEvent(evt);
    }
}
