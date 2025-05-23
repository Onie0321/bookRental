package librorent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class UserDashboard extends JFrame {
    private JPanel mainPanel;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JButton borrowButton;
    private JButton returnButton;
    private JButton searchButton;
    private JTextField searchField;
    private JLabel userInfoLabel;
    private JTextField memberIdField;
    private JTextField nameField;
    private JTextField emailField;
    private String currentUser;
    private Connection conn;
    
    public UserDashboard(String username) {
        this.currentUser = username;
        initializeComponents();
        setupDatabaseConnection();
        loadUserInfo();
        loadBooks();
    }
    
    private void initializeComponents() {
        setTitle("LibroRent - User Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // User info panel with gradient
        JPanel userInfoPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(70, 130, 180);  // Steel Blue
                Color color2 = new Color(65, 105, 225);  // Royal Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        userInfoPanel.setPreferredSize(new Dimension(0, 120));
        
        // Create user info form panel
        JPanel userFormPanel = new JPanel(new GridBagLayout());
        userFormPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Member ID field (non-editable)
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel memberIdLabel = new JLabel("Member ID:");
        memberIdLabel.setForeground(Color.WHITE);
        memberIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(memberIdLabel, gbc);
        
        gbc.gridx = 1;
        memberIdField = new JTextField(15);
        memberIdField.setEditable(false);
        memberIdField.setBackground(new Color(255, 255, 255, 200));
        memberIdField.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(memberIdField, gbc);
        
        // Name field (non-editable)
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setEditable(false);
        nameField.setBackground(new Color(255, 255, 255, 200));
        nameField.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(nameField, gbc);
        
        // Email field (non-editable)
        gbc.gridx = 2;
        gbc.gridy = 0;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(Color.WHITE);
        emailLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(emailLabel, gbc);
        
        gbc.gridx = 3;
        emailField = new JTextField(20);
        emailField.setEditable(false);
        emailField.setBackground(new Color(255, 255, 255, 200));
        emailField.setFont(new Font("Arial", Font.BOLD, 14));
        userFormPanel.add(emailField, gbc);
        
        userInfoPanel.add(userFormPanel, BorderLayout.CENTER);
        
        // Search panel with modern styling
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        searchField = new JTextField(30);
        searchField.setPreferredSize(new Dimension(300, 35));
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        searchButton = new JButton("🔍 Search") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(46, 204, 113);  // Green
                Color color2 = new Color(39, 174, 96);   // Darker Green
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        searchButton.setForeground(Color.WHITE);
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setPreferredSize(new Dimension(120, 35));
        searchButton.setOpaque(true);
        
        searchPanel.add(new JLabel("Search Books: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Table setup with modern styling
        String[] columns = {"ID", "Title", "Author", "ISBN", "Status", "Copies"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        booksTable = new JTable(tableModel);
        booksTable.setRowHeight(30);
        booksTable.setFont(new Font("Arial", Font.PLAIN, 14));
        booksTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        booksTable.getTableHeader().setBackground(new Color(70, 130, 180));
        booksTable.getTableHeader().setForeground(Color.WHITE);
        
        // Custom renderer for status column
        booksTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value.toString();
                switch (status) {
                    case "Available":
                        c.setForeground(new Color(46, 204, 113)); // Green
                        break;
                    case "Borrowed":
                        c.setForeground(new Color(231, 76, 60));  // Red
                        break;
                    default:
                        c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Button panel with modern styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        borrowButton = new JButton("📚 Borrow Book") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(46, 204, 113);  // Green
                Color color2 = new Color(39, 174, 96);   // Darker Green
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        borrowButton.setForeground(Color.WHITE);
        borrowButton.setFont(new Font("Arial", Font.BOLD, 14));
        borrowButton.setFocusPainted(false);
        borrowButton.setBorderPainted(false);
        borrowButton.setContentAreaFilled(false);
        borrowButton.setPreferredSize(new Dimension(150, 40));
        borrowButton.setOpaque(true);
        
        returnButton = new JButton("↩️ Return Book") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(231, 76, 60);   // Red
                Color color2 = new Color(192, 57, 43);   // Darker Red
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        returnButton.setForeground(Color.WHITE);
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setFocusPainted(false);
        returnButton.setBorderPainted(false);
        returnButton.setContentAreaFilled(false);
        returnButton.setPreferredSize(new Dimension(150, 40));
        returnButton.setOpaque(true);
        
        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);
        
        // Add components to main panel
        mainPanel.add(userInfoPanel, BorderLayout.NORTH);
        mainPanel.add(searchPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Add action listeners
        setupActionListeners();
    }
    
    private void setupDatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/librorent",
                "root",
                ""
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Database connection failed: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadUserInfo() {
        try {
            String query = "SELECT id, full_name, email FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int userId = rs.getInt("id");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                
                // Set member ID with LIB- prefix
                memberIdField.setText(String.format("LIB-%06d", userId));
                nameField.setText(fullName);
                emailField.setText(email);
            } else {
                memberIdField.setText("User not found");
                nameField.setText("");
                emailField.setText("");
            }
        } catch (SQLException e) {
            memberIdField.setText("Error loading user info");
            nameField.setText("");
            emailField.setText("");
            e.printStackTrace();
        }
    }
    
    private void loadBooks() {
        tableModel.setRowCount(0);
        try {
            String query = "SELECT * FROM books ORDER BY title";
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getString("status"),
                    rs.getInt("copies")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setupActionListeners() {
        searchButton.addActionListener(e -> searchBooks());
        borrowButton.addActionListener(e -> borrowBook());
        returnButton.addActionListener(e -> returnBook());
        
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchBooks();
                }
            }
        });
    }
    
    private void searchBooks() {
        String searchTerm = searchField.getText().trim();
        tableModel.setRowCount(0);
        
        try {
            String query = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getString("status"),
                    rs.getInt("copies")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error searching books: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void borrowBook() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a book to borrow",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 4);
        
        if (!status.equals("Available")) {
            JOptionPane.showMessageDialog(this,
                "This book is not available for borrowing",
                "Not Available",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Start transaction
            conn.setAutoCommit(false);
            
            // Update book status
            String updateBook = "UPDATE books SET status = 'Borrowed' WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateBook);
            pstmt.setInt(1, bookId);
            pstmt.executeUpdate();
            
            // Create borrowing record
            String insertBorrow = "INSERT INTO borrowings (book_id, user_id, borrow_date) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(insertBorrow);
            pstmt.setInt(1, bookId);
            pstmt.setString(2, currentUser);
            pstmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            pstmt.executeUpdate();
            
            conn.commit();
            loadBooks();
            
            JOptionPane.showMessageDialog(this,
                "Book borrowed successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                "Error borrowing book: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void returnBook() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a book to return",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 4);
        
        if (!status.equals("Borrowed")) {
            JOptionPane.showMessageDialog(this,
                "This book is not currently borrowed",
                "Not Borrowed",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Start transaction
            conn.setAutoCommit(false);
            
            // Update book status
            String updateBook = "UPDATE books SET status = 'Available' WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(updateBook);
            pstmt.setInt(1, bookId);
            pstmt.executeUpdate();
            
            // Update borrowing record
            String updateBorrow = "UPDATE borrowings SET return_date = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL";
            pstmt = conn.prepareStatement(updateBorrow);
            pstmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setInt(2, bookId);
            pstmt.setString(3, currentUser);
            pstmt.executeUpdate();
            
            conn.commit();
            loadBooks();
            
            JOptionPane.showMessageDialog(this,
                "Book returned successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                "Error returning book: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UserDashboard("test_user").setVisible(true);
        });
    }
}