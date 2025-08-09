package mx.edu.utch.proyectofinal1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class Database {
    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + "/simon.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static void init() {
        try {
            Path dir = Path.of(DB_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            try (Connection conn = DriverManager.getConnection(URL);
                 Statement st = conn.createStatement()) {

                st.execute("""
                    CREATE TABLE IF NOT EXISTS players(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE
                    );
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS scores(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_id INTEGER NOT NULL,
                        score INTEGER NOT NULL,
                        played_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(player_id) REFERENCES players(id)
                    );
                """);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando BD: " + e.getMessage(), e);
        }
    }

    public static int upsertPlayer(String name) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO players(name) VALUES(?)")) {
                ps.setString(1, name);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM players WHERE name=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : -1;
                }
            }
        }
    }

    public static void saveScore(int playerId, int score) throws SQLException {
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO scores(player_id, score) VALUES(?, ?)")) {
            ps.setInt(1, playerId);
            ps.setInt(2, score);
            ps.executeUpdate();
        }
    }
}
