package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class GuiRegister extends CutsceneData implements Listener {

    public GuiRegister(CutsceneMaker pl) {
        super(pl);
        EvtUtil.register(pl,this);
    }

    @Override
    public void reload() {
        new WeakHashMap<>(EXECUTOR_MAP).keySet().forEach(Player::closeInventory);
        CLICK_DELAY.values().forEach(BukkitTask::cancel);
        EXECUTOR_MAP.clear();
        CLICK_DELAY.clear();
    }
    private static final Map<Player,GuiExecutor> EXECUTOR_MAP = new HashMap<>();
    private static final Map<Player,BukkitTask> CLICK_DELAY = new HashMap<>();

    public static void registerNewGui(GuiExecutor executor) {
        Player p = executor.getPlayer();
        if (p == null || !p.isValid() || p.isDead()) return;
        p.openInventory(executor.getInventory());
        EXECUTOR_MAP.put(p,executor);
        executor.initialize();
    }
    private static GuiExecutor getExecutor(Player player) {
        return EXECUTOR_MAP.get(player);
    }
    public static void unregister(GuiExecutor executor) {
        EXECUTOR_MAP.remove(executor.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent e) {
        HumanEntity entity = e.getWhoClicked();
        if (entity instanceof Player) {
            Player p = (Player) entity;
            GuiExecutor executor = getExecutor(p);
            if (executor != null) {
                e.setCancelled(true);
                if (e.getClickedInventory() != null && e.getCurrentItem() != null && !CLICK_DELAY.containsKey(p)) {
                    CLICK_DELAY.put(p,getPlugin().getManager().runTaskLaterAsynchronously(() -> CLICK_DELAY.remove(p), executor.getDelay()));
                    MouseButton button;
                    if (e.isLeftClick()) {
                        if (e.isShiftClick()) button = MouseButton.LEFT;
                        else button = MouseButton.LEFT_WITH_SHIFT;
                    } else if (e.isRightClick()) {
                        if (e.isShiftClick()) button = MouseButton.RIGHT;
                        else button = MouseButton.RIGHT_WITH_SHIFT;
                    } else {
                        button = MouseButton.NUMBER_KEY;
                    }
                    executor.onClick(e.getCurrentItem(),e.getSlot(),button, e.getClickedInventory().equals(p.getInventory()));
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            GuiExecutor executor = getExecutor(p);
            if (executor != null) end(p, executor);
        }
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        unregister(e.getEntity());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        unregister(e.getPlayer());
    }
    private void unregister(Player p) {
        EXECUTOR_MAP.remove(p);
    }
    private void end(Player player, GuiExecutor executor) {
        unregister(player);
        executor.onEnd();
    }
}
