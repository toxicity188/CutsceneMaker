package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiExecutor;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class QnA extends AbstractEditorSupplier {
    private final FunctionPrinter name;
    private final int slot;
    private final int center;
    private final Map<Integer,Button> buttonMap = new HashMap<>();
    QnA(String fileName, String name, CutsceneManager manager, ConfigurationSection section) {
        super(fileName,name,manager,section);
        this.name = (section.isSet("Name") && section.isSet("Name")) ? new FunctionPrinter(section.getString("Name")) : null;
        this.slot = Math.min(section.getInt("Slot",3),5);
        center = Math.floorDiv(slot+1,2)*9-5;
        if (section.isSet("Button") && section.isConfigurationSection("Button")) {
            ConfigurationSection button = section.getConfigurationSection("Button");
            button.getKeys(false).forEach(s -> {
                try {
                    ConfigurationSection detail = button.getConfigurationSection(s);
                    buttonMap.put(Integer.parseInt(s),new Button(
                            InvUtil.fromConfig(detail,"Item"),
                            QuestUtil.getDialog(detail.getStringList("Dialog"))
                    ));
                } catch (Exception ignored) {

                }
            });
        } else throw new IllegalStateException("Invalid statement.");
    }
    void run(Dialog.DialogCurrent current) {
        Inventory inventory = InvUtil.create((name != null) ? name.print(current.player) : (current.inventory != null ? current.inventory.getTitle() : current.talker + "'s question"),slot);
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

    @Override
    public Editor getEditor(Player player) {
        return new QnAEditor(player);
    }

    private class QnAEditor extends AbstractEditor {
        private final ConfigurationSection resources = QuestUtil.copy(section);
        private final Map<Integer,ConfigurationSection> sectionMap = new HashMap<>();
        private String name = getString(resources,"Name").orElse(null);
        private int slot = getInt(resources,"Slot").orElse(3);

        private int newCenter = Math.floorDiv(slot+1,2)*9-5;

        QnAEditor(Player player) {
            super(player, "QnA");
            getConfig(resources,"Button").ifPresent(c -> c.getKeys(false).forEach(s -> getConfig(c,s).ifPresent(t -> {
                try {
                    sectionMap.put(Integer.parseInt(s),t);
                } catch (Exception ignored) {}
            })));
        }

        private void setCenter(int i) {
            slot = i;
            newCenter = Math.floorDiv(slot+1,2)*9-5;
        }

        private void reopen() {
            manager.runTaskLater(this::updateGui,5);
        }
        private Inventory getInventory() {
            Inventory inv = InvUtil.create(invName,slot + 1);
            sectionMap.forEach((k,v) -> {
                ItemBuilder builder = InvUtil.fromConfig(v,"Item");
                if (builder != null) inv.setItem(
                        k,
                        addLore(builder.get(player), Arrays.asList(
                                "",
                                ChatColor.GRAY + "(Left: open editor)",
                                ChatColor.GRAY + "(Shift+Left: delete this button.)"
                        ))
                );
            });
            inv.setItem(
                    newCenter,
                    new ItemStack(Material.BARRIER)
            );
            return inv;
        }
        @Override
        public GuiExecutor getMainExecutor() {
            return new GuiAdapter(player,getInventory()) {
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    ConfigurationSection get = sectionMap.get(slot);
                    if (get != null) openButtonEditor(slot,get);
                }
            };
        }
        private void openButtonEditor(int slot, ConfigurationSection button) {
            ItemBuilder item = InvUtil.fromConfig(button,"Item");
            if (item == null) return;

            ItemStack defItem = item.get(player);
            Inventory sub = InvUtil.create(invName + ": " + slot,6);
            sub.setItem(13,defItem);
            GuiRegister.registerNewGui(new GuiAdapter(player,sub) {
                private ItemStack stack = defItem;
                private String[] dialogs = getStringList(button,"Dialog").map(l -> l.toArray(new String[0])).orElse(null);
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (item.getType() == Material.AIR) return;
                    if (isPlayerInventory) {
                        sub.setItem(13,item);
                        InvUtil.give(player,stack);
                        stack = sub.getItem(13);
                        item.setAmount(0);
                    } else {

                    }
                }
            });
        }
        private ItemStack addLore(ItemStack stack, List<String> lore) {
            ItemMeta meta = stack.getItemMeta();
            List<String> l = meta.getLore();
            if (l.size() > 1) l.add("");
            l.addAll(lore);
            meta.setLore(l);
            stack.setItemMeta(meta);
            return stack;
        }

        @Override
        public ConfigurationSection getSaveData() {
            ConfigurationSection buttonSection = new MemoryConfiguration();
            sectionMap.forEach((k,v) -> buttonSection.set(Integer.toString(k),v));
            resources.set("Button",buttonSection);
            return resources;
        }
    }
}
