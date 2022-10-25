package kor.toxicity.cutscenemaker.actions.mechanics;

import com.google.gson.JsonObject;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.CutsceneAction;
import kor.toxicity.cutscenemaker.actions.DataField;
import kor.toxicity.cutscenemaker.util.ItemUtil;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ActGiveItem extends CutsceneAction {

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
    public JsonObject tag;


    private ItemStack give;

    public ActGiveItem(CutsceneManager pl) {
        super(pl);
    }

    @Override
    public void initialize() {
        try {
            give = new ItemStack(Material.valueOf(type));
        } catch (Exception e) {
            give = new ItemStack(Material.IRON_SWORD);
        }
        give.setAmount(amount);
        give.setDurability((short) durability);


        ItemMeta meta = give.getItemMeta();
        meta.setDisplayName(a(name));
        if (lore != null) meta.setLore(Arrays.stream((lore.contains("//") ? lore.split("//") : new String[] {lore})).map(this::a).collect(Collectors.toList()));
        give.setItemMeta(meta);

        if (tag != null) {
            Map<String,String> tags = new HashMap<>();
            tag.entrySet().stream().filter(e -> e.getValue().getAsString() != null).forEach(e -> tags.put(e.getKey(),e.getValue().getAsString()));
            ItemUtil.setInternalTag(give,tags);
        }
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player) ((Player) entity).getInventory().addItem(give);
    }
    private String a(String t) {
        return TextParser.getInstance().colored(t);
    }
}
