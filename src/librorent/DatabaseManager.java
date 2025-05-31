package librorent;

import java.sql.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JOptionPane;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String DB_URL = "jdbc:sqlite:librorent.db";
    private static final String DRIVER = "org.sqlite.JDBC";
    private boolean initialized = false;
    private boolean tablesCreated = false;
    
    private DatabaseManager() {
        try {
            Class.forName(DRIVER);
            System.out.println("SQLite JDBC Driver loaded successfully");
            // Initialize database on first instance creation
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading SQLite JDBC Driver: " + e.getMessage());
            throw new RuntimeException("Failed to load SQLite JDBC Driver", e);
        }
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    private synchronized void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Enable foreign keys
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            
            // Create tables if they don't exist
            createTables(conn);
            
            initialized = true;
            System.out.println("Database initialization completed successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
    
    private synchronized void createTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Create users table with all required columns
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "full_name TEXT NOT NULL," +
                "email TEXT NOT NULL," +
                "phone TEXT," +
                "admin_id INTEGER," +
                "role TEXT DEFAULT 'Member'," +
                "FOREIGN KEY (admin_id) REFERENCES users(id)" +
                ")");
            
            // Check if we need to migrate the table
            boolean needsMigration = false;
            try {
                stmt.execute("SELECT phone, admin_id, role FROM users LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("no such column")) {
                    needsMigration = true;
                }
            }
            
            if (needsMigration) {
                // Clean up any existing temporary table
                try {
                    stmt.execute("DROP TABLE IF EXISTS users_temp");
                } catch (SQLException e) {
                    // Ignore any errors during cleanup
                }
                
                // Create temporary table with new schema
                stmt.execute("CREATE TABLE users_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "full_name TEXT NOT NULL," +
                    "email TEXT NOT NULL," +
                    "phone TEXT," +
                    "admin_id INTEGER," +
                    "role TEXT DEFAULT 'Member'," +
                    "FOREIGN KEY (admin_id) REFERENCES users(id)" +
                    ")");
                
                // Copy data from old table to new table
                stmt.execute("INSERT INTO users_temp (id, username, password, full_name, email) " +
                    "SELECT id, username, password, full_name, email FROM users");
                
                // Drop old table
                stmt.execute("DROP TABLE users");
                
                // Rename new table to original name
                stmt.execute("ALTER TABLE users_temp RENAME TO users");
            }
            
            // Create books table
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                "book_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "author TEXT NOT NULL," +
                "genre TEXT," +
                "format TEXT," +
                "status TEXT DEFAULT 'Available'," +
                "copies INTEGER DEFAULT 1," +
                "fee REAL DEFAULT 10.0," +
                "late_return_fee REAL DEFAULT 5.0" +
                ")");
            
            // Check if fee column exists in books table
            try {
                stmt.execute("SELECT fee FROM books LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("no such column")) {
                    // Add fee column if it doesn't exist
                    stmt.execute("ALTER TABLE books ADD COLUMN fee REAL DEFAULT 10.0");
                }
            }
            
            // Check if late_return_fee column exists in books table
            try {
                stmt.execute("SELECT late_return_fee FROM books LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("no such column")) {
                    // Add late_return_fee column if it doesn't exist
                    stmt.execute("ALTER TABLE books ADD COLUMN late_return_fee REAL DEFAULT 5.0");
                }
            }
            
            // Create rentals table
            stmt.execute("CREATE TABLE IF NOT EXISTS rentals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "staff_id INTEGER," +
                "rental_date TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "return_date TEXT," +
                "late_fee REAL DEFAULT 0.0," +
                "status TEXT DEFAULT 'Active'," +
                "payment_status TEXT DEFAULT 'pending'," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (book_id) REFERENCES books(book_id)," +
                "FOREIGN KEY (staff_id) REFERENCES users(id)" +
                ")");
            
            // Check if payment_status column exists in rentals table
            try {
                stmt.execute("SELECT payment_status FROM rentals LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("no such column")) {
                    // Add payment_status column if it doesn't exist
                    stmt.execute("ALTER TABLE rentals ADD COLUMN payment_status TEXT DEFAULT 'pending'");
                }
            }
            
            // Create reservations table
            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "reservation_date TEXT NOT NULL," +
                "expiration_date TEXT NOT NULL," +
                "status TEXT DEFAULT 'Active'," +
                "copies INTEGER DEFAULT 1," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (book_id) REFERENCES books(book_id)" +
                ")");
            
            // Check if copies column exists in reservations table
            try {
                stmt.execute("SELECT copies FROM reservations LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("no such column")) {
                    // Add copies column if it doesn't exist
                    stmt.execute("ALTER TABLE reservations ADD COLUMN copies INTEGER DEFAULT 1");
                }
            }
            
            // Create admin user if not exists
            stmt.execute("INSERT OR IGNORE INTO users (username, password, full_name, email, phone, role) " +
                "VALUES ('admin', 'admin123', 'System Administrator', 'admin@librorent.com', '123-456-7890', 'Admin')");
            
            // Update admin role if it exists but has wrong role
            stmt.execute("UPDATE users SET role = 'Admin' WHERE username = 'admin' AND role != 'Admin'");
            
            tablesCreated = true;
        } catch (SQLException e) {
            System.err.println("Failed to create database tables: " + e.getMessage());
            throw new RuntimeException("Failed to create database tables: " + e.getMessage(), e);
        }
    }
    
    public synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            initializeDatabase();
        }
        
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
    
    // Add a method to check if the native library is properly loaded
    public static boolean isNativeLibraryLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
} 