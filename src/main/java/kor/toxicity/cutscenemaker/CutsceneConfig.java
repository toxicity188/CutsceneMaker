package kor.toxicity.cutscenemaker;

import kor.toxicity.cutscenemaker.util.ConfigLoad;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;

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
