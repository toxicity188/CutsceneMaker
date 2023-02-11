package kor.toxicity.cutscenemaker.util.gui;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.data.ActionData;
import kor.toxicity.cutscenemaker.quests.QuestUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class InventoryGui extends InventorySupplier {

    private final Map<Integer,Button> buttonMap = new HashMap<>();
    public InventoryGui(ConfigurationSection section) {
        super(section);
        ConfigurationSection button = getConfig(section,"Button");
        if (button != null) button.getKeys(false).forEach(s -> {
            ConfigurationSection target = getConfig(button,s);
            if (target != null) {
                try {
                    String[] array = target.getStringList("Action").toArray(new String[0]);
                    buttonMap.put(Integer.parseInt(s), new Button(
                            target.getBoolean("Close",true),
                            (target.isSet("Sound")) ? QuestUtil.getSoundPlay(target.getString("Sound")) : null,
                            array
                    ));
                } catch (Exception e) {
                    CutsceneMaker.warn("The key value \"" + s + "\" is not integer!");
                }
            }
        });
    }
    public void open(Player player) {
        Inventory inv = getInventory(player);
        GuiRegister.registerNewGui(new GuiAdapter(player,inv) {
            @Override
            public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                if (!isPlayerInventory) {
                    Button button1 = buttonMap.get(slot);
                    if (button1 != null) button1.randomCast(player);
                }
            }
        });
    }
    private ConfigurationSection getConfig(ConfigurationSection section, String name) {
        return (section.isSet(name) && section.isConfigurationSection(name)) ? section.getConfigurationSection(name) : null;
    }

    @RequiredArgsConstructor
    private static class Button {
        private final boolean closeInventory;
        private final Consumer<Player> soundPlay;
        private final String[] actionArray;

        private void randomCast(Player player) {
            if (soundPlay != null) soundPlay.accept(player);
            if (closeInventory) player.closeInventory();
            ActionData.start(actionArray[ThreadLocalRandom.current().nextInt(0,actionArray.length)],player);
        }
    }
}
