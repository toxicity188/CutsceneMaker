package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.ItemSupplier;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.EqualsAndHashCode;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class Present implements DialogAddonSupplier {

    private static final FunctionPrinter DEFAULT_TITLE = new FunctionPrinter("Click your item you want to present!");
    private static final ItemSupplier DEFAULT_ITEM_SUPPLIER;
    private static final Consumer<Player> DEFAULT_SOUND = QuestUtil.getSoundPlay("item.armor.equip_chain 1 1");
    private static final String NO_ITEM_PRESENTED_MESSAGE = "quest-no-item-presented-message";
    private static final String NO_ITEM_FOUND_MESSAGE = "quest-no-item-found-message";
    private static final String LESS_ITEM_AMOUNT_MESSAGE = "quest-less-item-amount-message";

    static {
        ItemStack stack = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Present");
        meta.setLore(Collections.singletonList(ChatColor.WHITE + "Click here to present your item!"));
        stack.setItemMeta(meta);
        DEFAULT_ITEM_SUPPLIER = new ItemBuilder(stack);
    }

    private final Set<PresentKey> presentKeys = new HashSet<>();
    private final CutsceneManager manager;
    private final FunctionPrinter name;
    private final ItemSupplier supplier;
    private final boolean take;
    private final Consumer<Player> soundPlay;

    Present(CutsceneManager manager, ConfigurationSection section) {
        this.manager = manager;
        name = (section.isSet("Name") && section.isSet("Name")) ? new FunctionPrinter(section.getString("Name")) : DEFAULT_TITLE;
        take = section.getBoolean("TakeItem",true);
        String sound = section.getString("Sound",null);
        soundPlay = (sound != null) ? QuestUtil.getSoundPlay(sound) : DEFAULT_SOUND;
        getSection(section,"Present").ifPresent(c -> c.getKeys(false).forEach(s -> getSection(c,s).ifPresent(t -> {
            try {
                presentKeys.add(new PresentKey(t));
            } catch (Exception e) {
                CutsceneMaker.warn("Error: " + e.getMessage());
            }
        })));
        if (presentKeys.size() == 0) throw new RuntimeException("This Present is empty!");
        supplier = (section.isSet("Confirm")) ? InvUtil.fromConfig(section,"Confirm") : DEFAULT_ITEM_SUPPLIER;
    }
    private final DialogAddon presentAddon = new PresentAddon();

    private class PresentAddon implements DialogAddon {

        @Override
        public boolean isGui() {
            return true;
        }

        @Override
        public void run(Dialog.DialogCurrent current) {
            Inventory inv = InvUtil.create(name.print(current.player),3);
            inv.setItem(13,current.inventory.getItem(CutsceneConfig.getInstance().getDefaultDialogCenter()));
            inv.setItem(15, supplier.get(current.player));

            Map<PresentKey,ItemStack> map = presentKeys.stream().collect(Collectors.toMap(k -> k, k -> k.builder.get(current.player)));
            GuiRegister.registerNewGui(new GuiAdapter(current.player,inv) {
                private PresentKey stack;
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (isPlayerInventory) {
                        Map.Entry<PresentKey,ItemStack> entry = map.entrySet().stream().filter(e -> e.getValue().isSimilar(item)).findFirst().orElse(null);
                        if (entry != null) {
                            if (item.getAmount() < entry.getValue().getAmount()) {
                                sendMessage(LESS_ITEM_AMOUNT_MESSAGE);
                                return;
                            }
                            inv.setItem(11,entry.getValue());
                            stack = entry.getKey();
                            soundPlay.accept(current.player);
                            current.player.updateInventory();
                        } else sendMessage(NO_ITEM_FOUND_MESSAGE);
                    } else if (slot == 15) {
                        if (stack == null) {
                            sendMessage(NO_ITEM_PRESENTED_MESSAGE);
                            return;
                        }
                        manager.runTask(() -> {
                            if (!stack.random().run(current)) {
                                current.player.closeInventory();
                            } else if (take) current.addTakeItem(stack.builder);
                        });

                    }
                }
                private void sendMessage(String msg) {
                    MessageSender sender = QuestData.QUEST_MESSAGE_MAP.get(msg);
                    if (sender != null) sender.send(current.player);
                }
            });
        }
    }
    private Optional<ConfigurationSection> getSection(ConfigurationSection section, String key) {
        return (section.isSet(key) && section.isConfigurationSection(key)) ? Optional.of(section.getConfigurationSection(key)) : Optional.empty();
    }

    @Override
    public DialogAddon getDialogAddon() {
        return presentAddon;
    }

    @EqualsAndHashCode
    private static class PresentKey {
        private final ItemBuilder builder;
        @EqualsAndHashCode.Exclude
        private final Dialog[] dialog;

        private PresentKey(ConfigurationSection section) {
            ItemBuilder item = InvUtil.toName(section.getString("Item"));
            dialog = QuestUtil.getDialog(section.getStringList("Dialog"));
            if (item == null || dialog == null) throw new RuntimeException("The item or dialog value doesn't exist!");

            int amount = section.getInt("Amount",1);
            builder = (amount != item.getAmount()) ? item.setAmount(amount) : item;
        }
        private Dialog random() {
            return dialog[ThreadLocalRandom.current().nextInt(0,dialog.length)];
        }
    }
}
