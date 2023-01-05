package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.gui.InventoryGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ItemData extends CutsceneData{

    private static ItemData instance;

    private final Map<String, ItemBuilder> items = new HashMap<>();
    private final Map<String, InventoryGui> guiMap = new HashMap<>();

    public ItemData(CutsceneMaker pl) {
        super(pl);
        instance = this;
    }

    @Override
    public void reload() {
        items.clear();
        guiMap.clear();
        ConfigLoad load = getPlugin().read("Items");
        ConfigLoad inventory = getPlugin().read("Inventory");
        load.getAllFiles().forEach(s -> {
            try {
                items.put(s,new ItemBuilder((ItemStack) load.get(s)));
            } catch (Exception ignored) {}
        });
        inventory.getAllFiles().forEach(s -> {
            try {
                guiMap.put(s,new InventoryGui(inventory.getConfigurationSection(s)));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage());
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(items.size()) + " Items successfully loaded.");
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(guiMap.size()) + " Inventories successfully loaded.");
    }

    public static ItemBuilder getItem(String name) {
        return instance.items.get(name);
    }
    public static InventoryGui getGui(String name) {
        return instance.guiMap.get(name);
    }
    public static ItemStack getItem(Player player, String name) {
        return Optional.ofNullable(instance.items.get(name)).map(i -> i.get(player)).orElse(null);
    }
    public static boolean isSet(String name) {
        return instance.items.containsKey(name);
    }
}
