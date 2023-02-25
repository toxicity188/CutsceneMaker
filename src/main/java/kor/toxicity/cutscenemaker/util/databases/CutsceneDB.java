package kor.toxicity.cutscenemaker.util.databases;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import kor.toxicity.cutscenemaker.CutsceneConfig;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.ItemUtil;
import kor.toxicity.cutscenemaker.util.StorageItem;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CutsceneDB  {
    private static final UserDB CSV_DB = new UserDB() {
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public VarsContainer read(OfflinePlayer player, JavaPlugin plugin) {
            VarsContainer container = new VarsContainer();
            File create = new File(new File(plugin.getDataFolder(),"User"), player.getUniqueId().toString() + ".csv");
            if (!create.exists()) {
                try {
                    create.createNewFile();
                    CutsceneMaker.debug("unable to find " + player.getName() + "'s data file. so create a new data file.");
                } catch (IOException ignored) {}
            }
            try (FileReader file = new FileReader(create); CSVReader reader = new CSVReader(file)) {
                List<StorageItem> decodedItems = container.getTempStorage();
                reader.forEach(t -> {
                    if (t.length >= 2) {
                        if (t[0].startsWith("temp.")) {
                            StorageItem decodedItem = ItemUtil.decode(t[1]);
                            if (decodedItem != null) decodedItems.add(decodedItem);
                        }
                        else container.getVars().put(t[0],new Vars(t[1]));
                    }
                });
                CutsceneMaker.debug(player.getName() + "'s data loaded.");
            } catch (Exception e) {
                e.printStackTrace();
                CutsceneMaker.warn("Unable to load the user data of " + player.getName());
            }
            return container;
        }

        @Override
        public void save(OfflinePlayer player, JavaPlugin plugin, VarsContainer container) {
            File create = new File(new File(plugin.getDataFolder(),"User"), player.getUniqueId().toString() + ".csv");
            try (FileWriter file = new FileWriter(create); CSVWriter writer = new CSVWriter(file)) {
                container.getVars().entrySet().stream()
                        .filter(e -> e.getKey().charAt(0) != '_' && !e.getValue().getVar().equals("<none>"))
                        .map(e -> new String[] {e.getKey(),e.getValue().getVar()})
                        .forEach(writer::writeNext);
                int i = 0;
                for (StorageItem decodedItem : container.getTempStorage()) {
                    writer.writeNext(new String[] {"temp." + (++i), ItemUtil.encode(decodedItem)});
                }
                CutsceneMaker.debug(player.getName() + "'s user data saved.");
            } catch (Exception e) {
                e.printStackTrace();
                CutsceneMaker.warn("Unable to save the user data of " + player.getName());
            }
        }
    };

    private static class MySqlDB implements UserDB, AutoCloseable {
        private final Connection con;
        private MySqlDB(String host, String database, String name, String password) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                con = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database + "?autoReconnect=true&useSSL=false", name, password);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        @Override
        public VarsContainer read(OfflinePlayer player, JavaPlugin plugin) {
            VarsContainer container = new VarsContainer();
            String name = player.getUniqueId().toString().replace('-','_');
            try (Statement create = con.createStatement()) {
                create.execute("CREATE TABLE IF NOT EXISTS " + name + " (k VARCHAR(256) UNIQUE NOT NULL,v TEXT NOT NULL);");
            } catch (SQLException ignored) {}
            try (PreparedStatement getter = con.prepareStatement("SELECT * FROM " + name + ";")) {
                ResultSet set = getter.executeQuery();
                while (set.next()) {
                    String key = set.getString("k");
                    String value = set.getString("v");
                    List<StorageItem> items = container.getTempStorage();
                    if (key.startsWith("temp.")) {
                        StorageItem item = ItemUtil.decode(value);
                        if (item != null) items.add(item);
                    }
                    else container.getVars().put(key, new Vars(value));
                }
            } catch (SQLException ignored) {}
            return container;
        }

        @Override
        public void save(OfflinePlayer player, JavaPlugin plugin, VarsContainer container) {
            String name = player.getUniqueId().toString().replace('-','_');
            try (Statement create = con.createStatement()) {
                create.execute("TRUNCATE " + name + ";");
            } catch (SQLException ignored) {}
            container.getVars().forEach((k,v) -> {
                if (k.charAt(0) != '_' && !v.getVar().equals("<none>")) {
                    try (PreparedStatement statement = con.prepareStatement("INSERT INTO " + name + " VALUES(?,?);")) {
                        statement.setString(1, k);
                        statement.setString(2, v.getVar());
                        statement.executeUpdate();
                    } catch (SQLException ignored) {}
                }
            });
            int i = 0;
            for (StorageItem storageItem : container.getTempStorage()) {
                try (PreparedStatement statement = con.prepareStatement("INSERT INTO " + name + " VALUES(?,?);")) {
                    statement.setString(1, "temp." + (++i));
                    statement.setString(2, ItemUtil.encode(storageItem));
                    statement.executeUpdate();
                } catch (SQLException ignored) {}
            }

        }

        @Override
        public void close() throws Exception {
            con.close();
        }
    }
    private static UserDB dbAccessor = CSV_DB;

    private static void close() {
        if (dbAccessor instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dbAccessor).close();
            } catch (Exception ignored) {}
        }
    }
    public static void setToDefault() {
        close();
        dbAccessor = CSV_DB;
    }
    public static boolean isDefault() {
        return dbAccessor == CSV_DB;
    }
    public static void useMySql(@NotNull String host, @NotNull String database, @NotNull String name, @NotNull String password) {
        close();
        try {
            dbAccessor = new MySqlDB(
                    Objects.requireNonNull(host),
                    Objects.requireNonNull(database),
                    Objects.requireNonNull(name),
                    Objects.requireNonNull(password)
            );
        } catch (Exception e) {
            dbAccessor = CSV_DB;
            CutsceneMaker.warn("unable to connect MySql");
        }
    }

    public static VarsContainer read(OfflinePlayer player, JavaPlugin plugin) {
        return dbAccessor.read(player,plugin);
    }
    public static VarsContainer load(OfflinePlayer player, JavaPlugin plugin) {
        return dbAccessor.load(player,plugin);
    }
    public static void save(OfflinePlayer player, JavaPlugin plugin, VarsContainer container) {
        dbAccessor.stop(player,plugin,container);
    }

    private interface UserDB {
        VarsContainer read(OfflinePlayer player, JavaPlugin plugin);
        void save(OfflinePlayer player, JavaPlugin plugin, VarsContainer container);


        default VarsContainer load(OfflinePlayer player, JavaPlugin plugin) {
            VarsContainer container = read(player,plugin);
            int autoSave = CutsceneConfig.getInstance().getAutoSaveTime() * 20;
            container.addTask(Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    () -> save(player,plugin,container),
                    autoSave,
                    autoSave
            ));
            container.addTask(Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    () -> container.getTempStorage().removeIf(item -> item.isTemp() && item.getLeft() - TextUtil.calculateDay(item.getYear(), item.getMonth(), item.getDay()) < 0),
                    60 * 20,
                    60 * 20
            ));
            return container;
        }
        default void stop(OfflinePlayer player, JavaPlugin plugin, VarsContainer container) {
            save(player,plugin,container);
            container.stopTask();
        }
    }

}
