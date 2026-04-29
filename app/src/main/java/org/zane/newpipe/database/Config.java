package org.zane.newpipe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.sqlite.SQLiteOpenMode;

public class Config {

    private final Database db;

    Config(Database db) {
        this.db = db;
    }

    void create(Connection conn) throws SQLException {
        try (Statement stmp = conn.createStatement()) {
            stmp.execute(
                "CREATE TABLE config( db_version INTEGER NOT NULL DEFAULT 1 )"
            );
            stmp.execute("INSERT INTO config(db_version) VALUES(1);");
        }
    }

    public int getVersion() {
        try (Connection conn = db.connect(SQLiteOpenMode.READONLY)) {
            Statement stmt = conn.createStatement();
            try (
                ResultSet rs = stmt.executeQuery(
                    "SELECT db_version FROM config;"
                )
            ) {
                if (rs.next()) {
                    return rs.getInt("db_version");
                } else {
                    return 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setVersion(int version) {
        try (Connection conn = db.connect(SQLiteOpenMode.READWRITE)) {
            try (
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE config SET db_version=?;"
                )
            ) {
                stmt.setInt(1, version);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
