package kor.toxicity.cutscenemaker.util;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@Getter
public class TitleWrapper implements InventoryHolder {
    private final String title;
    private final int rows;
    private final Inventory inventory;

    public TitleWrapper(String title, int rows) {
        this.title = title;
        this.rows = rows;
        inventory = Bukkit.createInventory(this,rows,title);
    }
}
