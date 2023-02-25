package kor.toxicity.cutscenemaker.util;

import lombok.Data;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Data
public class StorageItem {
    private final @NotNull ItemStack stack;
    private final int year, month, day, left;
    private final boolean temp;
}
