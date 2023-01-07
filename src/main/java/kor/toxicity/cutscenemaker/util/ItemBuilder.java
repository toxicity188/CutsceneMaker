package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemBuilder {
    @Getter
    private final ItemStack item;
    private final Function<Player,ItemStack> function;
    @Getter
    private final boolean sameItem;

    public ItemBuilder(ItemStack item) {
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
    public ItemBuilder setAmount(int i) {
        ItemStack target = item.clone();
        target.setAmount(i);
        return new ItemBuilder(target);
    }

    public ItemStack get(Player player) {
        return function.apply(player);
    }

}
