package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ItemData extends CutsceneData{

    private static ItemData instance;

    private final Map<String, ItemBuilder> items = new HashMap<>();

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
                items.put(s,new ItemBuilder((ItemStack) load.get(s)));
            } catch (Exception ignored) {}
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(items.size()) + " items successfully loaded.");
    }

    public static ItemBuilder getItem(String name) {
        return instance.items.get(name);
    }
    public static ItemStack getItem(Player player, String name) {
        return Optional.ofNullable(instance.items.get(name)).map(i -> i.get(player)).orElse(null);
    }
    public static boolean isSet(String name) {
        return instance.items.containsKey(name);
    }
}
