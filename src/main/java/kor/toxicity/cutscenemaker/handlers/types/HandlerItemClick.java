package kor.toxicity.cutscenemaker.handlers.types;

import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.handlers.ActionHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HandlerItemClick extends ActionHandler {

    @DataField(aliases = "i",throwable = true)
    public String item;

    private ItemBuilder builder;

    @DataField(aliases = {"r","req"})
    public int required = 1;

    @DataField(aliases = "c")
    public boolean cancel = true;
    @DataField(aliases = {"u","use"})
    public boolean consume = false;
    @DataField(aliases = "ca")
    public int consumeAmount = -1;

    public HandlerItemClick(ActionContainer container) {
        super(container);
    }

    @Override
    protected void initialize() {
        builder = ItemData.getItem(item);
        if (required < 1) required = 1;
        if (consumeAmount < 0) consumeAmount = required;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.hasItem()) {
            ItemStack stack = e.getItem();
            ItemStack compare = builder.getItem();
            int a;
            if ((a = stack.getAmount()) >= required && compare.isSimilar(stack)) {
                apply(e.getPlayer());
                if (consume) stack.setAmount(Math.max(a - consumeAmount, 0));
            }
            e.setCancelled(cancel);
        }
    }
}
