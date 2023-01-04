package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.CutsceneData;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class GuiRegister extends CutsceneData {

    private static final List<Player> PLAYER_LIST = new ArrayList<>(1 << 8);
    private static GuiRegister instance;
    public GuiRegister(CutsceneMaker pl) {
        super(pl);
        instance = this;
    }

    @Override
    public void reload() {
        new ArrayList<>(PLAYER_LIST).forEach(Player::closeInventory);
    }

    public static void registerNewGui(GuiExecutor executor) {
        Player p = executor.getPlayer();
        if (p == null || !p.isValid() || p.isDead()) return;
        PLAYER_LIST.add(p);
        p.openInventory(executor.getInventory());
        executor.initialize();
        EvtUtil.register(instance.getPlugin(), new Listener() {
            private BukkitTask delay;
            @EventHandler
            public void onClick(InventoryClickEvent e) {
                if (p.equals(e.getWhoClicked())) {
                    e.setCancelled(true);
                    if (e.getClickedInventory() != null && e.getCurrentItem() != null && delay == null) {
                        delay = instance.getPlugin().getManager().runTaskLaterAsynchronously(() -> delay = null, executor.getDelay());
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
            @EventHandler
            public void onEnd(InventoryCloseEvent e) {
                if (p.equals(e.getPlayer())) end();
            }
            @EventHandler
            public void onDeath(PlayerDeathEvent e) {
                if (p.equals(e.getEntity())) unregister();
            }
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                if (p.equals(e.getPlayer())) unregister();
            }
            private void unregister() {
                PLAYER_LIST.remove(p);
                EvtUtil.unregister(this);
            }
            private void end() {
                unregister();
                executor.onEnd();
            }
        });
    }
}
