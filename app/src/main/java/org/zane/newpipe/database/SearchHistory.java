package org.zane.newpipe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.sqlite.SQLiteOpenMode;

public class SearchHistory {

    private Database db;

    SearchHistory(Database db) {
        this.db = db;
    }

    void create(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE search_history(search_query TEXT UNIQUE NOT NULL);"
            );
        }
    }

    public List<String> get(String searchQuery) {
        try (Connection conn = db.connect(SQLiteOpenMode.READONLY)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT search_query FROM search_history WHERE search_query LIKE ? || '%';"
                )
            ) {
                stmt.setString(1, searchQuery);
                try (ResultSet rs = stmt.executeQuery()) {
                    ArrayList<String> searchHistoryList = new ArrayList<>();
                    while (rs.next()) {
                        searchHistoryList.add(rs.getString("search_query"));
                    }
                    return searchHistoryList;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(String searchQuery) {
        try (Connection conn = db.connect(SQLiteOpenMode.READWRITE)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO search_history(search_query) VALUES(?);"
                )
            ) {
                stmt.setString(1, searchQuery);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String searchQuery) {
        try (Connection conn = db.connect(SQLiteOpenMode.READWRITE)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM search_history WHERE search_query=?;"
                )
            ) {
                stmt.setString(1, searchQuery);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
