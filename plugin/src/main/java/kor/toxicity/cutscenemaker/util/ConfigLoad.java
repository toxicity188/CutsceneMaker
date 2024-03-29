package kor.toxicity.cutscenemaker.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigLoad {
	
	private final YamlConfiguration main = new YamlConfiguration();
	private final Map<String,Set<String>> keyList = new HashMap<>();
	
	public ConfigLoad(JavaPlugin pl, String file, String key) throws IOException, InvalidConfigurationException {
		File test = new File(pl.getDataFolder(),file);
		if (!test.exists()) test.createNewFile();
		QuestConfig_Load(pl.getDataFolder(),file,key);
	}
	public ConfigLoad(File folder, String key) {
		if (!folder.exists()) folder.mkdir();
		for (File MarketConfig : folder.listFiles()) {
			try {
				QuestConfig_Load(folder, MarketConfig.getName(), key);
			} catch (Exception e) {e.printStackTrace();}
		}
	}
	public ConfigLoad(File folder, String file, String key) throws IOException, InvalidConfigurationException {
		File test = new File(folder,file);
		if (!test.exists()) test.createNewFile();
		main.load(test);
	}
	public void QuestConfig_Load(File folder, String file, String key) throws IOException, InvalidConfigurationException {
		File config = new File(folder, file);
		if (config.exists() && file.contains(".yml")) {
			if (!key.equals("")) {
				this.main.createSection(key);
				key = key + ".";
			}
			YamlConfiguration t = new YamlConfiguration();
			t.load(config);
			for (String i : t.getKeys(true)) {
				if (t.get(i).getClass() == MemorySection.class) { 
					this.main.createSection(key + i);
				} else {
					this.main.set(key + i, t.get(i));
				}
			}
			keyList.put(file.replaceAll(".yml",""), t.getKeys(false));
		}
	}

	public List<String> getAllFiles() {
		List<String> t = new ArrayList<>();
		keyList.forEach((a,b) -> t.addAll(b));
		return t;
	}
	public void forEach(BiConsumer<String,Set<String>> consumer) {
		keyList.forEach(consumer);
	}


	public boolean isSet(String key) {
		return this.main.contains(key);
	}
	public Object get(String key) {
		if (!this.main.contains(key)) return null;
		return this.main.get(key);
	}
	public ItemStack getItemStack(String key) {
		return (main.isSet(key) && main.isItemStack(key)) ? main.getItemStack(key) : null;
	}
	public Set<String> getKeys(String key) {
		if (!this.main.contains(key) && !this.main.isConfigurationSection(key)) return null;
		return this.main.getConfigurationSection(key).getKeys(false);
	}
	public ConfigurationSection getConfigurationSection(String key) {
		if (!this.main.contains(key) && !this.main.isConfigurationSection(key)) return null;
		return this.main.getConfigurationSection(key);
	}
	public List<String> getStringList(String key) {
		if (!this.main.contains(key) && !this.main.isConfigurationSection(key)) return null;
		return this.main.getStringList(key);
	}

	public String getString(String key,String def) {
		if (this.main.isConfigurationSection(key)) return def;
		if (!this.main.contains(key)) return def;
		return this.main.getString(key);
	}
	public float getFloat(String key,float def) {
		if (!this.main.contains(key)) return def;
		return (float) this.main.getDouble(key);
	}

	public double getDouble(String key,double def) {
		if (!this.main.contains(key)) return def;
		return this.main.getDouble(key);
	}

	public int getInt(String key,int def) {
		if (!this.main.contains(key)) return def;
		return this.main.getInt(key);
	}

	public boolean getBoolean(String key, boolean def) {
		if (!this.main.contains(key)) return def;
		return this.main.getBoolean(key);
	}

}
