package kor.toxicity.cutscenemaker.util.gui;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.BlockPosition;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.Reloadable;
import kor.toxicity.cutscenemaker.material.WrappedMaterial;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CallbackManager implements Reloadable, Listener {
    private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    private static final Map<Player, CallbackData> CALLBACK_MAP = new ConcurrentHashMap<>();
    private final PacketListener listener;

    private final JavaPlugin pl;
    public CallbackManager(JavaPlugin pl) {
        this.pl = pl;
        EvtUtil.register(pl,this);
        listener = new PacketAdapter(pl,PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String[] args = event.getPacket().getStringArrays().read(0);
                CallbackData data = CALLBACK_MAP.get(event.getPlayer());
                if (data != null && data.type == CallBackType.SIGN) {
                    data.stringArray.accept(args);
                    CALLBACK_MAP.remove(event.getPlayer());
                }
            }
        };
        manager.addPacketListener(listener);
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        CallbackData data = CALLBACK_MAP.get(e.getPlayer());
        if (data != null && data.type == CallBackType.CHAT) {
            e.setCancelled(true);
            if (e.getMessage().equals("cancel")) {
                CutsceneMaker.send(e.getPlayer(),"task cancelled.");
            } else Bukkit.getScheduler().runTask(pl,() -> data.stringArray.accept(new String[] {e.getMessage()}));
            CALLBACK_MAP.remove(e.getPlayer());
        }
    }
    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            CallbackData data = CALLBACK_MAP.get(p);
            if (data != null && data.type == CallBackType.CHAT) CALLBACK_MAP.remove(p);
        }
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            CallbackData data = CALLBACK_MAP.get(p);
            if (data != null
                    && data.type == CallBackType.INVENTORY
                    && e.getCurrentItem() != null
                    && e.getCurrentItem().getType() == Material.BARRIER) e.setCancelled(true);
        }
    }
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            CallbackData data = CALLBACK_MAP.get(p);
            if (data != null && data.type == CallBackType.INVENTORY) {
                Map<Integer,ItemStack> stackMap = new WeakHashMap<>();
                ItemStack[] contents = e.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack stack = contents[i];
                    if (stack != null && stack.getType() != Material.AIR && stack.getType() != Material.BARRIER) stackMap.put(i,stack);
                }
                data.itemMap.accept(stackMap);
                CALLBACK_MAP.remove(p);
            }
        }
    }
    public static void callbackInventory(Player player, Inventory inventory, Consumer<Map<Integer,ItemStack>> callback) {
        player.openInventory(inventory);
        CALLBACK_MAP.put(player,new CallbackData(CallBackType.INVENTORY,null,callback));
    }
    public static void callbackChat(Player player, String[] args, Consumer<String[]> callback) {
        player.closeInventory();
        for (String arg : args) {
            CutsceneMaker.send(player,arg);
        }
        CALLBACK_MAP.put(player,new CallbackData(CallBackType.CHAT,callback,null));
    }
    public static void openSign(Player player, String[] args, Consumer<String[]> callback) {
        if (args.length != 4) return;
        Location location = player.getLocation();
        location.setY(0);

        BlockPosition position = new BlockPosition(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        PacketContainer block = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        block.getBlockData().write(0, WrappedMaterial.getSignData());
        block.getBlockPositionModifier().write(0,position);

        PacketContainer create = manager.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        create.getBlockPositionModifier().write(0,position);

        try {
            manager.sendServerPacket(player,block);
            player.sendSignChange(location,args);
            manager.sendServerPacket(player,create);
            CALLBACK_MAP.put(player,new CallbackData(CallBackType.SIGN,callback,null));
        } catch (Exception ignored) {}
    }
    @Override
    public void reload() {
        manager.removePacketListener(listener);
        CALLBACK_MAP.forEach((k,v) -> {
            if (v.type == CallBackType.SIGN) k.closeInventory();
        });
        CALLBACK_MAP.clear();
        manager.addPacketListener(listener);
    }

    @RequiredArgsConstructor
    private static class CallbackData {
        private final CallBackType type;
        private final Consumer<String[]> stringArray;
        private final Consumer<Map<Integer, ItemStack>> itemMap;
    }
    private enum CallBackType {
        SIGN,
        CHAT,
        INVENTORY,
    }
}
