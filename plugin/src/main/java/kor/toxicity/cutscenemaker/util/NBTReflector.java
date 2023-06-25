package kor.toxicity.cutscenemaker.util;

import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.nms.NMSChecker;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SuppressWarnings("JavaReflectionInvocation")
public final class NBTReflector {

	private static volatile NBTReflector reflector;

	private interface SetInternalTag {
		ItemStack invoke(ItemStack item, String internal, String key);
		ItemStack invoke(ItemStack item, Map<String,String> key);
	}
	private interface ReadInternalTag {
		String invoke(ItemStack item, String key);
	}

	private SetInternalTag setInternalTag;
	private ReadInternalTag readInternalTag;

	private NBTReflector() {
		try {
			Method getTag, setTag, hasTag, hasKey, getString, setString, asNMSCopy, asCraftMirror;
			Class<?> nmsItemStack = getNMSClass("net.minecraft.server", "ItemStack");
			Class<?> nbtTagCompound = getNMSClass("net.minecraft.server", "NBTTagCompound");
			Class<?> craftItemStack = getNMSClass("org.bukkit.craftbukkit", "inventory.CraftItemStack");

			getTag = nmsItemStack.getDeclaredMethod("getTag");
			setTag = nmsItemStack.getDeclaredMethod("setTag", nbtTagCompound);
			hasTag = nmsItemStack.getDeclaredMethod("hasTag");

			hasKey = nbtTagCompound.getDeclaredMethod("hasKey", String.class);
			getString = nbtTagCompound.getDeclaredMethod("getString", String.class);
			setString = nbtTagCompound.getDeclaredMethod("setString", String.class, String.class);

			asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
			asCraftMirror = craftItemStack.getDeclaredMethod("asCraftMirror", nmsItemStack);

			setInternalTag = new SetInternalTag() {
				@Override
				public ItemStack invoke(ItemStack item, String internal, String key) {
					try {
						Object o1 = asNMSCopy.invoke(null, item);
						Object o2 = getTag.invoke(o1);
						setString.invoke(o2, internal, key);
						setTag.invoke(o1, o2);
						return (ItemStack) asCraftMirror.invoke(null, o1);
					} catch (Exception e) {
						e.printStackTrace();
						return item;
					}
				}

				@Override
				public ItemStack invoke(ItemStack item, Map<String, String> key) {
					try {
						Object o1 = asNMSCopy.invoke(null, item);
						Object o2 = getTag.invoke(o1);
						key.forEach((a, b) -> {
							try {
								setString.invoke(o2, a, b);
							} catch (Exception ignored) {
							}
						});
						setTag.invoke(o1, o2);
						return (ItemStack) asCraftMirror.invoke(null, o1);
					} catch (Exception e) {
						e.printStackTrace();
						return item;
					}
				}
			};
			readInternalTag = (item,key) -> {
				try {
					Object itemStack = asNMSCopy.invoke(null, item);
					if (!(boolean) hasTag.invoke(itemStack)) return "";
					Object t = getTag.invoke(itemStack);
					if (!(boolean) hasKey.invoke(t, key)) return "";
					return (String) getString.invoke(t, key);
				} catch (Exception e) {
					e.printStackTrace();
					return "";
				}
			};
		} catch (Exception ex) {
			try {
				Class<?> persistenceDataType = Class.forName("org.bukkit.persistence.PersistentDataType");
				Class<?> persistentDataContainer = Class.forName("org.bukkit.persistence.PersistentDataContainer");

				Method method = Class.forName("org.bukkit.persistence.PersistentDataHolder").getDeclaredMethod("getPersistentDataContainer");

				Method get = persistentDataContainer.getDeclaredMethod("get", NamespacedKey.class,persistenceDataType);
				Method set = persistentDataContainer.getDeclaredMethod("set", NamespacedKey.class,persistenceDataType,Object.class);
				Method fromString = NamespacedKey.class.getDeclaredMethod("fromString",String.class);

				Object stringType = persistenceDataType.getDeclaredField("STRING").get(null);

				setInternalTag = new SetInternalTag() {
					@Override
					public ItemStack invoke(ItemStack item, String internal, String key) {
						try {
							ItemStack i = item.clone();
							ItemMeta meta = i.getItemMeta();
							Object o = method.invoke(meta);
							set.invoke(o,fromString.invoke(null,internal),stringType,key);
							i.setItemMeta(meta);
							return i;
						} catch (Exception e) {
							e.printStackTrace();
							return item;
						}
					}

					@Override
					public ItemStack invoke(ItemStack item, Map<String, String> key) {
						try {
							ItemStack i = item.clone();
							ItemMeta meta = i.getItemMeta();
							Object o = method.invoke(meta);
							for (Map.Entry<String, String> stringStringEntry : key.entrySet()) {
								set.invoke(o,fromString.invoke(null,stringStringEntry.getKey()),stringType,stringStringEntry.getValue());
							}
							i.setItemMeta(meta);
							return i;
						} catch (Exception e) {
							e.printStackTrace();
							return item;
						}
					}
				};
				readInternalTag = (i,k) -> {
					try {
						ItemMeta meta = i.getItemMeta();
						if (meta == null) return "";
						Object o = method.invoke(meta);
						String s = (String) get.invoke(o,fromString.invoke(null,k),stringType);
						return (s != null) ? s : "";
					} catch (Exception e) {
						e.printStackTrace();
						return "";
					}
				};
			} catch (Exception e2) {
				e2.printStackTrace();
				CutsceneMaker.warn("Unable to load the NBT Class.");
			}
		}
	}
	private static Class<?> getNMSClass(String pack, String name) throws ClassNotFoundException {
		return Class.forName(pack + "." + NMSChecker.getVersion() + "." + name);
	}

	public static ItemStack setInternalTag(ItemStack item, String internal, String key) {
		if (reflector == null) reflector = new NBTReflector();
		return reflector.setInternalTag.invoke(item,internal,key);
	}
	public static ItemStack setInternalTag(ItemStack item, Map<String,String> key) {
		if (reflector == null) reflector = new NBTReflector();
		return reflector.setInternalTag.invoke(item,key);
	}
	public static String readInternalTag(ItemStack item, String key) {
		if (reflector == null) reflector = new NBTReflector();
		return reflector.readInternalTag.invoke(item,key);
		
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
			ItemStack stack = config.getItemStack("i", null);
			LocalDateTime time = LocalDateTime.parse(config.getString("t"));
			int left = config.getInt("h",-1);
			return (stack != null
					&& stack.getType() != Material.AIR
					&& (left < 0 || ChronoUnit.HOURS.between(time,LocalDateTime.now()) < left)
			) ? new StorageItem(stack, time, left) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
