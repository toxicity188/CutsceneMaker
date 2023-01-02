package kor.toxicity.cutscenemaker.util.gui;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@Getter
public abstract class GuiAdapter implements GuiExecutor {
    private final Player player;
    private final Inventory inventory;
    private long delay = 4;
    public GuiAdapter(Player player, Inventory inventory) {
        this.player = player;
        this.inventory = inventory;
    }
    public GuiAdapter(Player player, Inventory inventory, long delay) {
        this(player,inventory);
        this.delay = delay;
    }
    @Override
    public void initialize() {

    }
    @Override
    public void onEnd() {

    }
}
