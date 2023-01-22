package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

final class QnA {
    private final FunctionPrinter name;
    private final int slot;
    private final int center;
    private final Map<Integer,Button> buttonMap = new HashMap<>();
    private final CutsceneManager manager;
    QnA(CutsceneManager manager, ConfigurationSection section) {
        this.manager = manager;
        this.name = (section.isSet("Name") && section.isSet("Name")) ? new FunctionPrinter(section.getString("Name")) : null;
        this.slot = section.getInt("Slot",3);
        center = Math.floorDiv(slot+1,2)*9-5;
        if (section.isSet("Button") && section.isConfigurationSection("Button")) {
            ConfigurationSection button = section.getConfigurationSection("Button");
            button.getKeys(false).forEach(s -> {
                try {
                    ConfigurationSection detail = button.getConfigurationSection(s);
                    buttonMap.put(Integer.parseInt(s),new Button(
                            InvUtil.getInstance().fromConfig(detail,"Item"),
                            QuestUtil.getInstance().getDialog(detail.getStringList("Dialog"))
                    ));
                } catch (Exception ignored) {

                }
            });
        } else throw new IllegalStateException("Invalid statement.");
    }
    void run(Dialog.DialogCurrent current) {
        Inventory inventory = InvUtil.getInstance().create((name != null) ? name.print(current.player) : (current.inventory != null ? current.inventory.getTitle() : current.talker + "'s question"),slot);
        ItemStack itemStack = current.inventory.getItem(CutsceneConfig.getInstance().getDefaultDialogCenter());
        buttonMap.forEach((i,b) -> inventory.setItem(i,b.builder.get(current.player)));
        if (itemStack != null) inventory.setItem(center,itemStack);
        GuiRegister.registerNewGui(new GuiAdapter(current.player, inventory) {
            @Override
            public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                Button button1 = buttonMap.get(slot);
                if (button1 != null) {
                    manager.runTask(() -> {
                        if (button1.dialogs == null || !random(button1.dialogs).run(current)) current.player.closeInventory();
                    });
                }
            }
        });
    }
    private <T> T random(T[] dialogs) {
        return dialogs[ThreadLocalRandom.current().nextInt(0,dialogs.length)];
    }
    @RequiredArgsConstructor
    private static class Button {
        private final ItemBuilder builder;
        private final Dialog[] dialogs;
    }
}
