package kor.toxicity.cutscenemaker.util.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface GuiExecutor {
    Player getPlayer();
    Inventory getInventory();
    long getDelay();

    void initialize();
    void onEnd();
    void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory);
}
