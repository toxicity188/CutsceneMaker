package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Navigator {
    private Navigator() {
        throw new RuntimeException();
    }
    public static void startNavigate(Player player, String name, Location location) {
        player.closeInventory();
        NavigateTarget t;
        if (!Objects.equals(player.getWorld(),location.getWorld())) {
            if (onAnotherWorld != null) onAnotherWorld.send(player,name);
            return;
        }
        if ((t = NAVIGATE_TARGET_MAP.get(player)) != null) {
            t.start(location);
            return;
        }
        NAVIGATE_TARGET_MAP.put(player,new NavigateTarget(player, name, location));
    }
    public static void endNavigate(Player player) {
        NavigateTarget target = NAVIGATE_TARGET_MAP.remove(player);
        if (target != null) target.cancel();
    }

    private static MessageSender onStart, onEnd, onFinish, onAnotherWorld;

    static void register(JavaPlugin plugin) {
        EvtUtil.register(plugin, new Listener() {
            @EventHandler
            public void quit(PlayerQuitEvent e) {
                endNavigate(e.getPlayer());
            }
            @EventHandler
            public void worldChange(PlayerChangedWorldEvent e) {
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
                        e.setCancelled(true);
                        endNavigate(player);
                    }
                }
            }
            @EventHandler
            public void walk(PlayerMoveEvent e) {
                Player player = e.getPlayer();
                NavigateTarget target;
                if ((target = NAVIGATE_TARGET_MAP.get(player)) != null) {
                    Location location = player.getLocation();
                    if (Objects.equals(location.getWorld(),target.location.getWorld()) && location.distance(target.location) < 3) {
                        player.getInventory().setItemInOffHand(target.before);
                        NAVIGATE_TARGET_MAP.remove(player);
                        if (onFinish != null) onFinish.send(player,target.name);
                    }
                }
            }
        });
    }
    static void reload() {
        NAVIGATE_TARGET_MAP.values().forEach(NavigateTarget::cancel);
        NAVIGATE_TARGET_MAP.clear();
        onStart = QuestData.QUEST_MESSAGE_MAP.get("quest-start-navigate");
        onFinish = QuestData.QUEST_MESSAGE_MAP.get("quest-finish-navigate");
        onEnd = QuestData.QUEST_MESSAGE_MAP.get("quest-end-navigate");
        onAnotherWorld = QuestData.QUEST_MESSAGE_MAP.get("quest-another-world");
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
        private final String name;
        private final Location location;

        private NavigateTarget(Player player, String name, Location location) {
            this.player = player;
            this.name = name;
            this.location = location;
            ItemStack hand = player.getInventory().getItemInOffHand();
            before = (hand != null) ? hand : EMPTY;
            player.getInventory().setItemInOffHand(COMPASS);
            start(location);
        }
        private void start(Location location) {
            player.setCompassTarget(location);
            if (onStart != null) onStart.send(player,name);
        }

        private void cancel() {
            player.getInventory().setItemInOffHand(before);
            player.closeInventory();
            if (onEnd != null) onEnd.send(player,name);
        }
    }
}
