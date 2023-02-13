package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.util.ConfigUtil;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiExecutor;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public abstract class EditorSupplier {
    private static final Map<String,Map<String,? extends EditorSupplier>> EDITOR_MAP = new HashMap<>();

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
        protected final Optional<ConfigurationSection> getConfig(ConfigurationSection section, String... key) {
            return ConfigUtil.getConfig(section, key);
        }
        protected final Optional<List<String>> getStringList(ConfigurationSection section, String... key) {
            return ConfigUtil.getStringList(section,key);
        }
        protected final Optional<String> getString(ConfigurationSection section, String... key) {
            return ConfigUtil.getString(section,key);
        }
        protected final Optional<Integer> getInt(ConfigurationSection section, String... key) {
            return ConfigUtil.getInt(section,key);
        }
        abstract GuiExecutor getMainExecutor();
        abstract ConfigurationSection getSaveData();

        public final void updateGui() {
            GuiExecutor executor = getMainExecutor();
            Inventory inventory = executor.getInventory();
            int slot = inventory.getContents().length;
            inventory.setItem(slot - 1, SAVE_BUTTON);
            inventory.setItem(slot - 9, EXIT_WITHOUT_SAVE_BUTTON);
            GuiRegister.registerNewGui(new GuiAdapter(Objects.requireNonNull(player),inventory) {
                @Override
                public void initialize() {
                    executor.initialize();
                }

                @Override
                public void onEnd() {
                    executor.onEnd();
                }

                @Override
                public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                    if (SAVE_BUTTON.equals(item)) {
                        manager.runTaskAsynchronously(() -> {
                            File file = new File(
                                    manager.getPlugin().getDataFolder().getAbsolutePath() + "\\" + srcName + "\\" + fileName + ".yml"
                            );
                            YamlConfiguration configuration = new YamlConfiguration();
                            try {
                                configuration.load(file);
                                configuration.set(name,getSaveData());
                                configuration.save(file);
                                player.sendMessage(ChatColor.AQUA + CutsceneMaker.NAME + ChatColor.WHITE + " The " + srcName + " " + name + " successfully saved.");
                            } catch (IOException | InvalidConfigurationException e) {
                                player.sendMessage(ChatColor.AQUA + CutsceneMaker.NAME + ChatColor.WHITE + " unable to save the " + srcName + " " + name);
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
