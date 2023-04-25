package kor.toxicity.cutscenemaker.handler.type;

import kor.toxicity.cutscenemaker.data.ItemData;
import kor.toxicity.cutscenemaker.handler.ActionHandler;
import kor.toxicity.cutscenemaker.handler.DelayedHandler;
import kor.toxicity.cutscenemaker.util.ActionContainer;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public class HandlerItemClick extends ActionHandler implements DelayedHandler {

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

    private final Map<Player,Long> time = new WeakHashMap<>();
    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.hasItem()) {
            Long d = time.put(e.getPlayer(), System.currentTimeMillis());
            if ((d == null ? - 200 : d) + 200 > System.currentTimeMillis()) return;
            ItemStack stack = e.getItem();
            ItemStack compare = builder.getItem();
            int a;
            if ((a = stack.getAmount()) >= required && compare.isSimilar(stack)) {
                if (apply(e.getPlayer()) && consume) stack.setAmount(Math.max(a - consumeAmount, 0));
                e.setCancelled(cancel);
            }
        }
    }

    @Override
    public Map<Player, Long> getTimeMap() {
        return time;
    }
}
