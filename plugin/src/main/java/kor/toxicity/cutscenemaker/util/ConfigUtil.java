package kor.toxicity.cutscenemaker.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ConfigUtil {
    private ConfigUtil() {
        throw new RuntimeException();
    }
    public static Optional<ConfigurationSection> getConfig(ConfigurationSection section, String... key) {
        return Arrays.stream(key).filter(k -> section.isSet(k) && section.isConfigurationSection(k)).findFirst().map(section::getConfigurationSection);
    }
    public static Optional<List<String>> getStringList(ConfigurationSection section, String... key) {
        return Arrays.stream(key).filter(k -> section.isSet(k) && section.isList(k)).findFirst().map(section::getStringList);
    }
    public static Optional<String> getString(ConfigurationSection section, String... key) {
        return Arrays.stream(key).filter(k -> section.isSet(k) && section.isString(k)).findFirst().map(section::getString);
    }
    public static Optional<Integer> getInt(ConfigurationSection section, String... key) {
        return Arrays.stream(key).filter(k -> section.isSet(k) && section.isInt(k)).findFirst().map(section::getInt);
    }
}
