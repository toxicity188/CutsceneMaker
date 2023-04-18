package kor.toxicity.cutscenemaker.data;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.NBTReflector;
import kor.toxicity.cutscenemaker.util.gui.InventoryGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class ItemData extends CutsceneData implements Listener {

    public static final String ITEM_KEY = "cutscene.item.key";

    private static final Map<String, ItemBuilder> ITEM_MAP = new HashMap<>();
    private static final Map<String, InventoryGui> GUI_MAP = new HashMap<>();

    private boolean enableTagging;
    public ItemData(CutsceneMaker pl) {
        super(pl);
        EvtUtil.register(pl,this);
    }
    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (enableTagging) check(e.getPlayer());
    }
    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack current;
        if ((current = e.getCurrentItem()) != null) {
            ItemBuilder builder = ITEM_MAP.get(NBTReflector.readInternalTag(current,ITEM_KEY));
            if (builder != null) {
                ItemStack get = builder.get((Player) e.getWhoClicked());
                if (get.isSimilar(current)) return;
                get.setAmount(current.getAmount());
                e.setCancelled(true);
                e.getClickedInventory().setItem(e.getSlot(),get);
            }
        }
    }
    public void check(Player player) {
        int i = 0;
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv) {
            if (stack != null) {
                ItemBuilder builder = ITEM_MAP.get(NBTReflector.readInternalTag(stack, ITEM_KEY));
                if (builder != null) inv.setItem(i,builder.get(player));
            }
            i++;
        }
    }

    @Override
    public void reload() {
        ITEM_MAP.clear();
        GUI_MAP.clear();
        enableTagging = CutsceneConfig.getInstance().isEnableTagging();
        ConfigLoad load = getPlugin().read("Items");
        ConfigLoad inventory = getPlugin().read("Inventory");
        load.getAllFiles().forEach(s -> {
            ItemStack stack = load.getItemStack(s);
            if (stack != null) {
                ITEM_MAP.put(s,new ItemBuilder((enableTagging) ? NBTReflector.setInternalTag(stack,ITEM_KEY,s) : stack));
            }
        });
        inventory.getAllFiles().forEach(s -> {
            try {
                GUI_MAP.put(s,new InventoryGui(inventory.getConfigurationSection(s)));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage());
            }
        });
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(ITEM_MAP.size()) + " Items successfully loaded.");
        CutsceneMaker.send(ChatColor.GREEN + Integer.toString(GUI_MAP.size()) + " Inventories successfully loaded.");
        if (enableTagging) getPlugin().getManager().runTask(() -> Bukkit.getOnlinePlayers().forEach(this::check));
    }

    public static List<String> getItemKeys() {
        return new ArrayList<>(ITEM_MAP.keySet());
    }
    public static ItemBuilder getItem(String name) {
        return ITEM_MAP.get(name);
    }
    public static InventoryGui getGui(String name) {
        return GUI_MAP.get(name);
    }
    public static ItemStack getItem(Player player, String name) {
        return Optional.ofNullable(ITEM_MAP.get(name)).map(i -> i.get(player)).orElse(null);
    }
    public static boolean isSet(String name) {
        return ITEM_MAP.containsKey(name);
    }
}
