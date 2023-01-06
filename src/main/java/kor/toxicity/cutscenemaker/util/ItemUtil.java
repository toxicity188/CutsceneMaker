package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
}
