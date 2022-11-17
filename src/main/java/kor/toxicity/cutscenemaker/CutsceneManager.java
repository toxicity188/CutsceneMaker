package kor.toxicity.cutscenemaker;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.actions.mechanics.ActSetSkriptVar;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class CutsceneManager {

    private final JavaPlugin plugin;
    private final CutsceneUser user;
    @Getter
    private EffectManager EffectLib = null;
    @Getter
    private final ProtocolManager ProtocolLib;

    @Getter
    private final Map<String, Location> locations = new HashMap<>();

    CutsceneManager(JavaPlugin plugin) {
        this.plugin = plugin;

        this.user = new CutsceneUser();
        Bukkit.getOnlinePlayers().forEach(this.user::load);
        EvtUtil.register(plugin,user);

        ProtocolLib = ProtocolLibrary.getProtocolManager();
        if (Bukkit.getPluginManager().isPluginEnabled("EffectLib")) EffectLib = new EffectManager(plugin);
        if (Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            ActionData.addAction("skript", ActSetSkriptVar.class);
        }
    }

    public BukkitTask runTaskTimer(Runnable task, long delay, long time) {
        return Bukkit.getScheduler().runTaskTimer(plugin,task,delay,time);
    }
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin,task,delay);
    }
    public BukkitTask runTaskLaterAsynchronously(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,task,delay);
    }
    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long time) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,task,delay,time);
    }

    public ListenerManager register(Listener... listener) {
        return new ListenerManager(plugin,listener);
    }

    public VarsContainer getVars(Player player) {
        return user.container.get(player);
    }
    public Vars getVars(Player player, String name) {
        return (getVars(player) != null) ? getVars(player).get(name) : null;
    }
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private class CutsceneUser implements Listener {

        private final Map<Player, VarsContainer> container = new HashMap<>();

        @EventHandler
        public void onJoin(PlayerJoinEvent e) {
            load(e.getPlayer());
        }
        private void load(Player player) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
                VarsContainer c = new VarsContainer(player);
                try {
                    c.load(plugin);
                } catch (Exception t) {
                    c.register(plugin);
                }
                c.autoSave(plugin, 300);
                container.put(player, c);
            });
        }
        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
                VarsContainer c = container.get(e.getPlayer());
                if (c != null) {
                    try {
                        c.save(plugin);
                    } catch (Exception t) {
                        throw new RuntimeException("An error has occurred.");
                    }
                    c.stop();
                    container.remove(e.getPlayer());
                }
            });
        }
    }
}