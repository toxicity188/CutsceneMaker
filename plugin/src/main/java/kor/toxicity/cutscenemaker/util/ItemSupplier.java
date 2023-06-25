package kor.toxicity.cutscenemaker.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemSupplier {
    ItemStack get(Player player);
}
