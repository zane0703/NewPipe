package org.zane.newpipe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.sqlite.SQLiteOpenMode;

public class Subscribed {

    private final Database db;

    Subscribed(Database db) {
        this.db = db;
    }

    void create(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE subscribed(service_id INT NOT NULL, url TEXT NOT NULL, name TEXT NOT NULL,  PRIMARY KEY(service_id, url));"
            );
        }
    }

    public void add(int serviceId, String url, String name) {
        try (Connection conn = db.connect(SQLiteOpenMode.READWRITE)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO subscribed(service_id, url, name) VALUES(?, ?, ?);"
                )
            ) {
                stmt.setInt(1, serviceId);
                stmt.setString(2, url);
                stmt.setString(3, name);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ChannelInfoItem> getAll(int serviceId) {
        try (Connection conn = db.connect(SQLiteOpenMode.READONLY)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT url, name FROM subscribed WHERE service_id=?;"
                )
            ) {
                stmt.setInt(1, serviceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    ArrayList<ChannelInfoItem> channelInfoItems =
                        new ArrayList<>();
                    while (rs.next()) {
                        channelInfoItems.add(
                            new ChannelInfoItem(
                                serviceId,
                                rs.getString("url"),
                                rs.getString("name")
                            )
                        );
                    }
                    return channelInfoItems;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isExist(int serviceId, String url) {
        try (Connection conn = db.connect(SQLiteOpenMode.READONLY)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM subscribed WHERE service_id=? AND url=?) AS exist;"
                )
            ) {
                stmt.setInt(1, serviceId);
                stmt.setString(2, url);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("exist");
                    } else {
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int serviceId, String url) {
        try (Connection conn = db.connect(SQLiteOpenMode.READWRITE)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM subscribed WHERE service_id=? AND url=?;"
                )
            ) {
                stmt.setInt(1, serviceId);
                stmt.setString(2, url);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
