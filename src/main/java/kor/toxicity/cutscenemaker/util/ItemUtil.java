package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class ItemUtil {
	private ItemUtil() {
		throw new RuntimeException();
	}
	private static Method getTag, setTag, hasTag, hasKey, getString, setString, asNMSCopy, asCraftMirror;

	static {
		try {
			Class<?> nmsItemStack = getNMSClass("net.minecraft.server","ItemStack");
			Class<?> nbtTagCompound = getNMSClass("net.minecraft.server","NBTTagCompound");
			Class<?> craftItemStack = getNMSClass("org.bukkit.craftbukkit","inventory.CraftItemStack");

			assert nmsItemStack != null;
			getTag = nmsItemStack.getDeclaredMethod("getTag");
			setTag = nmsItemStack.getDeclaredMethod("setTag",nbtTagCompound);
			hasTag = nmsItemStack.getDeclaredMethod("hasTag");

			assert nbtTagCompound != null;
			hasKey = nbtTagCompound.getDeclaredMethod("hasKey", String.class);
			getString = nbtTagCompound.getDeclaredMethod("getString", String.class);
			setString = nbtTagCompound.getDeclaredMethod("setString", String.class, String.class);

			assert craftItemStack != null;
			asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
			asCraftMirror = craftItemStack.getDeclaredMethod("asCraftMirror", nmsItemStack);
		} catch (Exception e) {
			CutsceneMaker.warn("Unable to load the NBT Class.");
		}
	}
	private static Class<?> getNMSClass(String pack, String name) {
		try {
			return Class.forName(pack + "." + getVersion() + "." + name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static String getVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}

	public static ItemStack setInternalTag(ItemStack item, String internal, String key) {
		try {
			Object o1 = asNMSCopy.invoke(null,item);
			Object o2 = getTag.invoke(o1);
			setString.invoke(o2, internal, key);
			setTag.invoke(o1, o2);
			return (ItemStack) asCraftMirror.invoke(null,o1);
		} catch (Exception e) {
			return item;
		}
	}
	public static ItemStack setInternalTag(ItemStack item, Map<String,String> key) {
		try {
			Object o1 = asNMSCopy.invoke(null,item);
			Object o2 = getTag.invoke(o1);
			key.forEach((a, b) -> {
				try {
					setString.invoke(o2, a, b);
				} catch (Exception ignored) {}
			});
			setTag.invoke(o1, o2);
			return (ItemStack) asCraftMirror.invoke(null,o1);
		} catch (Exception e) {
			return item;
		}
	}
	public static String readInternalTag(ItemStack item, String key) {
		try {
			Object nmsItemStack = asNMSCopy.invoke(null, item);
			if (!(boolean) hasTag.invoke(nmsItemStack)) return "";
			Object t = getTag.invoke(nmsItemStack);
			if (!(boolean) hasKey.invoke(t, key)) return "";
			return (String) getString.invoke(t, key);
		} catch (Exception e) {
			return "";
		}
		
	}
	public static Map<String,String> readInternalTag(ItemStack item, List<String> key) {
		try {
			Object nmsItemStack = asNMSCopy.invoke(null, item);
			if (!(boolean) hasTag.invoke(nmsItemStack)) return null;
			Map<String, String> ret = new WeakHashMap<>();
			Object t = getTag.invoke(nmsItemStack);
			for (String s : key) {
				if ((boolean) hasKey.invoke(t, s)) ret.put(s, (String) getString.invoke(t, s));
			}
			return ret;
		} catch (Exception e) {
			return null;
		}
	}
	public static String encode(StorageItem itemStack) {
		YamlConfiguration config = new YamlConfiguration();
		config.set("i", itemStack.getStack());
		config.set("t", itemStack.getTime().toString());
		config.set("h", itemStack.getLeftHour());
		return Base64.getEncoder().encodeToString(config.saveToString().getBytes(StandardCharsets.UTF_8));
	}
	public static StorageItem decode(String string) {
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.loadFromString(new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8));
		} catch (IllegalArgumentException | InvalidConfigurationException e) {
			e.printStackTrace();
			return null;
		}
		ItemStack stack = config.getItemStack("i", null);
		LocalDateTime time = LocalDateTime.parse(config.getString("t"));
		int left = config.getInt("h",-1);
		return (stack != null
				&& stack.getType() != Material.AIR
				&& (left < 0 || Duration.between(time.toLocalTime(),LocalDateTime.now().toLocalTime()).toHours() < left)
		) ? new StorageItem(stack, time, left) : null;
	}

	public static ItemMeta edit(@NotNull ItemMeta meta, String display) {
		return edit(meta,display,null);
	}
	public static ItemMeta edit(@NotNull ItemMeta meta, String display, List<String> lore) {
		meta.setDisplayName((display != null) ? display : "");
		meta.setLore(lore);
		return meta;
	}
	public static ItemMeta addLore(@NotNull ItemMeta meta, String... lore) {
		return addLore(meta,(lore == null) ? Collections.emptyList() : (lore.length == 1) ? Collections.singletonList(lore[0]) : Arrays.asList(lore));
	}
	public static ItemMeta addLore(@NotNull ItemMeta meta, List<String> lore) {
		List<String> get = meta.getLore();
		if (get == null) get = new ArrayList<>();
		get.addAll(lore);
		meta.setLore(get);
		return meta;
	}

}
