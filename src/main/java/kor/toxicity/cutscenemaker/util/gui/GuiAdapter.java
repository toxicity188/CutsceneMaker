package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

@Getter
public abstract class GuiAdapter implements GuiExecutor {
    private final Player player;
    private final CutsceneManager manager;
    private final Inventory inventory;
    private final long delay;

    public GuiAdapter(Player player, CutsceneManager manager, Inventory inventory) {
        this(player,manager,inventory,4);
    }
    public GuiAdapter(Player player, CutsceneManager manager,  Inventory inventory, long delay) {
        this.player = player;
        this.inventory = inventory;
        this.delay = delay;
        this.manager = manager;
    }
    @Override
    public void initialize() {

    }
    @Override
    public void onEnd() {

    }

    protected void open() {
        GuiRegister.registerNewGui(this);
    }
    protected void callbackSign(String[] message, Consumer<String> callback) {
        if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = true;
        CallbackManager.openSign(player,message,e -> {
            String s = e[0];
            if (s.equals("")) {
                CutsceneMaker.send(player,"A value cannot be empty string!");
            } else {
                callback.accept(s);
                manager.runTaskLater(() -> {
                    if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = false;
                    open();
                },5);
            }
        });
    }
    protected void callbackChat(String[] message, Consumer<String> callback) {
        if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = true;
        CallbackManager.callbackChat(player,message,t -> {
            String s = t[0];
            if (s.equals("cancel")) {
                CutsceneMaker.send(player,"task cancelled.");
            } else {
                callback.accept(s);
            }
            manager.runTaskLater(() -> {
                if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = false;
                open();
            },5);
        });
    }
    protected void callbackInventory(Inventory inv, Consumer<Map<Integer, ItemStack>> callback) {
        if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = true;
        CallbackManager.callbackInventory(player,inv,m -> {
            callback.accept(m);
            manager.runTaskLater(() -> {
                if (this instanceof SubAdapter) ((SubAdapter) this).safeEnd = false;
                open();
            },5);
        });
    }
}
