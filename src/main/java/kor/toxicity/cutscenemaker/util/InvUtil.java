package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.ItemData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InvUtil {
    private static final Pattern SIMPLE_ITEM_PATTERN = Pattern.compile("\\?(?<type>(\\w|_)+) (?<data>[0-9]+) (?<name>(\\w|\\W)+)", Pattern.UNICODE_CHARACTER_CLASS);

    public static String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (meta.getDisplayName() != null) ? meta.getDisplayName() : item.getType().toString().replace("_"," ").toLowerCase();
    }

    public static Inventory create(String name, int rows) {
        return Bukkit.createInventory(null,Math.min(Math.max(1,rows),6)*9,name);
    }

    public static void give(Player player, ItemStack... itemStack) {
        player.getInventory().addItem(itemStack);
    }
    public static void take(Player player, ItemStack... itemStacks) {
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
    public static ItemBuilder fromConfig(ConfigurationSection c, String s) {
        if (c.isItemStack(s)) return new ItemBuilder(c.getItemStack(s));
        else if (c.isConfigurationSection(s)) return new ItemBuilder(c.getConfigurationSection(s));
        else if (c.isString(s)) {
            Matcher matcher = SIMPLE_ITEM_PATTERN.matcher(c.getString(s));
            if (matcher.find()) {
                Material material;
                try {
                    material = Material.valueOf(matcher.group("type").toUpperCase());
                } catch (Exception e) {
                    material = Material.APPLE;
                }
                ItemStack itemStack = new ItemStack(material);
                itemStack.setDurability(Short.parseShort(matcher.group("data")));
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + TextUtil.colored(matcher.group("name")));
                itemStack.setItemMeta(meta);
                return new ItemBuilder(itemStack);
            } else {
                return toName(c.getString(s));
            }
        } else return null;
    }
    public static ItemBuilder toName(String s) {
        ItemBuilder builder = ItemData.getItem(s);
        if (builder == null) CutsceneMaker.warn("The item \"" + s + "\" doesn't exist!");
        return builder;
    }

    public static boolean has(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).anyMatch(i -> i != null && i.isSimilar(target) && i.getAmount() >= target.getAmount());
    }
    public static Optional<ItemStack> getSimilarItem(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).filter(i -> i != null && i.isSimilar(target) && i.getAmount() >= target.getAmount()).findFirst();
    }
    public static int getTotalAmount(Player player, ItemStack target) {
        return Arrays.stream(player.getInventory().getContents()).filter(target::isSimilar).mapToInt(ItemStack::getAmount).sum();
    }
    public static int emptySpace(Player player) {
        Inventory inv = player.getInventory();
        int r = 0;
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) r ++;
        }
        return r;
    }
    public static int storage(@NotNull Player player, @Nullable ItemStack target) {
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
