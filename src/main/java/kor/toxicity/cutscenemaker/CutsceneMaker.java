package kor.toxicity.cutscenemaker;

import com.google.gson.JsonParser;
import kor.toxicity.cutscenemaker.data.*;
import kor.toxicity.cutscenemaker.entities.EntityManager;
import kor.toxicity.cutscenemaker.events.ActionReloadEndEvent;
import kor.toxicity.cutscenemaker.events.ActionReloadStartEvent;
import kor.toxicity.cutscenemaker.quests.QuestData;
import kor.toxicity.cutscenemaker.util.ConfigLoad;
import kor.toxicity.cutscenemaker.util.EvtUtil;
import kor.toxicity.cutscenemaker.util.InvUtil;
import kor.toxicity.cutscenemaker.util.StorageItem;
import kor.toxicity.cutscenemaker.util.databases.CutsceneDB;
import kor.toxicity.cutscenemaker.util.gui.CallbackManager;
import kor.toxicity.cutscenemaker.util.gui.GuiAdapter;
import kor.toxicity.cutscenemaker.util.gui.GuiRegister;
import kor.toxicity.cutscenemaker.util.gui.MouseButton;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class CutsceneMaker extends JavaPlugin {

    public static final String NAME = "[CutsceneMaker]";
    public static final String BUILD_VERSION = "2023-04-21";
    public static final int BSTATS_ID = 18237;
    private static final List<Runnable> LATE_CHECK = new ArrayList<>();
    public void addLateCheck(Runnable runnable) {
        LATE_CHECK.add(runnable);
    }

    private final Set<Reloadable> reload = new LinkedHashSet<>();
    private static CutsceneManager manager;
    private static boolean debug;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEnable() {
        getDataFolder().mkdir();
        new File(getDataFolder(),"User").mkdir();
        setupDB();

        CutsceneCommand command = new CutsceneCommand(this);
        manager = new CutsceneManager(this);
        reload.add(command::unregister);
        reload.add(new GuiRegister(this));
        reload.add(new CallbackManager(this));
        reload.add(() -> {
            CutsceneConfig.getInstance().load(this);
            debug = CutsceneConfig.getInstance().isDebug();
        });
        reload.add(new EventData(this));
        reload.add(new ItemData(this));
        reload.add(new LocationData(this));
        reload.add(new QuestData(this));
        reload.add(new ActionData(this));

        //setup command
        getCommand("cutscene").setExecutor(command);
        getCommand("temp").setExecutor((sender, command1, label, args) -> {
            if (sender instanceof Player) {
                tempStorage((Player) sender);
            } else send(sender,"this command is player only.");
            return true;
        });


        load(t -> send("Plugin enabled."));
    }

    public void tempStorage(Player player) {
        Inventory inv = InvUtil.create(CutsceneConfig.getInstance().getTempStorageName().print(player),6);
        List<StorageItem> tempStorage = manager.getVars(player).getTempStorage();
        GuiRegister.registerNewGui(new GuiAdapter(player,inv) {
            @Override
            public void initialize() {
                for (int i = 0; i < 53; i++) {
                    ItemStack stack;
                    if (i < tempStorage.size()) {
                        StorageItem storageItem = tempStorage.get(i);
                        stack = storageItem.getStack().clone();
                        ItemMeta meta = stack.getItemMeta();

                        LocalDateTime localTime = storageItem.getTime();

                        String timeStr = ChatColor.GOLD + ChatColor.ITALIC.toString() + "Day: " + ChatColor.YELLOW + localTime.getYear() + "-" + localTime.getMonthValue() + "-" + localTime.getDayOfMonth();
                        List<String> time = (storageItem.getLeftHour() > 0) ? Arrays.asList(
                                timeStr,
                                ChatColor.GOLD + ChatColor.ITALIC.toString() + "Left: " + ChatColor.YELLOW + (storageItem.getLeftHour() - ChronoUnit.HOURS.between(localTime,LocalDateTime.now())) + "h"
                        ) : Collections.singletonList(timeStr);

                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            lore.add("");
                            lore.addAll(time);
                        } else lore = time;
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                    } else stack = null;

                    inv.setItem(i,stack);
                }
            }

            @Override
            public void onClick(ItemStack item, int slot, MouseButton button, boolean isPlayerInventory) {
                if (isPlayerInventory || item.getType() == Material.AIR) return;
                StorageItem t = tempStorage.get(slot);
                if (t == null) return;
                ItemStack stack = t.getStack();
                if (InvUtil.storage(player,stack) < stack.getAmount()) {
                    return;
                }
                InvUtil.give(player,stack);
                tempStorage.remove(t);
                initialize();
                player.updateInventory();
            }
        });
    }
    private void setupDB() {
        ConfigLoad load = readResourceFile("database");
        switch (load.getString("using","csv")) {
            default:
            case "csv":
                CutsceneDB.setToDefault();
                break;
            case "mysql":
                CutsceneDB.useMySql(
                        load.getString("mysql-host","localhost"),
                        load.getString("mysql-database-name","CutsceneMaker"),
                        load.getString("mysql-user-name","root"),
                        load.getString("mysql-user-password",null)
                );
                break;
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            VarsContainer container = manager.getVars(p);
            if (container != null) CutsceneDB.save(p,this,container);
        });
        send("Plugin disabled.");
    }

    public void load(Consumer<Long> callback) {
        ActionReloadStartEvent event = new ActionReloadStartEvent();
        EvtUtil.call(event);
        Bukkit.getScheduler().runTaskAsynchronously(this,() -> {
            try {
                long time = System.currentTimeMillis();
                event.run();
                reload.forEach(Reloadable::reload);
                LATE_CHECK.forEach(Runnable::run);
                LATE_CHECK.clear();
                long time2 = System.currentTimeMillis() - time;
                Bukkit.getScheduler().runTask(this,() -> {
                    if (callback != null) callback.accept(time2);
                    EvtUtil.call(new ActionReloadEndEvent());
                });
            } catch (Exception e) {
                warn("Error has occurred while reloading: " + e.getMessage());
            }
            try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse response = client.execute(new HttpGet("https://api.github.com/repos/toxicity188/CutsceneMaker/tags?per_page=1"))) {
                String version = new JsonParser().parse(new BufferedReader(new InputStreamReader(response.getEntity().getContent()))).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                if (!BUILD_VERSION.equals(version)) {
                    warn("New version found: " + version);
                    warn("Download: https://github.com/toxicity188/CutsceneMaker/releases/tag/" + version);
                }
            } catch (Exception e) {
                warn("fail to find the updated version.");
            }
        });
    }

    public static void send(String s) {
        Bukkit.getConsoleSender().sendMessage(NAME + " " + s);
    }
    public static void send(CommandSender sender, String s) {
        sender.sendMessage(ChatColor.AQUA + NAME + ChatColor.WHITE + " " + s);
    }
    public static void warn(String s) {
        Bukkit.getLogger().warning(NAME + " " + s);
    }
    public static void debug(String s) {
        if (debug) Bukkit.getLogger().info(NAME + " Debug: " + s);
    }

    public CutsceneManager getManager() {
        return manager;
    }

    public ConfigLoad readResourceFile(String file) {
        try {
            if (!new File(getDataFolder(), file + ".yml").exists()) saveResource(file + ".yml",false);
            return new ConfigLoad(this,file + ".yml","");
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    public ConfigLoad read(String dict) {
        return new ConfigLoad(new File(getDataFolder(), dict),"");
    }

    public static void addTempItem(OfflinePlayer player, ItemStack stack) {
        addTempItem(player,stack,-1);
    }
    public static void addTempItem(OfflinePlayer player, List<ItemStack> stack) {
        addTempItem(player,stack,-1);
    }
    public static void addTempItem(OfflinePlayer player, ItemStack stack, int left) {
        addTempItem(player,Collections.singletonList(stack),left);
    }
    public static void addTempItem(OfflinePlayer player, List<ItemStack> stack, int left) {
        List<StorageItem> storage = stack.stream().map(s -> new StorageItem(s, LocalDateTime.now(), left)).collect(Collectors.toList());
        VarsContainer container;
        if (player instanceof Player) {
             container = manager.getVars((Player) player);
            if (container == null) return;
            container.getTempStorage().addAll(storage);
        } else {
            container = CutsceneDB.read(player,manager.getPlugin());
            container.getTempStorage().addAll(storage);
            CutsceneDB.save(player,manager.getPlugin(),container);
        }
    }
    public static void removeVars(Player player, String key) {
        manager.getVars(player).remove(key);
    }
    public static Vars getVars(Player player, String key) {
        return manager.getVars(player,key);
    }
    public static Vars[] getVarsArray(Player player, String node) {
        return manager.getVars(player).getVars().entrySet().stream().filter(e -> e.getKey().startsWith(node)).map(Map.Entry::getValue).toArray(Vars[]::new);
    }
    public static boolean isSet(Player player, String key) {
        return manager.isSet(player,key);
    }

}
