package kor.toxicity.cutscenemaker.util;

import lombok.EqualsAndHashCode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@EqualsAndHashCode
public class StorageItem {
    private final @NotNull ItemStack stack;
    private final @NotNull LocalDateTime time;
    private final int leftHour;

    public StorageItem(@NotNull ItemStack stack,@NotNull LocalDateTime time, int leftHour) {
        this.stack = stack;
        this.time = time;
        this.leftHour = leftHour;
    }
    public int getLeftHour() {
        return leftHour;
    }

    @NotNull
    public ItemStack getStack() {
        return stack;
    }
    @NotNull
    public LocalDateTime getTime() {
        return time;
    }
}
