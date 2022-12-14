package kor.toxicity.cutscenemaker;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.slikey.effectlib.EffectManager;
import kor.toxicity.cutscenemaker.events.UserDataLoadEvent;
import kor.toxicity.cutscenemaker.util.DataContainer;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CutsceneManager {

    private final CutsceneMaker plugin;
    private final CutsceneUser user;
    @Getter
    private EffectManager EffectLib;
    @Getter
    private final ProtocolManager ProtocolLib;

    @Getter
    private final DataContainer<Location> locations = new DataContainer<>();

    private static final List<Player> delays = new ArrayList<>(1 << 8);
    private static Function<Player,Boolean> applyDelay;

    CutsceneManager(CutsceneMaker plugin) {
        this.plugin = plugin;

        this.user = new CutsceneUser();
        Bukkit.getOnlinePlayers().forEach(this.user::load);
        EvtUtil.register(plugin,user);

        applyDelay = p -> {
            if (!delays.contains(p)) {
                delays.add(p);
                runTaskLaterAsynchronously(() -> delays.remove(p), 4);
                return true;
            } else return false;
        };

        ProtocolLib = ProtocolLibrary.getProtocolManager();

        if (Bukkit.getPluginManager().isPluginEnabled("EffectLib")) EffectLib = new EffectManager(plugin);
    }
    public static boolean onDelay(Player player) {
        return applyDelay == null || applyDelay.apply(player);
    }

    public BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin,task);
    }
    public BukkitTask runTaskAsynchronously(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin,task);
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

    public void registerEvent(Listener listener) {
        EvtUtil.register(plugin,listener);
    }
    public ListenerManager register(Listener... listener) {
        return new ListenerManager(plugin,listener);
    }
    public void addLateCheck(Runnable runnable) {
        plugin.addLateCheck(runnable);
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

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onJoin(PlayerJoinEvent e) {
            Player player = e.getPlayer();
            if (CutsceneConfig.getInstance().isChangeGameMode()) {
                GameMode mode = CutsceneConfig.getInstance().getDefaultGameMode();
                if (!player.isOp() && player.getGameMode() != mode) {
                    player.setGameMode(mode);
                }
            }
            load(player);
        }
        private void load(Player player) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
                VarsContainer c = new VarsContainer(player);
                try {
                    c.load(plugin);
                } catch (Exception t) {
                    c.register(plugin);
                }
                c.autoSave(plugin, CutsceneConfig.getInstance().getAutoSaveTime());
                container.put(player, c);
                Bukkit.getScheduler().runTask(plugin,() -> EvtUtil.call(new UserDataLoadEvent(player)));
            });
        }
        @EventHandler(priority = EventPriority.MONITOR)
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