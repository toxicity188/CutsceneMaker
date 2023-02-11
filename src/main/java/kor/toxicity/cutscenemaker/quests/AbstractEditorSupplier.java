package kor.toxicity.cutscenemaker.quests;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.quests.editor.EditorSupplier;
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
public abstract class AbstractEditorSupplier implements EditorSupplier {
    private static final Map<String,Map<String,? extends AbstractEditorSupplier>> EDITOR_MAP = new HashMap<>();

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

    public static boolean openEditor(@NotNull Player player, @NotNull String type, @NotNull String name) {
        Map<String,? extends AbstractEditorSupplier> mapper = EDITOR_MAP.get(Objects.requireNonNull(type).toLowerCase());
        if (mapper == null) return false;
        AbstractEditorSupplier abstractEditor = mapper.get(name);
        if (abstractEditor == null) return false;
        abstractEditor.getEditor(player).updateGui();
        return true;
    }
    abstract class AbstractEditor implements EditorSupplier.Editor {
        final Player player;
        final String srcName;
        final String invName;

        AbstractEditor(Player player, String srcName) {
            this.player = player;
            this.srcName = srcName;
            invName = ChatColor.GOLD.toString() + ChatColor.BOLD + ChatColor.ITALIC + srcName + ": "
                    + ChatColor.YELLOW + fileName + ": "
                    + ChatColor.WHITE + name;
        }
        final Optional<ConfigurationSection> getConfig(ConfigurationSection section, String... key) {
            return Arrays.stream(key).filter(k -> section.isSet(k) && section.isConfigurationSection(k)).findFirst().map(section::getConfigurationSection);
        }
        final Optional<List<String>> getStringList(ConfigurationSection section, String... key) {
            return Arrays.stream(key).filter(k -> section.isSet(k) && section.isList(k)).findFirst().map(section::getStringList);
        }
        final Optional<String> getString(ConfigurationSection section, String... key) {
            return Arrays.stream(key).filter(k -> section.isSet(k) && section.isString(k)).findFirst().map(section::getString);
        }
        final Optional<Integer> getInt(ConfigurationSection section, String... key) {
            return Arrays.stream(key).filter(k -> section.isSet(k) && section.isInt(k)).findFirst().map(section::getInt);
        }


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
