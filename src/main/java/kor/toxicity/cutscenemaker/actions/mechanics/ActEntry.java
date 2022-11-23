package kor.toxicity.cutscenemaker.actions.mechanics;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.material.WrappedMaterial;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.managers.ListenerManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ActEntry extends CutsceneAction {

    private static final Map<String, BiConsumer<Player,ActEntry>> entries = new HashMap<>();
    public static void addEntry(String s, BiConsumer<Player,ActEntry> entry) {
        entries.putIfAbsent(s,entry);
    }
    static {
        entries.put("chat",(p,t) -> {
            if (t.message != null) p.sendMessage(t.message);
            ListenerManager manager = t.manager.register();
            BukkitTask task = (t.wait > 0) ? t.manager.runTaskLaterAsynchronously(() -> {
                manager.unregister();
                if (t.fail != null) p.sendMessage(t.fail);
            },t.wait * 20L) : null;
            manager.add(new Listener() {
                @EventHandler
                public void chat(AsyncPlayerChatEvent e) {
                    if (e.getPlayer().equals(p)) {
                        e.setCancelled(true);
                        t.invoke(p,e.getMessage());
                        kill();
                    }
                }
                @EventHandler
                public void death(EntityDeathEvent e) {
                    if (e.getEntity() instanceof Player && e.getEntity().equals(p)) kill();
                }
                @EventHandler
                public void quit(PlayerQuitEvent e) {
                    if (e.getPlayer().equals(p)) kill();
                }

                private void kill() {
                    if (task != null) task.cancel();
                    manager.unregister();
                }
            });
        });
        entries.put("sign",(p,t) -> {
            ProtocolManager manager = t.manager.getProtocolLib();

            Location loc = p.getLocation();
            loc.setY(0);
            BlockPosition position = new BlockPosition(loc.toVector());

            PacketContainer update = manager.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            update.getBlockPositionModifier().write(0,position);

            PacketContainer block = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            block.getBlockPositionModifier().write(0,position);
            block.getBlockData().write(0,WrappedBlockData.createData(WrappedMaterial.getWrapper().getSign()));


            try {
                manager.sendServerPacket(p,block);
                if (t.message != null) p.sendSignChange(loc,new String[] {"", t.message,"",""});
                manager.sendServerPacket(p,update);
                ListenerManager listener = t.manager.register();
                BukkitTask task = (t.wait > 0) ? t.manager.runTaskLaterAsynchronously(() -> {
                    listener.unregister();
                    p.closeInventory();
                    if (t.fail != null) p.sendMessage(t.fail);
                },t.wait * 20L) : null;
                listener.register(e -> {
                    if (e.getPlayer().equals(p)) {
                        try {
                            block.getBlockData().write(0,WrappedBlockData.createData(loc.getBlock().getType()));
                            manager.sendServerPacket(p,block);
                        } catch (Exception ignored) {}
                        if (task != null) task.cancel();
                        t.invoke(p,e.getPacket().getStringArrays().read(0)[0]);
                        listener.unregister();
                    }
                },PacketType.Play.Client.UPDATE_SIGN);
            } catch (Exception e) {
                e.printStackTrace();
                CutsceneMaker.warn("unable to send packet.");
            }
        });
    }

    @DataField
    public String type = "chat";
    @DataField(aliases = "m")
    public String message;
    @DataField
    public String var;
    @DataField(throwable = true)
    public String callback;
    @DataField(aliases = {"w","wt"})
    public int wait = -1;
    @DataField
    public String fail;

    private final CutsceneManager manager;
    private BiConsumer<Player,ActEntry> apply;

    public ActEntry(CutsceneManager pl) {
        super(pl);
        manager = pl;
    }

    @Override
    public void initialize() {
        super.initialize();
        apply = entries.get(type);
        if (apply == null) {
            CutsceneMaker.warn("entry type \""+ type + "\" does not exists. changed chat type automatically");
            apply = entries.get("chat");
        }
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) apply.accept((Player) entity,this);
    }

    private void invoke(Player player, String value) {
        if (var != null) CutsceneMaker.getVars(player,var).setVar(value);
        ActionData.start(callback,player);
    }
}
