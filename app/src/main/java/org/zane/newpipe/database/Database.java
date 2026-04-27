package org.zane.newpipe.database;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class Database {

    private static final String dbFileName = "config.db";
    private final String dbFilePath;
    private Config config;
    private SearchHistory searchHistory;

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
        boolean isDBExist;
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                Path dbFile = path.resolve(dbFileName);
                dbFilePath = dbFile.toAbsolutePath().toString();
                isDBExist = Files.exists(dbFile);
                if (isDBExist) {
                    if (Files.isDirectory(dbFile)) {
                        throw new RuntimeException(
                            "the config file " +
                                pathStr +
                                " is a directory not a file"
                        );
                    }
                }
            } else {
                throw new RuntimeException(
                    "the config directory path " +
                        pathStr +
                        " is a file not directory"
                );
            }
        } else {
            Files.createDirectory(path);
            dbFilePath = path.resolve(dbFileName).toAbsolutePath().toString();

            isDBExist = false;
        }
        System.out.println(dbFilePath);

        config = new Config(this);
        searchHistory = new SearchHistory(this);
        if (!isDBExist) {
            try {
                try (Connection conn = connect(SQLiteOpenMode.CREATE)) {
                    conn.setAutoCommit(false);
                    config.create(conn);
                    searchHistory.create(conn);
                    conn.commit();
                }
            } catch (SQLException e) {
                Files.delete(Paths.get(dbFileName));
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
            "jdbc:sqlite:" + dbFilePath,
            config.toProperties()
        );
    }

    public SearchHistory getSearchHistory() {
        return searchHistory;
    }
}
