package org.zane.newpipe.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class Database {

    private static final String dbFileName = "config.db";
    private final String dbFilePathStr;
    private final Config config;
    private final SearchHistory searchHistory;
    private final Subscribed subscribed;

    public Database() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String pathStr;
        if (os.contains("win")) {
            // Use APPDATA for Windows-specific application data
            pathStr = System.getenv("APPDATA") + "\\NewPipe";
        } else if (os.contains("mac")) {
            // Standard macOS Application Support directory
            pathStr =
                System.getProperty("user.home") +
                "/Library/Application Support/NewPipe";
        } else {
            // Standard Linux/Unix config directory or hidden home folder
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isEmpty()) {
                pathStr = xdgConfig + "/NewPipe";
            } else {
                pathStr = System.getProperty("user.home") + "/.config/NewPipe";
            }
        }

        Path path = Paths.get(pathStr);
        Path dbFilePath;
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                dbFilePath = path.resolve(dbFileName);
            } else {
                throw new RuntimeException(
                    "the config directory path " +
                        pathStr +
                        " is a file not directory"
                );
            }
        } else {
            Files.createDirectory(path);
            dbFilePath = path.resolve(dbFileName);
        }
        this(dbFilePath);
    }

    public Database(Path dbFilePath) throws IOException {
        dbFilePathStr = dbFilePath.toAbsolutePath().toString();
        boolean isDBExist = Files.exists(dbFilePath);
        if (isDBExist) {
            if (Files.isDirectory(dbFilePath)) {
                throw new RuntimeException(
                    "the config file " +
                        dbFilePathStr +
                        " is a directory not a file"
                );
            }
        }
        config = new Config(this);
        searchHistory = new SearchHistory(this);
        subscribed = new Subscribed(this);
        if (!isDBExist) {
            try {
                try (Connection conn = connect(SQLiteOpenMode.CREATE)) {
                    conn.setAutoCommit(false);
                    config.create(conn);
                    searchHistory.create(conn);
                    subscribed.create(conn);
                    conn.commit();
                }
            } catch (SQLException e) {
                Files.delete(dbFilePath);
                throw new RuntimeException(e);
            }
        }
    }

    Connection connect(SQLiteOpenMode mode) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setOpenMode(mode);
        if (mode == SQLiteOpenMode.READONLY) {
            config.setReadOnly(true);
        }
        return DriverManager.getConnection(
            "jdbc:sqlite:" + dbFilePathStr,
            config.toProperties()
        );
    }

    public SearchHistory getSearchHistory() {
        return searchHistory;
    }

    public Subscribed getSubscribed() {
        return subscribed;
    }
}
