package kor.toxicity.cutscenemaker;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketListener;
import de.slikey.effectlib.EffectManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CutsceneManager {

    private final JavaPlugin plugin;
    @Getter
    private EffectManager EffectLib = null;
    private ProtocolManager manager;

    CutsceneManager(JavaPlugin plugin) {
        this.plugin = plugin;
        manager = ProtocolLibrary.getProtocolManager();
        if (Bukkit.getPluginManager().isPluginEnabled("EffectLib")) EffectLib = new EffectManager(plugin);
    }

    public BukkitTask runTaskTimer(Runnable task, long delay, long time) {
        return Bukkit.getScheduler().runTaskTimer(plugin,task,delay,time);
    }

    public ProtocolManager getProtocolLib() {
        return manager;
    }

    public void register(PacketListener listener) {
        manager.addPacketListener(listener);
    }
}