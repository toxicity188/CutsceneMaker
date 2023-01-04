package kor.toxicity.cutscenemaker.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvUtil {
    @Getter
    private static final InvUtil instance = new InvUtil();

    public String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (meta.getDisplayName() != null) ? meta.getDisplayName() : item.getType().toString().replace("_"," ").toLowerCase();
    }

    public Inventory create(String name, int rows) {
        return Bukkit.createInventory(null,Math.min(Math.max(1,rows),6)*9,name);
    }

    public void give(Player player, ItemStack... itemStack) {
        player.getInventory().addItem(itemStack);
    }
    public void take(Player player, ItemStack... itemStacks) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack itemStack : itemStacks) {
            int amount = itemStack.getAmount();
            for (ItemStack target : contents) {
                if (amount <= 0) break;
                if (target != null && itemStack.isSimilar(target)) {
                    int get = target.getAmount();
                    int minus = Math.min(amount, get);
                    target.setAmount(get - minus);
                    amount -= minus;
                }
            }
        }
    }

    public boolean has(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).anyMatch(i -> i != null && i.isSimilar(target) && i.getAmount() >= target.getAmount());
    }
    public Optional<ItemStack> getSimilarItem(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).filter(i -> i != null && i.isSimilar(target) && i.getAmount() >= target.getAmount()).findFirst();
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
