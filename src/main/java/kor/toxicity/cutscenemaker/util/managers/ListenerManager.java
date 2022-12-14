package kor.toxicity.cutscenemaker.util.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ListenerManager {
    private List<Listener> listener;
    private List<PacketListener> pkgListener;

    private final Consumer<Listener> register;
    private final Consumer<Listener> unregister;

    private final BiFunction<PacketType[], Consumer<PacketEvent>, PacketListener> packet;

    private ListenerManager(final JavaPlugin pl) {
        register = l -> EvtUtil.register(pl,l);
        unregister = EvtUtil::unregister;

        packet = (p,c) -> new PacketAdapter(pl,p) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                c.accept(event);
            }
            @Override
            public void onPacketSending(PacketEvent event) {
                c.accept(event);
            }
        };
    }
    public ListenerManager(final JavaPlugin pl, Listener... listener) {
        this(pl);
        this.listener = new ArrayList<>();
        if (listener != null) {
            Arrays.stream(listener).forEach(l -> {
                this.listener.add(l);
                register.accept(l);
            });
        }
    }

    public void unregister() {
        if (listener != null) {
            listener.forEach(unregister);
            listener.clear();
        }
        if (pkgListener != null) {
            pkgListener.forEach(l -> ProtocolLibrary.getProtocolManager().removePacketListener(l));
            pkgListener.clear();
        }
    }

    public void add(Listener l) {
        if (listener != null && !listener.contains(l)) {
            register.accept(l);
            listener.add(l);
        }

    }
    public void remove(Listener l) {
        if (listener != null && listener.contains(l)) {
            unregister.accept(l);
            listener.remove(l);
        }
    }
    public void remove(PacketListener l) {
        if (pkgListener != null && pkgListener.contains(l)) {
            ProtocolLibrary.getProtocolManager().removePacketListener(l);
            pkgListener.remove(l);
        }
    }

    public PacketListener register(Consumer<PacketEvent> action, PacketType... type) {
        if (pkgListener == null) pkgListener = new ArrayList<>();
        PacketListener l = packet.apply(type,action);
        ProtocolLibrary.getProtocolManager().addPacketListener(l);
        pkgListener.add(l);
        return l;
    }
}
