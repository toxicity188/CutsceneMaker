package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class InventorySupplier {
    private final FunctionPrinter name;
    @Getter
    private final int row;
    private final Map<Integer, ItemBuilder> builderMap;

    @Getter
    private final boolean sameInventory;

    public InventorySupplier(ConfigurationSection section) {
        this(new FunctionPrinter(section.getString("Name","Unknown inventory")),Math.min(Math.max(section.getInt("Rows",5),1),6),getItemMap(section));
    }
    private static Map<Integer,ItemBuilder> getItemMap(ConfigurationSection section) {
        if (section.isSet("Item") && section.isConfigurationSection("Item")) {
            Map<Integer, ItemBuilder> builderMap = new HashMap<>();
            ConfigurationSection item = section.getConfigurationSection("Item");
            for (String key : item.getKeys(false)) {
                try {
                    builderMap.put(Integer.parseInt(key), InvUtil.fromConfig(item,key));
                } catch (Exception ignored) {
                }
            }
            return builderMap;
        }
        return null;
    }
    public InventorySupplier(FunctionPrinter name, int row, Map<Integer,ItemBuilder> builderMap) {
        this.name = name;
        this.row = row;
        this.builderMap = builderMap;
        sameInventory = !name.anyMatch() && (builderMap == null || builderMap.values().stream().allMatch(ItemBuilder::isSameItem));
    }
    public Inventory getInventory() {
        return (sameInventory) ? getInventory(null) : null;
    }
    public Inventory getInventory(Player player) {
        Inventory inv = InvUtil.create(name.print(player),row);
        if (builderMap != null) builderMap.forEach((i,b) -> inv.setItem(i,b.get(player)));
        return inv;
    }
}
