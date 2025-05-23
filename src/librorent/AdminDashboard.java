package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.border.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminDashboard extends JFrame {
    private JPanel sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private int currentUserId;
    private final DatabaseManager dbManager;
    
    // UI Components
    private JTable booksTable;
    private JTable usersTable;
    private JTable rentalsTable;
    private DefaultTableModel booksModel;
    private DefaultTableModel usersModel;
    private DefaultTableModel rentalsModel;
    
    // Settings
    private int defaultRentalDuration = 14; // days
    private double lateFeeRate = 10.0; // Default late fee rate per day
    private int reservationExpiration = 24; // hours
    
    private JTable bookTable;
    private DefaultTableModel bookModel;
    private JLabel totalBooksLabel;
    private JLabel totalUsersLabel;
    private JLabel totalRentalsLabel;
    private JLabel totalRevenueLabel;
    private JLabel totalCopiesLabel;
    private JLabel totalAvailableLabel;
    
    private String username;
    private JPanel mainPanel;
    
    public AdminDashboard(String username) {
        this.username = username;
        this.currentUserId = -1; // Assuming a default userId
        this.dbManager = DatabaseManager.getInstance();
        
        setTitle("Admin Dashboard - LibroRent");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Get admin's full name
        String fullName = "";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT full_name FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fullName = rs.getString("full_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Create main panel with gradient background
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 140, 0); // Orange
                Color color2 = new Color(255, 69, 0);  // Red-Orange
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        
        // Add welcome message
        JLabel welcomeLabel = new JLabel("Welcome, " + fullName);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.add(welcomeLabel, BorderLayout.NORTH);
        
        // Initialize UI
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        // Main layout
        setLayout(new BorderLayout());
        
        // Sidebar
        sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);
        
        // Content panel
        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);
        add(contentPanel, BorderLayout.CENTER);
        
        // Initialize all panels
        contentPanel.add(createDashboardPanel(), "DASHBOARD");
        contentPanel.add(createBookManagementPanel(), "BOOK_MANAGEMENT");
        contentPanel.add(createUserManagementPanel(), "USER_MANAGEMENT");
        contentPanel.add(createRentalManagementPanel(), "RENTAL_MANAGEMENT");
        contentPanel.add(new InventoryPanel(), "INVENTORY");
        contentPanel.add(createSettingsPanel(), "SETTINGS");
        
        // Show dashboard by default
        cardLayout.show(contentPanel, "DASHBOARD");
        
        // Load initial data only once
        SwingUtilities.invokeLater(() -> {
            loadBooks(bookModel);
            loadUsers();
            loadRentals();
            updateDashboardStats();
        });
    }
    
    private JPanel createSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(51, 51, 51));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        // Logo
        JLabel logoLabel = new JLabel("LibroRent");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logoLabel);
        sidebar.add(Box.createVerticalStrut(30));
        
        // Menu items
        String[] menuItems = {
            "📊 Dashboard",
            "📚 Book Management",
            "👤 User Management",
            "📖 Rental Management",
            "📦 Inventory",
            "⚙️ Settings"
        };
        
        for (String item : menuItems) {
            JButton button = createMenuButton(item);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(10));
        }
        
        // Logout button
        JButton logoutButton = createMenuButton("🚪 Logout");
        logoutButton.addActionListener(e -> handleLogout());
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);
        
        return sidebar;
    }
    
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(51, 51, 51));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(51, 51, 51));
            }
        });
        
        // Add click handler
        button.addActionListener(e -> {
            String cardName;
            switch (text) {
                case "📊 Dashboard":
                    cardName = "DASHBOARD";
                    updateDashboardStats(); // Only update stats when switching to dashboard
                    break;
                case "📚 Book Management":
                    cardName = "BOOK_MANAGEMENT";
                    break;
                case "👤 User Management":
                    cardName = "USER_MANAGEMENT";
                    break;
                case "📖 Rental Management":
                    cardName = "RENTAL_MANAGEMENT";
                    break;
                case "📦 Inventory":
                    cardName = "INVENTORY";
                    break;
                case "⚙️ Settings":
                    cardName = "SETTINGS";
                    break;
                default:
                    cardName = "DASHBOARD";
            }
            cardLayout.show(contentPanel, cardName);
        });
        
        return button;
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 245)); // Light gray background
        
        // Header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 140, 0); // Orange
                Color color2 = new Color(255, 69, 0);  // Red-Orange
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("Dashboard Overview");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Statistics panel
        JPanel statsPanel = new JPanel(new GridLayout(3, 3, 20, 20));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        statsPanel.setBackground(new Color(245, 245, 245));
        
        // Initialize the label fields
        totalBooksLabel = new JLabel("Total Books: 0");
        totalUsersLabel = new JLabel("Total Users: 0");
        totalRentalsLabel = new JLabel("Total Rentals: 0");
        totalRevenueLabel = new JLabel("Total Revenue: ₱0.00");
        
        // Create stat cards with different colors
        statsPanel.add(createStatCard("Total Books", "0", "📚", new Color(52, 152, 219)));      // Blue
        statsPanel.add(createStatCard("Available Books", "0", "✅", new Color(46, 204, 113)));  // Green
        statsPanel.add(createStatCard("Unavailable Books", "0", "❌", new Color(231, 76, 60))); // Red
        statsPanel.add(createStatCard("E-Books", "0", "💻", new Color(155, 89, 182)));         // Purple
        statsPanel.add(createStatCard("Physical Books", "0", "📖", new Color(241, 196, 15)));  // Yellow
        statsPanel.add(createStatCard("Total Copies", "0", "📦", new Color(230, 126, 34)));    // Orange
        statsPanel.add(createStatCard("Active Rentals", "0", "📖", new Color(52, 73, 94)));    // Dark Blue
        statsPanel.add(createStatCard("Total Users", "0", "👥", new Color(22, 160, 133)));     // Teal
        statsPanel.add(createStatCard("Overdue Books", "0", "⚠️", new Color(192, 57, 43)));    // Dark Red
        
        panel.add(statsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatCard(String title, String value, String icon, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color2 = color.darker();
                GradientPaint gp = new GradientPaint(0, 0, color, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 32));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(valueLabel);
        
        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(150, 150, 150)),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
            public void mouseExited(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
        });
        
        return card;
    }
    
    private JPanel createBookManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 245));
        
        // Header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(52, 152, 219);  // Blue
                Color color2 = new Color(41, 128, 185);  // Darker Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("Book Management");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Button panel for Add and Refresh buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        // Add New Book button
        JButton addButton = new JButton("Add New Book") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(46, 204, 113);   // Green
                Color color2 = new Color(39, 174, 96);    // Darker Green
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        addButton.setForeground(Color.BLACK);
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setFocusPainted(false);
        addButton.setBorderPainted(false);
        addButton.setContentAreaFilled(false);
        addButton.setOpaque(true);
        
        // Refresh button
        JButton refreshButton = new JButton("Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(52, 152, 219);   // Blue
                Color color2 = new Color(41, 128, 185);   // Darker Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setOpaque(true);
        
        // Add action listeners
        addButton.addActionListener(e -> showAddBookDialog());
        refreshButton.addActionListener(e -> {
            loadBooks(bookModel);
            updateBookStats(totalBooksLabel, totalCopiesLabel, totalAvailableLabel);
        });
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Search panel with gradient
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 248, 255);  // Light Blue
                Color color2 = new Color(230, 240, 250);  // Lighter Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(70, 130, 180));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "ISBN", "Format", "Genre", "Copies", "Fee", "Status", "Action"};
        bookModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 9; // Only action column is editable
            }
        };
        
        bookTable = new JTable(bookModel);
        bookTable.setRowHeight(30);
        bookTable.getTableHeader().setReorderingAllowed(false);
        bookTable.setShowGrid(false);
        bookTable.setIntercellSpacing(new Dimension(0, 0));
        bookTable.getTableHeader().setBackground(new Color(70, 130, 180));
        bookTable.getTableHeader().setForeground(Color.WHITE);
        bookTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // ISBN
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Format
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Genre
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Copies
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(80);   // Fee
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(100);  // Status
        bookTable.getColumnModel().getColumn(9).setPreferredWidth(100);  // Action
        
        JScrollPane scrollPane = new JScrollPane(bookTable);
        
        // Statistics panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBackground(new Color(245, 245, 245));
        JLabel totalBooksLabel = new JLabel("Total Books: 0");
        JLabel totalCopiesLabel = new JLabel("Total Copies: 0");
        JLabel totalAvailableLabel = new JLabel("Total Available: 0");
        totalBooksLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalCopiesLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalAvailableLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsPanel.add(totalBooksLabel);
        statsPanel.add(new JLabel(" | "));
        statsPanel.add(totalCopiesLabel);
        statsPanel.add(new JLabel(" | "));
        statsPanel.add(totalAvailableLabel);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(searchPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.SOUTH);
        
        // Load initial data
        loadBooks(bookModel);
        updateBookStats(totalBooksLabel, totalCopiesLabel, totalAvailableLabel);
        
        return panel;
    }
    
    private void updateBookStats(JLabel totalBooksLabel, JLabel totalCopiesLabel, JLabel totalAvailableLabel) {
        try (Connection conn = dbManager.getConnection()) {
            // Total books and copies
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                     SELECT COUNT(*) as total_books, 
                            SUM(copies) as total_copies,
                            SUM(CASE WHEN status = 'Available' THEN copies ELSE 0 END) as available_copies
                     FROM books
                     """)) {
                if (rs.next()) {
                    int totalBooks = rs.getInt("total_books");
                    int totalCopies = rs.getInt("total_copies");
                    int availableCopies = rs.getInt("available_copies");
                    int unavailableCopies = Math.max(0, totalCopies - availableCopies); // Ensure non-negative value
                    
                    totalBooksLabel.setText("Total Books: " + totalBooks);
                    totalCopiesLabel.setText("Total Copies: " + totalCopies);
                    totalAvailableLabel.setText("Total Available: " + availableCopies);
                    
                    // Update dashboard stats
                    updateStatCard(0, totalBooks); // Total Books
                    updateStatCard(1, availableCopies); // Available Books
                    updateStatCard(2, unavailableCopies); // Unavailable Books
                    updateStatCard(5, totalCopies); // Total Copies
                }
            }
            
            // E-Books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE format = 'E-Book'")) {
                if (rs.next()) {
                    updateStatCard(3, rs.getInt(1));
                }
            }
            
            // Physical Books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE format = 'Physical'")) {
                if (rs.next()) {
                    updateStatCard(4, rs.getInt(1));
                }
            }
            
            // Active rentals
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rentals WHERE status = 'Active'")) {
                if (rs.next()) {
                    updateStatCard(6, rs.getInt(1));
                }
            }
            
            // Total users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    updateStatCard(7, rs.getInt(1));
                }
            }
            
            // Overdue books
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                    SELECT COUNT(*) FROM rentals 
                    WHERE status = 'Active' AND due_date < datetime('now')
                 """)) {
                if (rs.next()) {
                    updateStatCard(8, rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating book stats: " + e.getMessage());
        }
    }
    
    private void updateStatCard(int index, int value) {
        JPanel statsPanel = (JPanel) ((JPanel) contentPanel.getComponent(0)).getComponent(1);
        JPanel card = (JPanel) statsPanel.getComponent(index);
        JLabel valueLabel = (JLabel) card.getComponent(4);
        valueLabel.setText(String.valueOf(value));
    }
    
    private void showAddBookDialog() {
        JDialog dialog = new JDialog(this, "Add New Book", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField titleField = new JTextField(20);
        JTextField authorField = new JTextField(20);
        JTextField isbnField = new JTextField(20);
        JComboBox<String> genreCombo = new JComboBox<>(new String[]{"Fiction", "Non-Fiction", "Mystery", "Science Fiction", "Romance", "Biography"});
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"Physical", "E-Book"});
        JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JSpinner feeSpinner = new JSpinner(new SpinnerNumberModel(10.0, 10.0, 1000.0, 10.0));
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        formPanel.add(titleField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        formPanel.add(authorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1;
        formPanel.add(isbnField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Genre:"), gbc);
        gbc.gridx = 1;
        formPanel.add(genreCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Format:"), gbc);
        gbc.gridx = 1;
        formPanel.add(formatCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Copies:"), gbc);
        gbc.gridx = 1;
        formPanel.add(copiesSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Fee (₱):"), gbc);
        gbc.gridx = 1;
        formPanel.add(feeSpinner, gbc);
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(ev -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO books (title, author, isbn, genre, format, copies, fee, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                pstmt.setString(1, titleField.getText());
                pstmt.setString(2, authorField.getText());
                pstmt.setString(3, isbnField.getText());
                pstmt.setString(4, (String)genreCombo.getSelectedItem());
                pstmt.setString(5, formatCombo.getSelectedItem().toString());
                pstmt.setInt(6, (Integer)copiesSpinner.getValue());
                pstmt.setDouble(7, (Double)feeSpinner.getValue());
                pstmt.setString(8, "Available");
                pstmt.executeUpdate();
                
                dialog.dispose();
                loadBooks(bookModel);
                updateBookStats(totalBooksLabel, totalCopiesLabel, totalAvailableLabel);
                JOptionPane.showMessageDialog(dialog,
                    "Book added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                    "Error adding book: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(ev -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void loadData() {
        // This method is now only used for initial loading
        if (bookModel != null) {
            loadBooks(bookModel);
        }
        loadUsers();
        loadRentals();
        updateDashboardStats();
    }
    
    private void loadUsers() {
        usersModel.setRowCount(0); // Clear existing data
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY id DESC")) {
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role")
                };
                usersModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading users: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 245));
        
        // Header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(22, 160, 133);  // Teal
                Color color2 = new Color(19, 141, 117);  // Darker Teal
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("Settings");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Settings form with gradient background
        JPanel formPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(236, 240, 241);  // Light Gray
                Color color2 = new Color(189, 195, 199);  // Darker Gray
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Default Rental Duration settings
        JPanel rentalDurationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rentalDurationPanel.setOpaque(false);
        JLabel rentalDurationLabel = new JLabel("Default Rental Duration:");
        rentalDurationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        rentalDurationLabel.setForeground(Color.BLACK);
        
        // Create input fields for rental duration
        JSpinner rentalDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        JSpinner rentalHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        JSpinner rentalMinutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        JSpinner rentalSecondsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        
        // Set initial values from defaultRentalDuration
        int totalSeconds = defaultRentalDuration;
        rentalDaysSpinner.setValue(totalSeconds / (24 * 3600));
        rentalHoursSpinner.setValue((totalSeconds % (24 * 3600)) / 3600);
        rentalMinutesSpinner.setValue((totalSeconds % 3600) / 60);
        rentalSecondsSpinner.setValue(totalSeconds % 60);
        
        rentalDurationPanel.add(rentalDurationLabel);
        rentalDurationPanel.add(new JLabel("Days:"));
        rentalDurationPanel.add(rentalDaysSpinner);
        rentalDurationPanel.add(new JLabel("Hours:"));
        rentalDurationPanel.add(rentalHoursSpinner);
        rentalDurationPanel.add(new JLabel("Minutes:"));
        rentalDurationPanel.add(rentalMinutesSpinner);
        rentalDurationPanel.add(new JLabel("Seconds:"));
        rentalDurationPanel.add(rentalSecondsSpinner);
        
        // Reservation Expiration settings
        JPanel reservationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reservationPanel.setOpaque(false);
        JLabel reservationLabel = new JLabel("Reservation Expiration:");
        reservationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        reservationLabel.setForeground(Color.BLACK);
        
        // Create input fields for reservation expiration
        JSpinner reservationDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        JSpinner reservationHoursSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        JSpinner reservationMinutesSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        JSpinner reservationSecondsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
        
        // Set initial values from reservationExpiration
        totalSeconds = reservationExpiration;
        reservationDaysSpinner.setValue(totalSeconds / (24 * 3600));
        reservationHoursSpinner.setValue((totalSeconds % (24 * 3600)) / 3600);
        reservationMinutesSpinner.setValue((totalSeconds % 3600) / 60);
        reservationSecondsSpinner.setValue(totalSeconds % 60);
        
        reservationPanel.add(reservationLabel);
        reservationPanel.add(new JLabel("Days:"));
        reservationPanel.add(reservationDaysSpinner);
        reservationPanel.add(new JLabel("Hours:"));
        reservationPanel.add(reservationHoursSpinner);
        reservationPanel.add(new JLabel("Minutes:"));
        reservationPanel.add(reservationMinutesSpinner);
        reservationPanel.add(new JLabel("Seconds:"));
        reservationPanel.add(reservationSecondsSpinner);
        
        // Late Fee Rate settings
        JPanel lateFeePanel = createSettingRow("Late Fee Rate (₱ per day):", 
            new JSpinner(new SpinnerNumberModel(lateFeeRate, 0.0, 1000.0, 10.0)));
        
        formPanel.add(rentalDurationPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(reservationPanel);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(lateFeePanel);
        
        // Save button with visible styling
        JButton saveButton = new JButton("Save Changes");
        saveButton.setFont(new Font("Arial", Font.BOLD, 14));
        saveButton.setForeground(Color.BLACK);
        saveButton.setBackground(new Color(46, 204, 113));  // Green
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(true);
        saveButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        saveButton.setPreferredSize(new Dimension(150, 35));
        
        saveButton.addActionListener(e -> {
            // Convert rental duration to seconds
            int rentalDays = (Integer)rentalDaysSpinner.getValue();
            int rentalHours = (Integer)rentalHoursSpinner.getValue();
            int rentalMinutes = (Integer)rentalMinutesSpinner.getValue();
            int rentalSeconds = (Integer)rentalSecondsSpinner.getValue();
            int rentalDurationInSeconds = rentalDays * 24 * 3600 + rentalHours * 3600 + rentalMinutes * 60 + rentalSeconds;
            
            // Convert reservation expiration to seconds
            int reservationDays = (Integer)reservationDaysSpinner.getValue();
            int reservationHours = (Integer)reservationHoursSpinner.getValue();
            int reservationMinutes = (Integer)reservationMinutesSpinner.getValue();
            int reservationSeconds = (Integer)reservationSecondsSpinner.getValue();
            int reservationDurationInSeconds = reservationDays * 24 * 3600 + reservationHours * 3600 + reservationMinutes * 60 + reservationSeconds;
            
            saveSettings(rentalDurationInSeconds, reservationDurationInSeconds, (Double)((JSpinner)lateFeePanel.getComponent(1)).getValue());
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(saveButton);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private int convertToSeconds(int value, String unit) {
        switch (unit) {
            case "Days":
                return value * 24 * 60 * 60;
            case "Hours":
                return value * 60 * 60;
            case "Minutes":
                return value * 60;
            case "Seconds":
                return value;
            default:
                return value;
        }
    }
    
    private void saveSettings(int rentalDurationSeconds, int reservationSeconds, double lateFeeRate) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO settings (key, value, last_updated) VALUES (?, ?, datetime('now'))")) {
            
            // Store old values for comparison
            int oldRentalDuration = this.defaultRentalDuration;
            int oldReservationExpiration = this.reservationExpiration;
            double oldLateFeeRate = this.lateFeeRate;
            
            pstmt.setString(1, "default_rental_duration");
            pstmt.setString(2, String.valueOf(rentalDurationSeconds));
            pstmt.executeUpdate();
            
            pstmt.setString(1, "late_fee_rate");
            pstmt.setString(2, String.valueOf(lateFeeRate));
            pstmt.executeUpdate();
            
            pstmt.setString(1, "reservation_expiration");
            pstmt.setString(2, String.valueOf(reservationSeconds));
            pstmt.executeUpdate();
            
            // Update the instance variables
            this.defaultRentalDuration = rentalDurationSeconds;
            this.reservationExpiration = reservationSeconds;
            this.lateFeeRate = lateFeeRate;
            
            // Get current timestamp
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // Build message showing what changed
            StringBuilder message = new StringBuilder("Settings updated at " + timestamp + ":\n\n");
            
            if (oldRentalDuration != rentalDurationSeconds) {
                message.append("• Default Rental Duration: ")
                      .append(formatDuration(oldRentalDuration))
                      .append(" → ")
                      .append(formatDuration(rentalDurationSeconds))
                      .append("\n");
            }
            
            if (oldReservationExpiration != reservationSeconds) {
                message.append("• Reservation Expiration: ")
                      .append(formatDuration(oldReservationExpiration))
                      .append(" → ")
                      .append(formatDuration(reservationSeconds))
                      .append("\n");
            }
            
            if (oldLateFeeRate != lateFeeRate) {
                message.append("• Late Fee Rate: ₱")
                      .append(String.format("%.2f", oldLateFeeRate))
                      .append(" → ₱")
                      .append(String.format("%.2f", lateFeeRate))
                      .append(" per day\n");
            }
            
            JOptionPane.showMessageDialog(this, 
                message.toString(),
                "Settings Updated",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage());
        }
    }
    
    private String formatDuration(int seconds) {
        int days = seconds / (24 * 3600);
        int hours = (seconds % (24 * 3600)) / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;
        
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (remainingSeconds > 0 || result.length() == 0) result.append(remainingSeconds).append("s");
        
        return result.toString().trim();
    }
    
    private JPanel createSettingRow(String label, JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Arial", Font.BOLD, 14));
        
        component.setPreferredSize(new Dimension(100, 30));
        
        panel.add(labelComponent);
        panel.add(component);
        
        return panel;
    }
    
    private void loadBooks(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY book_id DESC")) {
            
            while (rs.next()) {
                double fee = rs.getDouble("fee");
                if (rs.wasNull()) {
                    fee = 10.0; // Default fee if null
                }
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getString("format"),
                    rs.getString("genre"),
                    rs.getInt("copies"),
                    "₱" + fee,
                    rs.getString("status"),
                    "Edit"
                };
                model.addRow(row);
            }
            
            // Set up the edit button column
            TableColumn editColumn = bookTable.getColumnModel().getColumn(9);
            editColumn.setCellRenderer(new ButtonRenderer());
            editColumn.setCellEditor(new ButtonEditor(bookTable));
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Custom table cell renderer for action buttons
    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            editButton = new JButton("Edit");
            editButton.setBackground(new Color(52, 152, 219)); // Blue
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            add(editButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    // Custom table cell editor for action buttons
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        
        public ButtonEditor(JTable table) {
            super(new JCheckBox());
            this.table = table;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                editBook();
            }
            isPushed = false;
            return label;
        }
        
        private void editBook() {
            int row = table.getSelectedRow();
            if (row != -1) {
                String bookIdStr = (String)table.getValueAt(row, 0);
                int bookId = Integer.parseInt(bookIdStr.substring(1)); // Remove 'B' prefix
                String title = (String) table.getValueAt(row, 1);
                String author = (String) table.getValueAt(row, 2);
                String isbn = (String) table.getValueAt(row, 3);
                String format = (String) table.getValueAt(row, 4);
                String genre = (String) table.getValueAt(row, 5);
                int copies = (int) table.getValueAt(row, 6);
                double fee = Double.parseDouble(((String)table.getValueAt(row, 7)).substring(1)); // Remove '₱' prefix
                String status = (String) table.getValueAt(row, 8);
                
                JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(table), "Edit Book", true);
                dialog.setLayout(new BorderLayout(10, 10));
                
                JPanel formPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                JTextField titleField = new JTextField(title, 20);
                JTextField authorField = new JTextField(author, 20);
                JTextField isbnField = new JTextField(isbn, 20);
                JComboBox<String> genreCombo = new JComboBox<>(new String[]{"Fiction", "Non-Fiction", "Mystery", "Science Fiction", "Romance", "Biography"});
                genreCombo.setSelectedItem(genre);
                JComboBox<String> formatCombo = new JComboBox<>(new String[]{"Physical", "E-Book"});
                formatCombo.setSelectedItem(format);
                JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(copies, 0, 1000, 1));
                JSpinner feeSpinner = new JSpinner(new SpinnerNumberModel(fee, 10.0, 1000.0, 10.0));
                JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Available", "Unavailable"});
                statusCombo.setSelectedItem(status);
                
                gbc.gridx = 0; gbc.gridy = 0;
                formPanel.add(new JLabel("Title:"), gbc);
                gbc.gridx = 1;
                formPanel.add(titleField, gbc);
                
                gbc.gridx = 0; gbc.gridy = 1;
                formPanel.add(new JLabel("Author:"), gbc);
                gbc.gridx = 1;
                formPanel.add(authorField, gbc);
                
                gbc.gridx = 0; gbc.gridy = 2;
                formPanel.add(new JLabel("ISBN:"), gbc);
                gbc.gridx = 1;
                formPanel.add(isbnField, gbc);
                
                gbc.gridx = 0; gbc.gridy = 3;
                formPanel.add(new JLabel("Genre:"), gbc);
                gbc.gridx = 1;
                formPanel.add(genreCombo, gbc);
                
                gbc.gridx = 0; gbc.gridy = 4;
                formPanel.add(new JLabel("Format:"), gbc);
                gbc.gridx = 1;
                formPanel.add(formatCombo, gbc);
                
                gbc.gridx = 0; gbc.gridy = 5;
                formPanel.add(new JLabel("Copies:"), gbc);
                gbc.gridx = 1;
                formPanel.add(copiesSpinner, gbc);
                
                gbc.gridx = 0; gbc.gridy = 6;
                formPanel.add(new JLabel("Fee (₱):"), gbc);
                gbc.gridx = 1;
                formPanel.add(feeSpinner, gbc);
                
                gbc.gridx = 0; gbc.gridy = 7;
                formPanel.add(new JLabel("Status:"), gbc);
                gbc.gridx = 1;
                formPanel.add(statusCombo, gbc);
                
                JButton saveButton = new JButton("Save");
                saveButton.addActionListener(e -> {
                    try (Connection conn = DatabaseManager.getInstance().getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                             "UPDATE books SET title = ?, author = ?, isbn = ?, genre = ?, format = ?, copies = ?, fee = ?, status = ? WHERE book_id = ?")) {
                        
                        pstmt.setString(1, titleField.getText());
                        pstmt.setString(2, authorField.getText());
                        pstmt.setString(3, isbnField.getText());
                        pstmt.setString(4, (String)genreCombo.getSelectedItem());
                        pstmt.setString(5, formatCombo.getSelectedItem().toString());
                        pstmt.setInt(6, (Integer)copiesSpinner.getValue());
                        pstmt.setDouble(7, (Double)feeSpinner.getValue());
                        pstmt.setString(8, (String)statusCombo.getSelectedItem());
                        pstmt.setInt(9, bookId);
                        pstmt.executeUpdate();
                        
                        dialog.dispose();
                        
                        // Refresh all related components
                        loadBooks(bookModel);
                        updateBookStats(totalBooksLabel, totalCopiesLabel, totalAvailableLabel);
                        loadRentals(); // Refresh rentals to show updated book information
                        
                        JOptionPane.showMessageDialog(dialog,
                            "Book updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dialog,
                            "Error updating book: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                JButton deleteButton = new JButton("Delete");
                deleteButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete this book?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        try (Connection conn = DatabaseManager.getInstance().getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(
                                 "DELETE FROM books WHERE book_id = ?")) {
                            
                            pstmt.setInt(1, bookId);
                            pstmt.executeUpdate();
                            
                            dialog.dispose();
                            
                            // Refresh all related components
                            loadBooks(bookModel);
                            updateBookStats(totalBooksLabel, totalCopiesLabel, totalAvailableLabel);
                            loadRentals(); // Refresh rentals to show updated book information
                            
                            JOptionPane.showMessageDialog(dialog,
                                "Book deleted successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(dialog,
                                "Error deleting book: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(e -> dialog.dispose());
                
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(deleteButton);
                buttonPanel.add(cancelButton);
                buttonPanel.add(saveButton);
                
                dialog.add(formPanel, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                dialog.pack();
                dialog.setLocationRelativeTo(table);
                dialog.setVisible(true);
            }
        }
    }

    private void updateDashboardStats() {
        try (Connection conn = dbManager.getConnection()) {
            // Total books
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM books")) {
                if (rs.next()) {
                    totalBooksLabel.setText("Total Books: " + rs.getInt("total"));
                }
            }
            
            // Total users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM users")) {
                if (rs.next()) {
                    totalUsersLabel.setText("Total Users: " + rs.getInt("total"));
                }
            }
            
            // Total rentals
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM rentals")) {
                if (rs.next()) {
                    totalRentalsLabel.setText("Total Rentals: " + rs.getInt("total"));
                }
            }
            
            // Total revenue (sum of late fees)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SUM(late_fee) as total FROM rentals")) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    totalRevenueLabel.setText("Total Revenue: ₱" + String.format("%.2f", total));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 245));
        
        // Header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(155, 89, 182);  // Purple
                Color color2 = new Color(142, 68, 173);  // Darker Purple
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("User Management");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Add User button with gradient
        JButton addUserButton = new JButton("Add New User") {
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
                super.paintComponent(g);  // Paint the text on top of the gradient
            }
        };
        addUserButton.setForeground(Color.BLACK);
        addUserButton.setFont(new Font("Arial", Font.BOLD, 14));
        addUserButton.setFocusPainted(false);
        addUserButton.setBorderPainted(false);
        addUserButton.setContentAreaFilled(false);
        addUserButton.setPreferredSize(new Dimension(150, 35));
        addUserButton.setOpaque(true);
        addUserButton.addActionListener(e -> showAddUserDialog());
        
        // Create a panel to hold the button with proper padding
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addUserButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Users table with custom styling
        String[] columns = {"ID", "Username", "Full Name", "Email", "Phone", "Role"};
        usersModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all columns non-editable
            }
        };
        
        usersTable = new JTable(usersModel);
        usersTable.setRowHeight(30);
        usersTable.setShowGrid(false);
        usersTable.setIntercellSpacing(new Dimension(0, 0));
        usersTable.getTableHeader().setBackground(new Color(155, 89, 182));
        usersTable.getTableHeader().setForeground(Color.WHITE);
        usersTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Set column widths
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // ID
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Username
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Full Name
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(200);  // Email
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Phone
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Role
        
        // Custom table renderer for alternating row colors
        usersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createRentalManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 245));
        
        // Header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(52, 73, 94);  // Dark Blue
                Color color2 = new Color(44, 62, 80);  // Darker Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("Rental Management");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Create main content panel with split view
        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 20));
        contentPanel.setBackground(new Color(245, 245, 245));
        
        // Top panel for user late fees summary
        JPanel userFeesPanel = new JPanel(new BorderLayout(10, 10));
        userFeesPanel.setBackground(new Color(245, 245, 245));
        
        // User fees header
        JPanel userFeesHeader = new JPanel(new BorderLayout());
        userFeesHeader.setBackground(new Color(52, 73, 94));
        JLabel userFeesLabel = new JLabel("User Late Fees Summary");
        userFeesLabel.setFont(new Font("Arial", Font.BOLD, 18));
        userFeesLabel.setForeground(Color.WHITE);
        userFeesLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        userFeesHeader.add(userFeesLabel, BorderLayout.WEST);
        
        // User fees table
        String[] userColumns = {"User ID", "Username", "Full Name", "Total Books Rented", "Book Titles", "Total Late Fees", "Active Rentals", "Overdue Rentals"};
        DefaultTableModel userFeesModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable userFeesTable = new JTable(userFeesModel);
        userFeesTable.setRowHeight(30);
        userFeesTable.setShowGrid(false);
        userFeesTable.setIntercellSpacing(new Dimension(0, 0));
        userFeesTable.getTableHeader().setBackground(new Color(52, 73, 94));
        userFeesTable.getTableHeader().setForeground(Color.WHITE);
        userFeesTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Set column widths
        userFeesTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // User ID
        userFeesTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Username
        userFeesTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Full Name
        userFeesTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Total Books
        userFeesTable.getColumnModel().getColumn(4).setPreferredWidth(300);  // Book Titles
        userFeesTable.getColumnModel().getColumn(5).setPreferredWidth(120);  // Late Fees
        userFeesTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Active Rentals
        userFeesTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Overdue Rentals
        
        // Custom renderer for user fees table
        userFeesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                if (column == 5) { // Late Fees column
                    if (value != null && value.toString().startsWith("₱")) {
                        double fee = Double.parseDouble(value.toString().substring(1));
                        if (fee > 0) {
                            c.setForeground(new Color(192, 57, 43)); // Red for fees > 0
                        } else {
                            c.setForeground(new Color(39, 174, 96)); // Green for no fees
                        }
                    }
                }
                return c;
            }
        });
        
        JScrollPane userFeesScrollPane = new JScrollPane(userFeesTable);
        userFeesPanel.add(userFeesHeader, BorderLayout.NORTH);
        userFeesPanel.add(userFeesScrollPane, BorderLayout.CENTER);
        
        // Bottom panel for rental details
        JPanel rentalDetailsPanel = new JPanel(new BorderLayout(10, 10));
        rentalDetailsPanel.setBackground(new Color(245, 245, 245));
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(new Color(245, 245, 245));
        JTextField searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 30));
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(52, 73, 94));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Rentals table
        String[] columns = {"ID", "Book", "User", "Rental Date", "Due Date", "Return Date", "Status", "Late Fee"};
        rentalsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        rentalsTable = new JTable(rentalsModel);
        rentalsTable.setRowHeight(30);
        rentalsTable.setShowGrid(false);
        rentalsTable.setIntercellSpacing(new Dimension(0, 0));
        rentalsTable.getTableHeader().setBackground(new Color(52, 73, 94));
        rentalsTable.getTableHeader().setForeground(Color.WHITE);
        rentalsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Custom renderer for rentals table
        rentalsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                if (column == 7) { // Late Fee column
                    if (value != null && value.toString().startsWith("₱")) {
                        double fee = Double.parseDouble(value.toString().substring(1));
                        if (fee > 0) {
                            c.setForeground(new Color(192, 57, 43)); // Red for fees > 0
                        } else {
                            c.setForeground(new Color(39, 174, 96)); // Green for no fees
                        }
                    }
                }
                return c;
            }
        });
        
        JScrollPane rentalsScrollPane = new JScrollPane(rentalsTable);
        rentalDetailsPanel.add(searchPanel, BorderLayout.NORTH);
        rentalDetailsPanel.add(rentalsScrollPane, BorderLayout.CENTER);
        
        // Add panels to content
        contentPanel.add(userFeesPanel);
        contentPanel.add(rentalDetailsPanel);
        
        // Add components to main panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Load initial data
        loadRentals();
        loadUserFeesSummary(userFeesModel);
        
        // Add search functionality
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                searchRentals(searchTerm);
            } else {
                loadRentals();
            }
        });
        
        return panel;
    }

    private void loadUserFeesSummary(DefaultTableModel model) {
        System.out.println("\n=== Starting User Fees Summary Loading ===");
        model.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            System.out.println("Database connection established");
            
            // First, let's check what roles we have in the database
            try (Statement stmt = conn.createStatement();
                 ResultSet roleCheck = stmt.executeQuery("SELECT DISTINCT role FROM users")) {
                System.out.println("\nAvailable roles in database:");
                while (roleCheck.next()) {
                    System.out.println("Role: '" + roleCheck.getString("role") + "'");
                }
            }
            
            // Now let's check if we have any rentals
            try (Statement stmt = conn.createStatement();
                 ResultSet rentalCheck = stmt.executeQuery("SELECT COUNT(*) as count FROM rentals")) {
                if (rentalCheck.next()) {
                    System.out.println("Total rentals found: " + rentalCheck.getInt("count"));
                }
            }
            
            // Main query for user fees summary - using 'Member' role
            try (PreparedStatement pstmt = conn.prepareStatement("""
                SELECT 
                    u.id,
                    u.username,
                    u.full_name,
                    (SELECT COUNT(DISTINCT r.book_id) FROM rentals r WHERE r.user_id = u.id) as total_books,
                    (SELECT b.title FROM rentals r JOIN books b ON r.book_id = b.book_id WHERE r.user_id = u.id LIMIT 1) as sample_book,
                    (SELECT COALESCE(SUM(r.late_fee), 0) FROM rentals r WHERE r.user_id = u.id) as total_late_fees,
                    (SELECT COUNT(*) FROM rentals r WHERE r.user_id = u.id AND r.status = 'Active') as active_rentals,
                    (SELECT COUNT(*) FROM rentals r WHERE r.user_id = u.id AND r.status = 'Active' AND r.due_date < datetime('now')) as overdue_rentals
                FROM users u
                WHERE u.role = 'Member'
                ORDER BY total_late_fees DESC
             """)) {
                
                System.out.println("Executing main query...");
                ResultSet rs = pstmt.executeQuery();
                int rowCount = 0;
                
                while (rs.next()) {
                    rowCount++;
                    int userId = rs.getInt("id");
                    String username = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    int totalBooks = rs.getInt("total_books");
                    String sampleBook = rs.getString("sample_book");
                    double totalLateFees = rs.getDouble("total_late_fees");
                    int activeRentals = rs.getInt("active_rentals");
                    int overdueRentals = rs.getInt("overdue_rentals");
                    
                    System.out.println("\nProcessing user: " + username);
                    System.out.println("User ID: " + userId);
                    System.out.println("Total Books: " + totalBooks);
                    System.out.println("Sample Book: " + (sampleBook != null ? sampleBook : "null"));
                    System.out.println("Total Late Fees: " + totalLateFees);
                    System.out.println("Active Rentals: " + activeRentals);
                    System.out.println("Overdue Rentals: " + overdueRentals);
                    
                    String bookTitles;
                    if (totalBooks == 0) {
                        bookTitles = "No books rented";
                    } else if (totalBooks == 1) {
                        bookTitles = sampleBook != null ? sampleBook : "Unknown book";
                    } else {
                        bookTitles = sampleBook != null ? 
                            sampleBook + " and " + (totalBooks - 1) + " more" : 
                            totalBooks + " books rented";
                    }
                    
                    Object[] row = {
                        userId,
                        username,
                        fullName,
                        totalBooks,
                        bookTitles,
                        String.format("₱%.2f", totalLateFees),
                        activeRentals,
                        overdueRentals
                    };
                    model.addRow(row);
                }
                
                System.out.println("\n=== Summary ===");
                System.out.println("Total users loaded: " + rowCount);
                System.out.println("Table model row count: " + model.getRowCount());
                
            } catch (SQLException e) {
                System.err.println("Error in main query execution:");
                e.printStackTrace();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("\n=== Error Details ===");
            System.err.println("SQL Error in loadUserFeesSummary: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            JOptionPane.showMessageDialog(this,
                "Error loading user fees summary: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        
        System.out.println("=== End of User Fees Summary Loading ===\n");
    }

    private void loadRentals() {
        rentalsModel.setRowCount(0);
        double totalLateFees = 0.0;
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                 SELECT r.*, b.title, u.username, 
                        CASE 
                            WHEN r.return_date IS NULL AND r.due_date < datetime('now') 
                            THEN (julianday('now') - julianday(r.due_date)) * ?
                            WHEN r.return_date > r.due_date
                            THEN (julianday(r.return_date) - julianday(r.due_date)) * ?
                            ELSE r.late_fee 
                        END as calculated_fee
                 FROM rentals r 
                 JOIN books b ON r.book_id = b.book_id 
                 JOIN users u ON r.user_id = u.id
                 ORDER BY r.rental_date DESC
                 """)) {
            
            pstmt.setDouble(1, lateFeeRate);
            pstmt.setDouble(2, lateFeeRate);
            ResultSet rs = pstmt.executeQuery();
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            while (rs.next()) {
                String rentalDateStr = rs.getString("rental_date");
                String dueDateStr = rs.getString("due_date");
                String returnDateStr = rs.getString("return_date");
                double lateFee = rs.getDouble("calculated_fee");
                totalLateFees += lateFee;
                
                // Format dates with time
                String formattedRentalDate = "";
                String formattedDueDate = "";
                String formattedReturnDate = "";
                
                try {
                    if (rentalDateStr != null) {
                        java.util.Date date = parseFormat.parse(rentalDateStr);
                        formattedRentalDate = displayFormat.format(date);
                    }
                    if (dueDateStr != null) {
                        java.util.Date date = parseFormat.parse(dueDateStr);
                        formattedDueDate = displayFormat.format(date);
                    }
                    if (returnDateStr != null) {
                        java.util.Date date = parseFormat.parse(returnDateStr);
                        formattedReturnDate = displayFormat.format(date);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("username"),
                    formattedRentalDate,
                    formattedDueDate,
                    formattedReturnDate,
                    rs.getString("status"),
                    String.format("₱%.2f", lateFee)
                };
                rentalsModel.addRow(row);
            }
            
            // Update total label
            JPanel rentalPanel = (JPanel) contentPanel.getComponent(2); // Rental Management panel
            Component[] components = rentalPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getComponentCount() > 0 && panel.getComponent(0) instanceof JLabel) {
                        JLabel label = (JLabel) panel.getComponent(0);
                        if (label.getText().startsWith("Total Late Fees:")) {
                            label.setText(String.format("Total Late Fees: ₱%.2f", totalLateFees));
                            break;
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading rentals: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchRentals(String query) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                SELECT r.*, b.title, u.full_name 
                FROM rentals r 
                JOIN books b ON r.book_id = b.id 
                JOIN users u ON r.user_id = u.id
                WHERE b.title LIKE ? OR u.full_name LIKE ?
                ORDER BY r.rental_date DESC
             """)) {
            
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            rentalsModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("full_name"),
                    rs.getString("rental_date"),
                    rs.getString("due_date"),
                    rs.getString("return_date"),
                    rs.getString("status"),
                    calculateLateFee(rs.getString("due_date"), rs.getString("return_date"))
                };
                rentalsModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error searching rentals: " + e.getMessage());
        }
    }

    private String calculateLateFee(String dueDate, String returnDate) {
        if (returnDate == null) {
            // If book is not returned yet, calculate based on current date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date due = sdf.parse(dueDate);
                java.util.Date now = new java.util.Date();
                
                if (now.after(due)) {
                    long diff = now.getTime() - due.getTime();
                    int daysLate = (int) (diff / (24 * 60 * 60 * 1000));
                    double fee = daysLate * lateFeeRate;
                    return String.format("₱%.2f", fee);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "₱0.00";
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date due = sdf.parse(dueDate);
            java.util.Date returned = sdf.parse(returnDate);
            
            if (returned.after(due)) {
                long diff = returned.getTime() - due.getTime();
                int daysLate = (int) (diff / (24 * 60 * 60 * 1000));
                double fee = daysLate * lateFeeRate;
                return String.format("₱%.2f", fee);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "₱0.00";
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                new LoginForm(null).setVisible(true);
            });
        }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);
        
        // Full Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        JTextField fullNameField = new JTextField(20);
        panel.add(fullNameField, gbc);
        
        // Email
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        panel.add(emailField, gbc);
        
        // Phone
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        JTextField phoneField = new JTextField(20);
        panel.add(phoneField, gbc);
        
        // Role
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"User", "Admin"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        panel.add(roleCombo, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Please fill in all required fields",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, full_name, email, phone, role, status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                
                pstmt.setString(1, username);
                pstmt.setString(2, password); // In production, this should be hashed
                pstmt.setString(3, fullName);
                pstmt.setString(4, email);
                pstmt.setString(5, phone);
                pstmt.setString(6, role);
                pstmt.setString(7, "Active");
                
                pstmt.executeUpdate();
                dialog.dispose();
                loadData(); // Refresh the table
                
                JOptionPane.showMessageDialog(this,
                    "User added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Error adding user: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
} 