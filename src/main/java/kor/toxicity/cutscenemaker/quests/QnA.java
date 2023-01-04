package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
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

public final class QnA {
    private final Map<Integer,Button> buttonMap = new HashMap<>();

    public QnA(ConfigurationSection section) {
        if (section.isSet("Button") && section.isConfigurationSection("Button")) {
            ConfigurationSection button = section.getConfigurationSection("Button");
            button.getKeys(false).forEach(s -> {
                try {
                    ConfigurationSection detail = button.getConfigurationSection(s);
                    buttonMap.put(Integer.parseInt(s),new Button(
                            QuestUtil.getInstance().getBuilder(detail,"Item"),
                            QuestUtil.getInstance().getDialog(detail.getStringList("Dialog"))
                    ));
                } catch (Exception ignored) {

                }
            });
        } else throw new IllegalStateException("Invalid statement.");
    }
    void run(Dialog.DialogCurrent current) {
        Inventory inventory = InvUtil.getInstance().create(current.talker + "'s question",3);
        ItemStack itemStack = current.inventory.getItem(CutsceneConfig.getInstance().getDefaultDialogCenter());
        if (itemStack != null) inventory.setItem(13,itemStack);
        buttonMap.forEach((i,b) -> inventory.setItem(i,b.builder.get(current.player)));
        GuiRegister.registerNewGui(new GuiAdapter(current.player, inventory) {
            @Override
            public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                Button button1 = buttonMap.get(slot);
                if (button1 != null && button1.dialogs != null) {
                    if (!random(button1.dialogs).run(current)) current.player.closeInventory();
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
