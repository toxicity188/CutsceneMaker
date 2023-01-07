package kor.toxicity.cutscenemaker.quests.enums;

import kor.toxicity.cutscenemaker.util.ItemBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
@RequiredArgsConstructor
public enum QuestGuiButton {
    PAGE_BEFORE(19),
    PAGE_AFTER(25),
    TYPE_SORT(22),
    ;
    private final int defaultSlot;
    private final StringBuilder builder = new StringBuilder();
    public static final ItemBuilder DEFAULT_ITEM_BUILDER;
    static {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "This is the default item!");
        item.setItemMeta(meta);
        DEFAULT_ITEM_BUILDER = new ItemBuilder(item);
    }
}
