package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Navigator {
    private Navigator() {
        throw new RuntimeException();
    }
    static void startNavigate(Player player, Location location) {
        NavigateTarget t;
        if ((t = NAVIGATE_TARGET_MAP.get(player)) != null) {
            t.start(location);
            return;
        }
        NAVIGATE_TARGET_MAP.put(player,new NavigateTarget(player, location));
    }
    static void endNavigate(Player player) {
        NavigateTarget target = getNavigate(player);
        if (target != null) target.cancel();
    }

    private static MessageSender onStart, onEnd;

    static void register(JavaPlugin plugin) {
        EvtUtil.register(plugin, new Listener() {
            @EventHandler
            public void quit(PlayerQuitEvent e) {
                endNavigate(e.getPlayer());
            }
            @EventHandler
            public void kick(PlayerKickEvent e) {
                endNavigate(e.getPlayer());
            }
            @EventHandler
            public void swap(PlayerSwapHandItemsEvent e) {
                if (contains(e.getPlayer())) e.setCancelled(true);
            }
            @EventHandler
            public void click(InventoryClickEvent e) {
                if (e.getWhoClicked() instanceof Player) {
                    Player player = (Player) e.getWhoClicked();
                    if (e.getSlot() == 40 && contains(player)) {
                        endNavigate(player);
                    }
                }
            }
        });
    }
    static void reload() {
        NAVIGATE_TARGET_MAP.values().forEach(NavigateTarget::cancel);
        onStart = QuestData.QUEST_MESSAGE_MAP.get("quest-start-navigate");
        onEnd = QuestData.QUEST_MESSAGE_MAP.get("quest-end-navigate");
    }
    private static NavigateTarget getNavigate(Player player) {
        return NAVIGATE_TARGET_MAP.get(player);
    }
    private static boolean contains(Player player) {
        return NAVIGATE_TARGET_MAP.containsKey(player);
    }
    private static final Map<Player,NavigateTarget> NAVIGATE_TARGET_MAP = new ConcurrentHashMap<>();

    private static final ItemStack COMPASS = new ItemStack(Material.COMPASS);
    private static final ItemStack EMPTY = new ItemStack(Material.AIR);

    private static class NavigateTarget {
        private final Player player;
        private final ItemStack before;

        private NavigateTarget(Player player, Location location) {
            this.player = player;
            player.closeInventory();
            ItemStack hand = player.getInventory().getItemInOffHand();
            before = (hand != null) ? hand : EMPTY;
            player.getInventory().setItemInOffHand(COMPASS);
            start(location);
        }
        private void start(Location location) {
            player.setCompassTarget(location);
            if (onStart != null) onStart.send(player);
        }

        private void cancel() {
            player.closeInventory();
            player.getInventory().setItemInOffHand(before);
            if (onEnd != null) onEnd.send(player);
            NAVIGATE_TARGET_MAP.remove(player);
        }
    }
}
