package kor.toxicity.cutscenemaker.util.databases.sqlite;

import kor.toxicity.cutscenemaker.util.databases.UserDB;
import org.bukkit.entity.Player;

public class SqliteUserDB implements UserDB {

    @Override
    public void save(Player player) {
        //TODO make a save code
    }

    @Override
    public void load(Player player) {

    }

    @Override
    public boolean isLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void initialize() {

    }
}
