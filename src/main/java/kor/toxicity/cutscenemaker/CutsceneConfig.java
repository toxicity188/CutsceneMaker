package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.quests.QuestSet;
import kor.toxicity.cutscenemaker.quests.QuestUtil;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class CutsceneConfig {
    @Getter
    private static final CutsceneConfig instance = new CutsceneConfig();
    @Getter
    private GameMode defaultGameMode;
    @Getter
    private boolean changeGameMode;
    @Getter
    private int autoSaveTime;
    @Getter
    private Material dialogReader;
    @Getter
    private Consumer<Player> defaultTypingSound, questCompleteSound;
    @Getter
    private int defaultDialogRows;
    @Getter
    private int defaultDialogCenter;
    @Getter
    private ItemStack defaultQuestIcon;

    void load(CutsceneMaker pl) {
        try {
            if (!new File(pl.getDataFolder().getAbsolutePath() + "\\config.yml").exists()) pl.saveResource("config.yml",false);
            ConfigLoad load = new ConfigLoad(pl,"config.yml","");

            String mode = load.getString("default-game-mode","SURVIVAL");
            safeSet(() -> defaultGameMode = GameMode.valueOf(mode.toUpperCase()),() -> {
                CutsceneMaker.warn("unable to find the game mode named \"" + mode + "\"");
                defaultGameMode = GameMode.SURVIVAL;
            });
            String material = load.getString("default-dialog-reader","BOOK");
            safeSet(() -> dialogReader = Material.valueOf(material.toUpperCase()),() -> {
                CutsceneMaker.warn("unable to find the material named \"" + material + "\"");
                dialogReader = Material.BOOK;
            });
            changeGameMode = load.getBoolean("change-game-mode",true);
            autoSaveTime = load.getInt("auto-save-time",300);

            defaultTypingSound = QuestUtil.getInstance().getSoundPlay(load.getString("default-typing-sound","block.stone_button.click_on 0.2 0.7"));
            questCompleteSound = QuestUtil.getInstance().getSoundPlay(load.getString("quest-complete-sound","ui.toast.challenge_complete 1 1"));

            defaultDialogRows = load.getInt("default-dialog-rows",5);
            defaultDialogCenter = load.getInt("default-dialog-center", 22);

            String questMaterial = load.getString("default-quest-icon", "BOOK");
            safeSet(() -> defaultQuestIcon = new ItemStack(Material.valueOf(questMaterial.toUpperCase())),() -> {
                CutsceneMaker.warn("unable to find the material named \"" + questMaterial + "\"");
                defaultQuestIcon = new ItemStack(Material.BOOK);
            });
            defaultQuestIcon.setDurability((short) load.getInt("default-quest-durability",0));
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void safeSet(Runnable tries, Runnable ifFailed) {
        try {
            tries.run();
        } catch (Exception e) {
            ifFailed.run();
        }
    }
}
