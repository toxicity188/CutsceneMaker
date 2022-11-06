package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ItemData extends CutsceneData{

    private static ItemData instance;

    private final Map<String, ItemStack> items = new HashMap<>();

    public ItemData(CutsceneMaker pl) {
        super(pl);
        instance = this;
    }

    @Override
    public void reload() {
        items.clear();
        ConfigLoad load = getPlugin().read("Items");
        load.getAllFiles().forEach(s -> {
            try {
                items.put(s,(ItemStack) load.get(s));
            } catch (Exception ignored) {}
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(items.size()) + " items successfully loaded.");
    }

    public static ItemStack getItem(String name) {
        return instance.items.get(name);
    }
}
