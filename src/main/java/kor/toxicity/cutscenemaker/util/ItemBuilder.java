package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class ItemBuilder implements ItemSupplier {
    @Getter
    private final ItemStack item;
    @EqualsAndHashCode.Exclude
    private final Function<Player,ItemStack> function;
    @Getter
    @EqualsAndHashCode.Exclude
    private final boolean sameItem;

    private static final ItemStack DEFAULT_ITEM = new ItemStack(Material.APPLE);
    static {
        ItemMeta meta = DEFAULT_ITEM.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "fail to load an item.");
        DEFAULT_ITEM.setItemMeta(meta);
    }

    public ItemBuilder(@NotNull ConfigurationSection section) {
        this(getFromConfig(Objects.requireNonNull(section)));
    }
    private static ItemStack getFromConfig(ConfigurationSection section) {
        ItemStack stack;
        try {
            stack = new ItemStack(Material.valueOf(section.getString("Type").toUpperCase()));
        } catch (Exception e) {
            return DEFAULT_ITEM;
        }
        stack.setAmount((short) section.getInt("Amount",1));
        stack.setDurability((short) section.getInt("Data",0));
        ItemMeta meta = stack.getItemMeta();
        if (section.isSet("Lore")) {
            try {
                meta.setLore(section.getStringList("Lore").stream().map(s -> ChatColor.WHITE + TextUtil.colored(s)).collect(Collectors.toList()));
            } catch (Exception ignored) {}
        }
        meta.setUnbreakable(section.getBoolean("Unbreakable",true));
        String name = section.getString("Display",null);
        if (name != null) meta.setDisplayName(ChatColor.WHITE + TextUtil.colored(name));
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemBuilder(@NotNull ItemStack item) {
        this.item = Objects.requireNonNull(item);
        ItemMeta meta = item.getItemMeta();

        List<BiConsumer<Player,ItemMeta>> metaList = new ArrayList<>();

        Optional.ofNullable(meta.getDisplayName()).ifPresent(d -> {
            FunctionPrinter printer = new FunctionPrinter(d);
            if (printer.ANY_MATCH) metaList.add((p,m) -> m.setDisplayName(printer.print(p)));
        });
        Optional.ofNullable(meta.getLore()).ifPresent(l -> {
            List<FunctionPrinter> printer = l.stream().map(FunctionPrinter::new).collect(Collectors.toList());
            if (printer.stream().anyMatch(q -> q.ANY_MATCH)) metaList.add((p,m) -> m.setLore(printer.stream().map(t -> t.print(p)).collect(Collectors.toList())));
        });
        Optional.ofNullable(meta.getLocalizedName()).ifPresent(d -> {
            FunctionPrinter printer = new FunctionPrinter(d);
            if (printer.ANY_MATCH) metaList.add((p,m) -> m.setLocalizedName(printer.print(p)));
        });
        sameItem = metaList.size() > 0;
        function = (sameItem) ? p -> {
            ItemStack i = item.clone();
            ItemMeta m = i.getItemMeta();
            for (BiConsumer<Player, ItemMeta> consumer : metaList) consumer.accept(p,m);
            i.setItemMeta(m);
            return i;
        } : e -> item;
    }
    public int getAmount() {
        return item.getAmount();
    }
    public ItemBuilder setAmount(int i) {
        ItemStack target = item.clone();
        target.setAmount(i);
        return new ItemBuilder(target);
    }

    public ItemStack get(Player player) {
        return function.apply(player);
    }

}
