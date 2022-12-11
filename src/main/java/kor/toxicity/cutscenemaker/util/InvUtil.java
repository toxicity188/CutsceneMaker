package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
    public int storage(@NotNull Player player, @Nullable ItemStack target) {
        if (target == null || target.getType() == Material.AIR) return emptySpace(player);
        Inventory inv = player.getInventory();
        int max = target.getMaxStackSize();
        return IntStream.range(0,36).map(i -> {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                if (item.getType() == Material.AIR) return max;
                else if (item.isSimilar(target)) return Math.max(max - item.getAmount(), 0);
                else return 0;
            } else return max;
        }).sum();
    }
}
