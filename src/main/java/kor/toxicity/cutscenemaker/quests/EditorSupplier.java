package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.ConfigUtil;
import kor.toxicity.cutscenemaker.util.gui.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public abstract class EditorSupplier {
    private static final Map<String,Map<String,? extends EditorSupplier>> EDITOR_MAP = new HashMap<>();

    private static final Map<String,EditorConfiguration> CONFIGURATION_MAP = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EditorSupplier supplier = (EditorSupplier) o;

        return Objects.equals(name, supplier.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    private static class EditorConfiguration {
        private final Class<? extends EditorSupplier> clazz;
        private final YamlConfiguration section = new YamlConfiguration();
        private String build;
        private EditorConfiguration(Class<? extends EditorSupplier> clazz) {
            this.clazz = clazz;
        }
        private EditorConfiguration set(String key, Object obj) {
            section.set(key,obj);
            return this;
        }
        private EditorConfiguration build() {
            build = section.saveToString();
            return this;
        }

        private EditorSupplier getInstance(CutsceneManager manager, String file, String key) {
            try {
                Constructor<? extends EditorSupplier> constructor = clazz.getDeclaredConstructor(
                        String.class,
                        String.class,
                        CutsceneManager.class,
                        ConfigurationSection.class
                );
                YamlConfiguration configuration = new YamlConfiguration();
                configuration.loadFromString(build);
                constructor.setAccessible(true);
                EditorSupplier supplier = constructor.newInstance(file,key,manager,configuration);
                constructor.setAccessible(false);
                return supplier;
            } catch (Exception e) {
                return null;
            }
        }
    }


    private static final ItemStack SAVE_BUTTON = setDisplayName(
            new ItemStack(Material.BEACON),
            ChatColor.GREEN + "Save and exit"
    );
    private static final ItemStack EXIT_WITHOUT_SAVE_BUTTON = setDisplayName(
            new ItemStack(Material.BARRIER),
            ChatColor.RED + "Exit without saving"
    );

    static  {
        EDITOR_MAP.put("dialog", QuestData.DIALOG_MAP);
        EDITOR_MAP.put("qna", QuestData.QNA_MAP);
        EDITOR_MAP.put("questset", QuestData.QUEST_SET_MAP);

        CONFIGURATION_MAP.put(
                "dialog",
                new EditorConfiguration(Dialog.class)
                        .set("Talk",new String[] {"a new talk"})
                        .build()
        );
        CONFIGURATION_MAP.put(
                "qna",
                new EditorConfiguration(QnA.class)
                        .set("Button", new MemoryConfiguration())
                        .build()
        );
        CONFIGURATION_MAP.put(
                "questset",
                new EditorConfiguration(QuestSet.class)
                        .set("Title","a new Title")
                        .set("Lore", new String[] {"a new lore"})
                        .build()
        );
    }
    public static @NotNull List<String> getEditorList() {
        return new ArrayList<>(EDITOR_MAP.keySet());
    }
    public static @Nullable List<String> getEditableObjects(String key) {
        Map<String,? extends EditorSupplier> supplierMap;
        return ((supplierMap = EDITOR_MAP.get(key)) == null) ? null : new ArrayList<>(supplierMap.keySet());
    }
    private static ItemStack setDisplayName(ItemStack stack, String name) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    final String fileName;
    final String name;
    final CutsceneManager manager;
    final ConfigurationSection section;

    abstract Editor getEditor(Player player);
    public static boolean openEditor(@NotNull Player player, @NotNull String type, @NotNull String name) {
        Map<String,? extends EditorSupplier> mapper = EDITOR_MAP.get(Objects.requireNonNull(type).toLowerCase());
        if (mapper == null) return false;
        EditorSupplier abstractEditor = mapper.get(Objects.requireNonNull(name));
        if (abstractEditor == null) return false;
        abstractEditor.getEditor(Objects.requireNonNull(player)).updateGui();
        return true;
    }
    public static boolean createEditor(@NotNull Player player, @NotNull CutsceneManager manager, @NotNull String type, @NotNull String file, @NotNull String name) {
        EditorConfiguration configuration = CONFIGURATION_MAP.get(Objects.requireNonNull(type));
        if (configuration == null) return false;
        EditorSupplier supplier = configuration.getInstance(
                Objects.requireNonNull(manager),
                Objects.requireNonNull(file),
                Objects.requireNonNull(name)
        );
        if (supplier == null) return false;
        supplier.getEditor(Objects.requireNonNull(player)).updateGui();
        return true;
    }
    abstract class Editor {
        final Player player;
        final String srcName;
        final String invName;

        Editor(Player player, String srcName) {
            this.player = player;
            this.srcName = srcName;
            invName = ChatColor.GOLD.toString() + ChatColor.BOLD + ChatColor.ITALIC + srcName + ": "
                    + ChatColor.YELLOW + fileName + ": "
                    + ChatColor.WHITE + name;
        }


        abstract GuiExecutor getMainExecutor();
        abstract ConfigurationSection getSaveData();

        protected void openSign(String[] strings, Consumer<String> consumer) {
            CallbackManager.openSign(player,strings,t -> {
                String s = t[0];
                if (s.equals("")) CutsceneMaker.send(player,"the value cannot be empty string!");
                else consumer.accept(s);
                manager.runTaskLater(this::updateGui,5);
            });
        }
        protected void openInventory(Inventory inventory, Consumer<Map<Integer,ItemStack>> consumer) {
            CallbackManager.callbackInventory(player,inventory,m -> {
                consumer.accept(m);
                manager.runTaskLater(this::updateGui,5);
            });
        }

        protected void openChat(String[] strings, Consumer<String> consumer) {
            CallbackManager.callbackChat(player,strings,t -> {
                String s = t[0];
                if (s.equals("cancel")) CutsceneMaker.send(player,"successfully cancelled.");
                else consumer.accept(s);
                manager.runTaskLater(this::updateGui,5);
            });
        }
        public final void updateGui() {
            GuiExecutor executor = getMainExecutor();
            Inventory inventory = executor.getInventory();
            int slot = inventory.getContents().length;
            inventory.setItem(slot - 1, SAVE_BUTTON);
            inventory.setItem(slot - 9, EXIT_WITHOUT_SAVE_BUTTON);
            GuiRegister.registerNewGui(new GuiAdapter(Objects.requireNonNull(player),manager,inventory) {
                @Override
                public void initialize() {
                    executor.initialize();
                }

                @Override
                public void onEnd() {
                    executor.onEnd();
                }

                @SuppressWarnings("ResultOfMethodCallIgnored")
                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (SAVE_BUTTON.equals(item)) {
                        manager.runTaskAsynchronously(() -> {
                            File file = new File(
                                    manager.getPlugin().getDataFolder().getAbsolutePath() + "\\" + srcName + "\\" + fileName + ".yml"
                            );
                            try {
                                if (!file.exists()) file.createNewFile();
                                YamlConfiguration configuration = new YamlConfiguration();
                                configuration.load(file);
                                configuration.set(name,getSaveData());
                                configuration.save(file);
                                CutsceneMaker.send(player,"The " + srcName + " " + name + " successfully saved.");
                            } catch (IOException | InvalidConfigurationException e) {
                                CutsceneMaker.send(player,"Unable to save the " + srcName + " " + name);
                            }
                            manager.runTask(player::closeInventory);
                        });
                    } else if (EXIT_WITHOUT_SAVE_BUTTON.equals(item)) {
                        player.closeInventory();
                    } else {
                        executor.onClick(item, slot, button, isPlayerInventory);
                    }
                }
            });
        }
    }
}
