package kor.toxicity.cutscenemaker.util.gui;

import org.bukkit.inventory.Inventory;

public abstract class SubAdapter extends GuiAdapter {

    private final GuiAdapter adapter;
    public SubAdapter(Inventory inventory, GuiAdapter adapter) {
        super(adapter.getPlayer(), adapter.getManager(), inventory);
        this.adapter = adapter;
    }

    protected boolean safeEnd = false;

    @Override
    public void onEnd() {
        if (!safeEnd) getManager().runTaskLater(adapter::open,5);
    }
}
