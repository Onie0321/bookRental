package com.librorent.database;

import java.sql.*;

public class DBConnection {
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:librorent.db"; // SQLite file
        return DriverManager.getConnection(url);
    }

    public static void setupDatabase() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                role TEXT
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);

            // Insert admin if not exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username='admin'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'librarian')");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
