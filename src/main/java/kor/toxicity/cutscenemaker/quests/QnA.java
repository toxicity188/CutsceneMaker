package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.ConfigUtil;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import kor.toxicity.cutscenemaker.util.MetaBuilder;
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

final class QnA extends EditorSupplier implements DialogAddon {
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
                            QuestUtil.getDialog(detail.getStringList("Dialog"),fileName,name)
                    ));
                } catch (Exception ignored) {

                }
            });
        } else throw new IllegalStateException("Invalid statement.");
    }
    @Override
    public boolean isGui() {
        return true;
    }
    @Override
    public void run(Dialog.DialogCurrent current) {

        Inventory inventory = InvUtil.create((name != null) ? name.print(current.player) : (current.inventory != null ? current.player.getOpenInventory().getTitle() : current.talker + "'s question"),slot);
        ItemStack itemStack = current.inventory.getItem(CutsceneConfig.getInstance().getDefaultDialogCenter());
        buttonMap.forEach((i,b) -> inventory.setItem(i,b.builder.get(current.player)));
        if (itemStack != null) inventory.setItem(center,itemStack);
        GuiRegister.registerNewGui(new GuiAdapter(current.player,manager, inventory) {
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

    private class QnAEditor extends Editor {
        private final ConfigurationSection resource = QuestUtil.copy(section);
        private String name = ConfigUtil.getString(resource,"Name").orElse(null);
        private int slot = ConfigUtil.getInt(resource,"Slot").orElse(3);

        private final Map<Integer,ItemEditor> editorMap = new HashMap<>();

        private class ItemEditor {
            private ItemBuilder item;
            private String[] dialogs;
            private ItemEditor(ConfigurationSection section) {
                item = InvUtil.fromConfig(section,"Item");
                dialogs = ConfigUtil.getStringList(section,"Dialog").map(l -> l.toArray(new String[0])).orElse(null);
            }
            private void open() {
                Inventory subInv = InvUtil.create(invName + ": Button",3);
                subInv.setItem(
                        26,
                        new MetaBuilder(new ItemStack(Material.STONE_BUTTON))
                                .setDisplayName(ChatColor.WHITE + "Back")
                                .setLore(
                                        Arrays.asList(
                                                "",
                                                ChatColor.GRAY + "(Click: back)"
                                        )
                                )
                                .build()
                );
                GuiRegister.registerNewGui(new GuiAdapter(player,manager,subInv) {
                    @Override
                    public void initialize() {
                        subInv.setItem(11, (item != null) ? new MetaBuilder(item.get())
                                .addLore(Arrays.asList(
                                        "",
                                        ChatColor.GRAY + "(Click player inventory: change item)"
                                ))
                                .build() : null);
                        List<String> strings = new ArrayList<>((dialogs == null) ? Collections.singletonList(ChatColor.GRAY + "<none>") : Arrays.stream(dialogs).map(s -> ChatColor.GRAY + " - " + ChatColor.WHITE + s).collect(Collectors.toList()));
                        strings.add("");
                        strings.add(ChatColor.GRAY + "(Left: add Dialog, Right - Delete last dialog)");
                        subInv.setItem(
                                15,
                                new MetaBuilder(new ItemStack(Material.BOOK))
                                        .setDisplayName(ChatColor.WHITE + "Dialogs")
                                        .setLore(strings)
                                        .build()
                        );
                    }
                    @Override
                    public void onClick(ItemStack clicked, int slot, MouseButton button, boolean isPlayerInventory) {
                        if (isPlayerInventory) {
                            ItemStack stack = clicked.clone();
                            clicked.setAmount(0);
                            if (item != null) InvUtil.give(player,item.get());
                            item = new ItemBuilder(stack);
                            initialize();
                            player.updateInventory();
                        } else switch (slot) {
                            case 15:
                                switch (button) {
                                    case LEFT:
                                    case LEFT_WITH_SHIFT:
                                        callbackSign(new String[] {
                                                "",
                                                "write the dialog here!",
                                                "",
                                                ""
                                        },s -> {
                                            if (!QuestData.DIALOG_MAP.containsKey(s)) {
                                                CutsceneMaker.send(player,"The Dialog named \"" + s + "\" doesn't exist!");
                                            } else {
                                                dialogs = QuestUtil.plusElement(dialogs,s);
                                                initialize();
                                                player.updateInventory();
                                            }
                                        });
                                        break;
                                    case RIGHT:
                                    case RIGHT_WITH_SHIFT:
                                        dialogs = QuestUtil.deleteLast(dialogs);
                                        initialize();
                                        player.updateInventory();
                                        break;
                                }
                                break;
                            case 26:
                                updateGui();
                                break;
                        }
                    }
                });
            }
        }

        public QnAEditor(Player player) {
            super(player,"QnA");
            ConfigUtil.getConfig(resource,"Button").ifPresent(c -> c.getKeys(false).forEach(k -> ConfigUtil.getConfig(c,k).ifPresent(c2 -> {
                try {
                    editorMap.put(Integer.parseInt(k),new ItemEditor(c2));
                } catch (NumberFormatException ignored) {}
            })));
        }

        @Override
        GuiExecutor getMainExecutor() {
            Inventory inventory = InvUtil.create(invName,slot + 1);
            Iterator<Map.Entry<Integer,ItemEditor>> entryIterator = editorMap.entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<Integer,ItemEditor> entry = entryIterator.next();
                ItemEditor v = entry.getValue();
                Integer k = entry.getKey();
                ItemBuilder builder = v.item;
                if (builder != null) inventory.setItem(k,new MetaBuilder(builder.get())
                        .addLore(Arrays.asList(
                                "",
                                ChatColor.GRAY + "(Left: Open Item Editor, Right: Relocate this item)"
                        ))
                        .build());
                else entryIterator.remove();
            }
            int center = Math.floorDiv(slot+1,2)*9-5;
            ItemStack barrier = new MetaBuilder(new ItemStack(Material.BARRIER))
                    .setDisplayName(ChatColor.RED + "Talker")
                    .build();
            inventory.setItem(center, barrier);
            int q = slot * 9;
            inventory.setItem(
                    q + 2,
                    new MetaBuilder(new ItemStack(Material.BOOK))
                            .setDisplayName(ChatColor.WHITE + "Rename")
                            .setLore(Arrays.asList(
                                    "",
                                    ChatColor.WHITE + "Rename this inventory.",
                                    ChatColor.YELLOW + "Current Name: " + ((name != null) ? ChatColor.WHITE + name : ChatColor.GRAY + "<none>"),
                                    "",
                                    ChatColor.GRAY + "(Click: rename this inventory)"
                            ))
                            .build()
            );
            inventory.setItem(
                    q + 4,
                    new MetaBuilder(new ItemStack(Material.PAPER))
                            .setDisplayName(ChatColor.WHITE + "Resize")
                            .setLore(Arrays.asList(
                                    "",
                                    ChatColor.WHITE + "Resize this inventory.",
                                    ChatColor.YELLOW + "Current Size: " + ChatColor.WHITE + QnAEditor.this.slot,
                                    "",
                                    ChatColor.GRAY + "(Click: resize this inventory)"
                            ))
                            .build()
            );
            inventory.setItem(
                    q + 6,
                    new MetaBuilder(new ItemStack(Material.CHEST))
                            .setDisplayName(ChatColor.WHITE + "Change layout")
                            .setLore(Arrays.asList(
                                    "",
                                    ChatColor.WHITE + "Change the layout of this QnA.",
                                    "",
                                    ChatColor.GRAY + "(Click: change layout)"
                            ))
                            .build()
            );
            return new GuiAdapter(player,manager,inventory) {
                @Override
                public void onClick(ItemStack clicked, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (slot == q + 2) {
                        openChat(new String[]{
                                ChatColor.YELLOW + "enter a value in the chat. " + ChatColor.GOLD + "cancel: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "cancel" + ChatColor.WHITE + "\"",
                                ChatColor.GOLD + "remove: " + ChatColor.WHITE + "type \"" + ChatColor.RED + "null" + ChatColor.WHITE + "\""
                        }, s -> {
                            if ("null".equals(s)) {
                                CutsceneMaker.send(player, "successfully changed to <none>.");
                                name = null;
                            } else {
                                CutsceneMaker.send(player, "successfully changed to " + s + ".");
                                name = s;
                            }
                            reopen();
                        });
                    }
                    else if (slot == q + 4) {
                        openSign(new String[]{
                                "",
                                "write the size here!",
                                "",
                                ""
                        }, s -> {
                            try {
                                QnAEditor.this.slot = Math.min(5, Math.max(3, Integer.parseInt(s)));
                            } catch (NumberFormatException e) {
                                CutsceneMaker.send(player, "An argument \"" + s + "\" is not an integer!");
                            }
                        });
                    }
                    else if (slot == q + 6) {
                        Inventory callbackInv = InvUtil.create("Put your item in here!",QnAEditor.this.slot);
                        callbackInv.setItem(center,barrier);
                        editorMap.forEach((k,v) -> {
                            ItemBuilder builder = v.item;
                            callbackInv.setItem(k,(builder != null) ? builder.get() : null);
                        });
                        openInventory(callbackInv,m -> m.forEach((k, v) -> {
                            ConfigurationSection configuration = new MemoryConfiguration();
                            configuration.set("Item",v);
                            if (v.getType() != Material.AIR) {
                                ItemEditor editor = editorMap.get(k);
                                editorMap.put(k,(editor != null) ? editor : new ItemEditor(configuration));
                            }
                            else editorMap.remove(k);
                        }));
                    }
                    else if (clicked.getType() != Material.BARRIER) {
                        switch (button) {
                            case LEFT:
                            case LEFT_WITH_SHIFT:
                                ItemEditor editor = editorMap.get(slot);
                                if (editor != null) editor.open();
                                break;
                            case RIGHT:
                            case RIGHT_WITH_SHIFT:
                                openSign(new String[]{
                                        "",
                                        "write the slot here!",
                                        "",
                                        ""
                                },s -> {
                                    try {
                                        int t = Integer.parseInt(s);
                                        ItemEditor editor1 = editorMap.remove(slot);
                                        if (editor1 != null) editorMap.put(t,editor1);
                                    } catch (NumberFormatException e) {
                                        CutsceneMaker.send(player,"A argument \"" + s + "\" is not an integer!");
                                    }
                                });
                                break;
                        }
                    }
                }
            };
        }
        private void reopen() {
            manager.runTaskLater(this::updateGui, 5);
        }
        @Override
        ConfigurationSection getSaveData() {
            resource.set("Name",name);
            resource.set("Slot",(slot == 3) ? null : slot);

            ConfigurationSection button = new MemoryConfiguration();
            for (Map.Entry<Integer, ItemEditor> e : editorMap.entrySet()) {
                MemoryConfiguration configuration = new MemoryConfiguration();
                ItemEditor editor = e.getValue();
                configuration.set("Item",editor.item != null ? editor.item.get() : null);
                configuration.set("Dialog",editor.dialogs);
                button.set(Integer.toString(e.getKey()),configuration);
            }
            resource.set("Button",button);
            return resource;
        }
    }
}
