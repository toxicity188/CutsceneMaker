package kor.toxicity.cutscenemaker.util;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigWriter {

    private final YamlConfiguration config;
    private final File file;

    public ConfigWriter(File file) throws IOException, InvalidConfigurationException {
        this.file = file;
        if (!file.exists()) file.createNewFile();
        config = new YamlConfiguration();
        config.load(file);
    }

    public void save() throws IOException {
        config.save(file);
    }

    public void set(String key, Object value) {
        config.set(key,value);
    }
    public ConfigurationSection createSection(String key) {
        return config.createSection(key);
    }

    public void setLocation(String key, Location location) {
        ConfigurationSection section = createSection(key);
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("pitch", location.getPitch());
        section.set("yaw", location.getYaw());
        section.set("world", location.getWorld().getName());
    }
}
