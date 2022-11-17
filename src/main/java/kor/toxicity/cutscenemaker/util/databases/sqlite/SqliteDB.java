package kor.toxicity.cutscenemaker.util.databases.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class SqliteDB {

    protected final String EXTENSION = ".sqlite";

    public Connection getConnection(String dir) throws SQLException, IOException {
        File file = new File(dir+EXTENSION);
        if (!file.exists()) file.createNewFile();
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }
}
