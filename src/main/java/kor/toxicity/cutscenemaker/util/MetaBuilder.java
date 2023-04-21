package kor.toxicity.cutscenemaker.util;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MetaBuilder {
    private final ItemStack stack;
    private final ItemMeta meta;

    public MetaBuilder(ItemStack stack) {
        this.stack = stack;
        meta = stack.getItemMeta();
    }

    public MetaBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }
    public MetaBuilder setLore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }
    public MetaBuilder addLore(List<String> lore) {
        List<String> list = meta.getLore();
        if (list == null) list = new ArrayList<>();
        list.addAll(lore);
        meta.setLore(lore);
        return this;
    }
    public MetaBuilder setDisplayName(String displayName) {
        meta.setDisplayName(displayName);
        return this;
    }
    public MetaBuilder setLocalizedName(String localizedName) {
        meta.setLocalizedName(localizedName);
        return this;
    }
    public MetaBuilder addAllFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}
