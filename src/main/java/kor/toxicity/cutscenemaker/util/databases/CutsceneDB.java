package kor.toxicity.cutscenemaker.util.databases;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.util.vars.Vars;
import kor.toxicity.cutscenemaker.util.vars.VarsContainer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.sql.*;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CutsceneDB  {
    private static final UserDB CSV_DB = new UserDB() {
        @Override
        public VarsContainer read(OfflinePlayer player, JavaPlugin plugin) {
            VarsContainer container = new VarsContainer();
            String name = plugin.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv";
            File create = new File(name);
            if (!create.exists()) {
                try {
                    create.createNewFile();
                    CutsceneMaker.debug("unable to find " + player.getName() + "'s data file. so create a new data file.");
                } catch (IOException ignored) {}
            }
            try (FileReader file = new FileReader(name); CSVReader reader = new CSVReader(file)) {
                reader.forEach(t -> {
                    if (t.length >= 2) container.getVars().put(t[0],new Vars(t[1]));
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
            String name = plugin.getDataFolder().getAbsolutePath() + "\\User\\" + player.getUniqueId().toString() + ".csv";
            try (FileWriter file = new FileWriter(name); CSVWriter writer = new CSVWriter(file)) {
                writer.writeAll(
                        container.getVars().entrySet().stream()
                        .filter(e -> !e.getKey().startsWith("_"))
                        .map(e -> new String[] {e.getKey(),e.getValue().getVar()})
                        .collect(Collectors.toList()));
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
                con = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database + "?useSSL=false", name, password);
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
                create.execute("CREATE TABLE IF NOT EXISTS " + name + " (k VARCHAR(256) UNIQUE NOT NULL,v VARCHAR(256) NOT NULL);");
            } catch (SQLException ignored) {}
            try (PreparedStatement getter = con.prepareStatement("SELECT * FROM " + name + ";")) {
                ResultSet set = getter.executeQuery();
                while (set.next()) container.getVars().put(set.getString("k"), new Vars(set.getString("v")));
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
                if (k.charAt(0) != '_') {
                    try (PreparedStatement statement = con.prepareStatement("INSERT INTO " + name + " VALUES(?,?);")) {
                        statement.setString(1, k);
                        statement.setString(2, v.getVar());
                        statement.executeUpdate();
                    } catch (SQLException ignored) {}
                }
            });
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
            container.setTask(Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    () -> save(player,plugin,container),
                    300,
                    300
            ));
            return container;
        }
        default void stop(OfflinePlayer player, JavaPlugin plugin, VarsContainer container) {
            save(player,plugin,container);
            BukkitTask task = container.getTask();
            if (task != null) task.cancel();
        }
    }

}
