-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL
);

-- Insert sample admin user
INSERT OR IGNORE INTO users (username, password, role)
VALUES ('admin', 'admin123', 'admin'); 