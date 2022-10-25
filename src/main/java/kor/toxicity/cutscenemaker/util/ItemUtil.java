package kor.toxicity.cutscenemaker.util;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class ItemUtil {


	public static ItemStack setInternalTag(ItemStack item, String key) {
		return setInternalTag(item,"internalTag",key);
	}
	public static ItemStack setInternalTag(ItemStack item, String internal, String key) {
		try {
			
			Method setstring = getNBTTagCompound().getDeclaredMethod("setString", String.class, String.class);
			
			Object o1 = asNMSCopy(item);
			Object o2 = getTag(o1);
			setstring.invoke(o2, internal, key);
			setTag(o1, o2);
			return asCraftMirror(o1);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static ItemStack setInternalTag(ItemStack item, Map<String,String> key) {
		try {

			Method setstring = getNBTTagCompound().getDeclaredMethod("setString", String.class, String.class);

			Object o1 = asNMSCopy(item);
			Object o2 = getTag(o1);
			key.forEach((a, b) -> {
				try {
					setstring.invoke(o2, a, b);
				} catch (Exception ignored) {}
			});
			setTag(o1, o2);
			return asCraftMirror(o1);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static ItemStack setShiny(ItemStack item) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(Enchantment.DURABILITY,1,true);
		item.setItemMeta(meta);
		return item;
	}

	public static String readInternalTag(ItemStack item) {
		return readInternalTag(item,"internalTag");
	}
	public static String readInternalTag(ItemStack item, String key) {
		if (!hasTag(asNMSCopy(item))) return "";
		Object t = getTag(asNMSCopy(item));
		if (!hasKey(t,key)) return "";
		return getString(t,key);
		
	}
	public static Map<String,String> readInternalTag(ItemStack item, List<String> key) {
		if (!hasTag(asNMSCopy(item))) return null;
		Map<String,String> ret = new WeakHashMap<>();
		Object t = getTag(asNMSCopy(item));
		key.forEach(s -> {
			if (hasKey(t,s)) ret.put(s,getString(t,s));
		});
		return ret;

	}

	//NMS NBTTagCompound
	private static Class<?> getNBTTagCompound() {return getNMSClass("net.minecraft.server","NBTTagCompound");}
	private static boolean hasKey(Object item,String key) {
		try {return (boolean) getNBTTagCompound().getDeclaredMethod("hasKey", String.class).invoke(item, key);} catch (Exception e) {return false;}
	}
	private static String getString(Object item,String key) {
		try {return (String) getNBTTagCompound().getDeclaredMethod("getString", String.class).invoke(item, key);} catch (Exception e) {return null;}
	}
	
	//NMS ItemStack
	private static Class<?> getNMSItemStack() {return getNMSClass("net.minecraft.server","ItemStack");}
	private static Object getTag(Object item) {
		try {return getNMSItemStack().getDeclaredMethod("getTag").invoke(item);} catch (Exception e) {return null;}
	}
	private static Object setTag(Object item, Object compound) {
		try {return getNMSItemStack().getDeclaredMethod("setTag",getNBTTagCompound()).invoke(item, compound);} catch (Exception e) {return null;}
	}
	private static boolean hasTag(Object item) {
		try {return (boolean) getNMSItemStack().getDeclaredMethod("hasTag").invoke(item);} catch (Exception e) {return false;}
	}
	
	//NMS CraftItemStack
	private static Class<?> getCraftItemStack() {return getNMSClass("org.bukkit.craftbukkit","inventory.CraftItemStack");}
	private static Object asNMSCopy(ItemStack item) {
		try {return getCraftItemStack().getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, item);} catch (Exception e) {return null;}
	}
	private static ItemStack asCraftMirror(Object item) {
		try {return (ItemStack) getCraftItemStack().getDeclaredMethod("asCraftMirror", getNMSItemStack()).invoke(null, item);} catch (Exception e) {return null;}
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
}
