package kor.toxicity.cutscenemaker.actions.mechanics;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.util.*;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ActItem extends CutsceneAction {

    @DataField(aliases = "n")
    public String name = "";
    @DataField
    public String type = "IRON_SWORD";
    @DataField(aliases = "a")
    public int amount = 1;
    @DataField(aliases = "dur")
    public int durability = 0;
    @DataField(aliases = "l")
    public String lore;
    @DataField
    public JsonObject tag;

    @DataField(aliases = "g")
    public boolean give;

    @DataField(aliases = "c")
    public String config;


    private Function<Player,ItemStack> apply;

    public ActItem(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (config != null) {
            apply = p -> ItemData.getItem(p,config);
        }
        if (apply == null) {
            ItemStack give;
            try {
                give = new ItemStack(Material.valueOf(type));
            } catch (Exception e) {
                give = new ItemStack(Material.IRON_SWORD);
            }
            give.setAmount(amount);
            give.setDurability((short) durability);


            ItemMeta meta = give.getItemMeta();
            meta.setDisplayName(a(name));
            if (lore != null)
                meta.setLore(Arrays.stream((lore.contains("//") ? lore.split("//") : new String[]{lore})).map(this::a).collect(Collectors.toList()));
            give.setItemMeta(meta);

            if (tag != null) {
                Map<String, String> tags = new HashMap<>();
                tag.entrySet().stream().filter(e -> e.getValue().getAsString() != null).forEach(e -> tags.put(e.getKey(), e.getValue().getAsString()));
                give = ItemUtil.setInternalTag(give, tags);
            }
            ItemBuilder builder = new ItemBuilder(give);
            apply = builder::get;
        }
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) {
            Player p = ((Player) entity);
            ItemStack item = apply.apply(p);
            if (give) p.getInventory().addItem(item);
            else {
                InvUtil.getInstance().getSimilarItem(p,item).ifPresent(i -> {
                    if (i.getAmount() > item.getAmount()) i.setAmount(i.getAmount() - item.getAmount());
                    else p.getInventory().remove(item);
                });
            }
        }
    }
    private String a(String t) {
        return TextUtil.getInstance().colored(t);
    }
}
