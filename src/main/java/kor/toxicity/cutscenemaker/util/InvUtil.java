package kor.toxicity.cutscenemaker.util;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public class InvUtil {
    @Getter
    private static final InvUtil instance = new InvUtil();

    public boolean has(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).anyMatch(i -> i.isSimilar(target) && i.getAmount() >= target.getAmount());
    }
    public Optional<ItemStack> getSimilarItem(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).filter(i -> i.isSimilar(target) && i.getAmount() >= target.getAmount()).findFirst();
    }
    public int emptySpace(Player player) {
        Inventory inv = player.getInventory();
        int r = 0;
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) r ++;
        }
        return r;
    }
}
