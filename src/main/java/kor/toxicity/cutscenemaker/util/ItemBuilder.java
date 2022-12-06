package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.util.functions.MethodInterpreter;
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
import java.util.stream.Stream;

public class ItemBuilder {

    private final Function<Player,ItemStack> function;

    public ItemBuilder(ItemStack item) {
        Objects.requireNonNull(item);
        ItemMeta meta = item.getItemMeta();

        List<BiConsumer<Player,ItemMeta>> metaList = new ArrayList<>();

        Optional.ofNullable(meta.getDisplayName()).ifPresent(d -> metaList.add((p,m) -> {
            MethodInterpreter printer = new MethodInterpreter(d);
            if (printer.ANY_MATCH) m.setDisplayName(printer.print(p));
        }));
        Optional.ofNullable(meta.getLore()).ifPresent(l -> metaList.add((p,m) -> {
            Stream<MethodInterpreter> printer = l.stream().map(MethodInterpreter::new);
            if (printer.anyMatch(q -> q.ANY_MATCH)) m.setLore(printer.map(t -> t.print(p)).collect(Collectors.toList()));
        }));
        Optional.ofNullable(meta.getLocalizedName()).ifPresent(d -> metaList.add((p,m) -> {
            MethodInterpreter printer = new MethodInterpreter(d);
            if (printer.ANY_MATCH) m.setLocalizedName(printer.print(p));
        }));

        function = (metaList.size() > 0) ? p -> {
            ItemStack i = item.clone();
            ItemMeta m = i.getItemMeta();
            for (BiConsumer<Player, ItemMeta> consumer : metaList) consumer.accept(p,m);
            i.setItemMeta(m);
            return i;
        } : e -> item;
    }

    public ItemStack get(Player player) {
        return function.apply(player);
    }

}
