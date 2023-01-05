package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class InventorySupplier {
    private final FunctionPrinter name;
    private final int row;
    private Map<Integer, ItemBuilder> builderMap;

    public InventorySupplier(ConfigurationSection section) {
        name = new FunctionPrinter(section.getString("Name","Unknown inventory"));
        row = Math.min(Math.max(section.getInt("Rows",5),1),6);

        if (section.isSet("Item") && section.isConfigurationSection("Item")) {
            builderMap = new HashMap<>();
            ConfigurationSection item = section.getConfigurationSection("Item");
            for (String key : item.getKeys(false)) {
                try {
                    builderMap.put(Integer.parseInt(key), InvUtil.getInstance().fromConfig(item,key));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public Inventory getInventory(Player player) {
        Inventory inv = InvUtil.getInstance().create(name.print(player),row);
        if (builderMap != null) builderMap.forEach((i,b) -> inv.setItem(i,b.get(player)));
        return inv;
    }
}
