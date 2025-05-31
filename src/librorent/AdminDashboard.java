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
import java.util.Calendar;

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
    private JLabel totalBooksLabel = new JLabel("0");
    private JLabel totalUsersLabel = new JLabel("0");
    private JLabel totalRentalsLabel = new JLabel("0");
    private JLabel totalRevenueLabel = new JLabel("₱0.00");
    private JLabel totalCopiesLabel = new JLabel("0");
    private JLabel totalAvailableLabel = new JLabel("0");
    private JLabel overdueBooksLabel = new JLabel("0");
    
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
        
        // Logo with enhanced styling
        JLabel logoLabel = new JLabel("📚 LibroRent");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 0));
        sidebar.add(logoLabel);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Menu items with enhanced icons and styling
        String[] menuItems = {
            "📊 Dashboard Overview",
            "📚 Book Management",
            "👥 User Management",
            "📖 Rental Management",
            "📦 Inventory"
        };
        
        for (String item : menuItems) {
            JButton button = createMenuButton(item);
            button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(10));
        }
        
        // Logout button with enhanced styling
        JButton logoutButton = createMenuButton("🚪 Logout");
        logoutButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        logoutButton.addActionListener(e -> handleLogout());
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);
        
        return sidebar;
    }
    
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(51, 51, 51));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect with smooth transition
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(70, 70, 70));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(51, 51, 51));
                button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
        });
        
        // Add click handler with visual feedback
        button.addActionListener(e -> {
            String cardName;
            switch (text) {
                case "📊 Dashboard Overview":
                    cardName = "DASHBOARD";
                    updateDashboardStats();
                    break;
                case "📚 Book Management":
                    cardName = "BOOK_MANAGEMENT";
                    break;
                case "👥 User Management":
                    cardName = "USER_MANAGEMENT";
                    break;
                case "📖 Rental Management":
                    cardName = "RENTAL_MANAGEMENT";
                    refreshRentalManagementPanel();
                    break;
                case "📦 Inventory":
                    cardName = "INVENTORY";
                    break;
                default:
                    cardName = "DASHBOARD";
            }
            cardLayout.show(contentPanel, cardName);
            
            // Visual feedback for selected button
            for (Component comp : sidebar.getComponents()) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    if (btn.getText().equals(text)) {
                        btn.setBackground(new Color(90, 90, 90));
                        btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(120, 120, 120), 1),
                            BorderFactory.createEmptyBorder(5, 10, 5, 10)
                        ));
                    } else {
                        btn.setBackground(new Color(51, 51, 51));
                        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    }
                }
            }
        });
        
        return button;
    }
    
    private void refreshRentalManagementPanel() {
        System.out.println("\n=== Refreshing Rental Management Panel ===");
        try {
            // Get the rental management panel
            JPanel rentalPanel = (JPanel) contentPanel.getComponent(3); // Index 3 is RENTAL_MANAGEMENT
            JPanel contentPanel = (JPanel) rentalPanel.getComponent(1); // Get the content panel
            JPanel userFeesPanel = (JPanel) contentPanel.getComponent(0); // Get the user fees panel
            JScrollPane scrollPane = (JScrollPane) userFeesPanel.getComponent(1); // Get the scroll pane
            JTable userFeesTable = (JTable) scrollPane.getViewport().getView();
            DefaultTableModel model = (DefaultTableModel) userFeesTable.getModel();
            
            System.out.println("Found user fees table, refreshing data...");
            loadUserFeesSummary(model);
            System.out.println("User fees data refreshed");
            
            // Also refresh the rentals table
            JPanel rentalDetailsPanel = (JPanel) contentPanel.getComponent(1);
            JScrollPane rentalsScrollPane = (JScrollPane) rentalDetailsPanel.getComponent(1);
            JTable rentalsTable = (JTable) rentalsScrollPane.getViewport().getView();
            DefaultTableModel rentalsModel = (DefaultTableModel) rentalsTable.getModel();
            
            System.out.println("Found rentals table, refreshing data...");
            loadRentals();
            System.out.println("Rentals data refreshed");
            
        } catch (Exception e) {
            System.out.println("Error refreshing rental management panel: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== Rental Management Panel Refresh Complete ===\n");
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
        JPanel statsPanel = createStatsPanel();
        panel.add(statsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create stat panels with different colors and enhanced icons
        JPanel booksPanel = createStatPanel("Total Books", "📚", totalBooksLabel, new Color(52, 152, 219));  // Blue
        JPanel unavailablePanel = createStatPanel("Unavailable Books", "🔒", new JLabel("0"), new Color(231, 76, 60));  // Red
        JPanel ebooksPanel = createStatPanel("E-Books", "📱", new JLabel("0"), new Color(46, 204, 113));  // Green
        JPanel physicalPanel = createStatPanel("Physical Books", "📖", new JLabel("0"), new Color(155, 89, 182));  // Purple
        JPanel activePanel = createStatPanel("Active Rentals", "✅", new JLabel("0"), new Color(241, 196, 15));  // Yellow
        JPanel overduePanel = createStatPanel("Overdue Books", "⚠️", overdueBooksLabel, new Color(230, 126, 34));  // Orange
        
        // Add panels to stats panel
        statsPanel.add(booksPanel);
        statsPanel.add(unavailablePanel);
        statsPanel.add(ebooksPanel);
        statsPanel.add(physicalPanel);
        statsPanel.add(activePanel);
        statsPanel.add(overduePanel);
        
        // Create a panel for total users at the bottom right with proper emoji
        JPanel usersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        usersPanel.setOpaque(false);
        JLabel usersIcon = new JLabel("👥");
        usersIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        JLabel usersText = new JLabel("Total Users: ");
        usersText.setFont(new Font("Arial", Font.BOLD, 14));
        totalUsersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        usersPanel.add(usersIcon);
        usersPanel.add(usersText);
        usersPanel.add(totalUsersLabel);
        
        // Create a container panel to hold both stats and users panel
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(statsPanel, BorderLayout.CENTER);
        containerPanel.add(usersPanel, BorderLayout.SOUTH);
        
        return containerPanel;
    }
    
    private JPanel createStatPanel(String title, String icon, JLabel valueLabel, Color backgroundColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(backgroundColor);
        
        // Create icon panel with background
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36)); // Use emoji font
        iconLabel.setForeground(Color.WHITE);
        iconPanel.add(iconLabel);
        
        // Create title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        // Create value panel
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        valuePanel.setOpaque(false);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(Color.WHITE);
        valuePanel.add(valueLabel);
        
        // Add panels to main panel
        panel.add(iconPanel, BorderLayout.NORTH);
        panel.add(titlePanel, BorderLayout.CENTER);
        panel.add(valuePanel, BorderLayout.SOUTH);
        
        // Add hover effect
        panel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(150, 150, 150)),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
            public void mouseExited(MouseEvent e) {
                panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }
        });
        
        return panel;
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
        String[] columns = {"Book ID", "Title", "Author", "ISBN", "Format", "Genre", "Copies", "Rental Fee", "Late Return Fee", "Status", "Action"};
        bookModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 10; // Only action column is editable
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
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(80);   // Rental Fee
        bookTable.getColumnModel().getColumn(8).setPreferredWidth(100);  // Late Return Fee
        bookTable.getColumnModel().getColumn(9).setPreferredWidth(100);  // Status
        bookTable.getColumnModel().getColumn(10).setPreferredWidth(100);  // Action
        
        JScrollPane scrollPane = new JScrollPane(bookTable);
        
        // Statistics panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBackground(new Color(245, 245, 245));
        totalBooksLabel = new JLabel("Total Books: 0");
        totalCopiesLabel = new JLabel("Total Copies: 0");
        totalAvailableLabel = new JLabel("Total Available: 0");
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
                    updateStatCard(1, unavailableCopies); // Unavailable Books
                }
            }
            
            // E-Books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE format = 'E-Book'")) {
                if (rs.next()) {
                    updateStatCard(2, rs.getInt(1));
                }
            }
            
            // Physical Books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE format = 'Physical'")) {
                if (rs.next()) {
                    updateStatCard(3, rs.getInt(1));
                }
            }
            
            // Active rentals - only count rentals that are currently active and not returned
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                    SELECT COUNT(*) FROM rentals 
                    WHERE status = 'Active' 
                    AND return_date IS NULL""")) {
                if (rs.next()) {
                    updateStatCard(4, rs.getInt(1));
                }
            }
            
            // Overdue books - only count currently active overdue rentals
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                    SELECT COUNT(*) FROM rentals 
                    WHERE status = 'Active' 
                    AND due_date < datetime('now')
                 """)) {
                if (rs.next()) {
                    updateStatCard(5, rs.getInt(1));
                }
            }
            
            // Total users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) {
                    updateStatCard(6, rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating book stats: " + e.getMessage());
        }
    }
    
    private void updateStatCard(int index, int value) {
        System.out.println("\n=== Updating Stat Card ===");
        System.out.println("Index: " + index + ", Value: " + value);
        
        try {
            JPanel dashboardPanel = (JPanel) contentPanel.getComponent(0);
            System.out.println("Found dashboard panel");
            
            JPanel containerPanel = (JPanel) dashboardPanel.getComponent(1);
            System.out.println("Found container panel");
            
            JPanel statsPanel = (JPanel) containerPanel.getComponent(0);
            System.out.println("Found stats panel with " + statsPanel.getComponentCount() + " components");
            
            // Check if the index is valid
            if (index < 0 || index >= statsPanel.getComponentCount()) {
                System.err.println("Invalid stat card index: " + index);
                return;
            }
            
            JPanel card = (JPanel) statsPanel.getComponent(index);
            System.out.println("Found stat card at index " + index);
            
            // Find the value panel and update its label
            boolean labelUpdated = false;
            for (Component comp : card.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    for (Component innerComp : panel.getComponents()) {
                        if (innerComp instanceof JLabel) {
                            JLabel label = (JLabel) innerComp;
                            String oldValue = label.getText();
                            if (oldValue.equals("0") || oldValue.equals("₱0.00") || 
                                oldValue.matches("\\d+") || oldValue.matches("₱\\d+\\.\\d+")) {
                                String newValue = String.valueOf(value);
                                if (index == 5) { // Overdue books card
                                    System.out.println("Updating overdue books count from " + oldValue + " to " + newValue);
                                }
                                label.setText(newValue);
                                labelUpdated = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (!labelUpdated) {
                System.out.println("WARNING: Could not find label to update in stat card " + index);
            }
            
        } catch (Exception e) {
            System.err.println("Error updating stat card: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== Stat Card Update Complete ===\n");
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
        JSpinner lateReturnFeeSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 1000.0, 5.0));
        
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
        formPanel.add(new JLabel("Rental Fee (₱):"), gbc);
        gbc.gridx = 1;
        formPanel.add(feeSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(new JLabel("Late Return Fee (₱):"), gbc);
        gbc.gridx = 1;
        formPanel.add(lateReturnFeeSpinner, gbc);
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(ev -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO books (title, author, isbn, genre, format, copies, fee, late_return_fee, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                pstmt.setString(1, titleField.getText());
                pstmt.setString(2, authorField.getText());
                pstmt.setString(3, isbnField.getText());
                pstmt.setString(4, (String)genreCombo.getSelectedItem());
                pstmt.setString(5, formatCombo.getSelectedItem().toString());
                pstmt.setInt(6, (Integer)copiesSpinner.getValue());
                pstmt.setDouble(7, (Double)feeSpinner.getValue());
                pstmt.setDouble(8, (Double)lateReturnFeeSpinner.getValue());
                pstmt.setString(9, "Available");
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
                double lateReturnFee = rs.getDouble("late_return_fee");
                if (rs.wasNull()) {
                    lateReturnFee = 5.0; // Default late return fee if null
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
                    "₱" + lateReturnFee,
                    rs.getString("status"),
                    "Edit"
                };
                model.addRow(row);
            }
            
            // Set up the edit button column
            TableColumn editColumn = bookTable.getColumnModel().getColumn(10);
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
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    // Custom table cell editor for action buttons
    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
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
                int row = table.getSelectedRow();
                String bookId = table.getValueAt(row, 0).toString();
                showEditBookDialog(bookId);
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    private void showEditBookDialog(String bookId) {
        // Remove 'B' prefix from book ID
        final String numericBookId = bookId.substring(1);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM books WHERE book_id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(numericBookId));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                JDialog dialog = new JDialog(this, "Edit Book", true);
                dialog.setLayout(new BorderLayout(10, 10));
                
                JPanel formPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                // Title
                gbc.gridx = 0; gbc.gridy = 0;
                formPanel.add(new JLabel("Title:"), gbc);
                gbc.gridx = 1;
                JTextField titleField = new JTextField(rs.getString("title"), 20);
                formPanel.add(titleField, gbc);
                
                // Author
                gbc.gridx = 0; gbc.gridy = 1;
                formPanel.add(new JLabel("Author:"), gbc);
                gbc.gridx = 1;
                JTextField authorField = new JTextField(rs.getString("author"), 20);
                formPanel.add(authorField, gbc);
                
                // ISBN
                gbc.gridx = 0; gbc.gridy = 2;
                formPanel.add(new JLabel("ISBN:"), gbc);
                gbc.gridx = 1;
                JTextField isbnField = new JTextField(rs.getString("isbn"), 20);
                formPanel.add(isbnField, gbc);
                
                // Genre
                gbc.gridx = 0; gbc.gridy = 3;
                formPanel.add(new JLabel("Genre:"), gbc);
                gbc.gridx = 1;
                JTextField genreField = new JTextField(rs.getString("genre"), 20);
                formPanel.add(genreField, gbc);
                
                // Format
                gbc.gridx = 0; gbc.gridy = 4;
                formPanel.add(new JLabel("Format:"), gbc);
                gbc.gridx = 1;
                String[] formats = {"Physical", "E-Book",};
                JComboBox<String> formatCombo = new JComboBox<>(formats);
                formatCombo.setSelectedItem(rs.getString("format"));
                formPanel.add(formatCombo, gbc);
                
                // Copies
                gbc.gridx = 0; gbc.gridy = 5;
                formPanel.add(new JLabel("Copies:"), gbc);
                gbc.gridx = 1;
                JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(rs.getInt("copies"), 0, 1000, 1));
                formPanel.add(copiesSpinner, gbc);
                
                // Rental Fee
                gbc.gridx = 0; gbc.gridy = 6;
                formPanel.add(new JLabel("Rental Fee (₱):"), gbc);
                gbc.gridx = 1;
                JSpinner feeSpinner = new JSpinner(new SpinnerNumberModel(rs.getDouble("fee"), 0.0, 1000.0, 10.0));
                formPanel.add(feeSpinner, gbc);
                
                // Late Return Fee
                gbc.gridx = 0; gbc.gridy = 7;
                formPanel.add(new JLabel("Late Return Fee (₱):"), gbc);
                gbc.gridx = 1;
                JSpinner lateFeeSpinner = new JSpinner(new SpinnerNumberModel(rs.getDouble("late_return_fee"), 0.0, 1000.0, 5.0));
                formPanel.add(lateFeeSpinner, gbc);
                
                // Status
                gbc.gridx = 0; gbc.gridy = 8;
                formPanel.add(new JLabel("Status:"), gbc);
                gbc.gridx = 1;
                String[] statuses = {"Available", "Rented", "Maintenance"};
                JComboBox<String> statusCombo = new JComboBox<>(statuses);
                statusCombo.setSelectedItem(rs.getString("status"));
                formPanel.add(statusCombo, gbc);
                
                JButton saveButton = new JButton("Save");
                JButton cancelButton = new JButton("Cancel");
                JButton deleteButton = new JButton("Delete");
                
                saveButton.addActionListener(e -> {
                    try {
                        // Update book in database
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE books SET title = ?, author = ?, isbn = ?, genre = ?, " +
                                "format = ?, copies = ?, fee = ?, late_return_fee = ?, status = ? " +
                                "WHERE book_id = ?")) {
                        
                            updateStmt.setString(1, titleField.getText());
                            updateStmt.setString(2, authorField.getText());
                            updateStmt.setString(3, isbnField.getText());
                            updateStmt.setString(4, genreField.getText());
                            updateStmt.setString(5, (String)formatCombo.getSelectedItem());
                            updateStmt.setInt(6, (Integer)copiesSpinner.getValue());
                            updateStmt.setDouble(7, (Double)feeSpinner.getValue());
                            updateStmt.setDouble(8, (Double)lateFeeSpinner.getValue());
                            updateStmt.setString(9, (String)statusCombo.getSelectedItem());
                            updateStmt.setInt(10, Integer.parseInt(numericBookId));
                            
                            updateStmt.executeUpdate();
                            
                            // Refresh book list
                        loadBooks(bookModel);
                        
                            dialog.dispose();
                            
                            JOptionPane.showMessageDialog(this,
                            "Book updated successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                            "Error updating book: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                deleteButton.addActionListener(e -> {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete this book? This action cannot be undone.",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    
                    if (choice == JOptionPane.YES_OPTION) {
                        try {
                            // First check if the book has any active rentals
                            try (PreparedStatement checkStmt = conn.prepareStatement(
                                "SELECT COUNT(*) as rental_count FROM rentals WHERE book_id = ? AND return_date IS NULL")) {
                                checkStmt.setInt(1, Integer.parseInt(numericBookId));
                                ResultSet rentalCheck = checkStmt.executeQuery();
                                
                                if (rentalCheck.next() && rentalCheck.getInt("rental_count") > 0) {
                                    JOptionPane.showMessageDialog(this,
                                        "Cannot delete book: It has active rentals. Please ensure all copies are returned first.",
                                        "Delete Error",
                                        JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                            
                            // Begin transaction
                            conn.setAutoCommit(false);
                            try {
                                // Delete related records first
                                try (PreparedStatement deleteRentalsStmt = conn.prepareStatement(
                                    "DELETE FROM rentals WHERE book_id = ?")) {
                                    deleteRentalsStmt.setInt(1, Integer.parseInt(numericBookId));
                                    deleteRentalsStmt.executeUpdate();
                                }
                                
                                // Delete related reservations
                                try (PreparedStatement deleteReservationsStmt = conn.prepareStatement(
                                    "DELETE FROM reservations WHERE book_id = ?")) {
                                    deleteReservationsStmt.setInt(1, Integer.parseInt(numericBookId));
                                    deleteReservationsStmt.executeUpdate();
                                }
                                
                                // Now delete the book
                            try (PreparedStatement deleteStmt = conn.prepareStatement(
                                 "DELETE FROM books WHERE book_id = ?")) {
                                    deleteStmt.setInt(1, Integer.parseInt(numericBookId));
                                    int rowsAffected = deleteStmt.executeUpdate();
                                    
                                    if (rowsAffected > 0) {
                                        // Commit transaction
                                        conn.commit();
                            
                                // Refresh book list
                                loadBooks(bookModel);
                            dialog.dispose();
                            
                                JOptionPane.showMessageDialog(this,
                                "Book deleted successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                                    } else {
                                        conn.rollback();
                                        JOptionPane.showMessageDialog(this,
                                            "Book not found or already deleted.",
                                            "Delete Error",
                                            JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            } catch (SQLException ex) {
                                // Rollback transaction on error
                                conn.rollback();
                                throw ex;
                            } finally {
                                // Reset auto-commit
                                conn.setAutoCommit(true);
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            String errorMessage = "Error deleting book: ";
                            if (ex.getMessage().contains("foreign key constraint")) {
                                errorMessage += "This book has associated records in the system. Please delete related records first.";
                            } else {
                                errorMessage += ex.getMessage();
                            }
                            JOptionPane.showMessageDialog(this,
                                errorMessage,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                
                cancelButton.addActionListener(e -> dialog.dispose());
                
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(deleteButton);
                buttonPanel.add(cancelButton);
                buttonPanel.add(saveButton);
                
                dialog.add(formPanel, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading book details: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDashboardStats() {
        System.out.println("\n=== Updating Dashboard Stats ===");
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            System.out.println("Database connection established");
            
            // Get total books
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM books")) {
                if (rs.next()) {
                    int totalBooks = rs.getInt("total");
                    System.out.println("Total Books: " + totalBooks);
                    updateStatCard(0, totalBooks);
                }
            }
            
            // Get unavailable books
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                     SELECT COUNT(*) as total FROM books 
                     WHERE status != 'Available' OR copies = 0""")) {
                if (rs.next()) {
                    int unavailableBooks = rs.getInt("total");
                    System.out.println("Unavailable Books: " + unavailableBooks);
                    updateStatCard(1, unavailableBooks);
                }
            }
            
            // Get e-books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM books WHERE format = 'E-Book'")) {
                if (rs.next()) {
                    int ebooks = rs.getInt("total");
                    System.out.println("E-Books: " + ebooks);
                    updateStatCard(2, ebooks);
                }
            }
            
            // Get physical books count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM books WHERE format = 'Physical'")) {
                if (rs.next()) {
                    int physicalBooks = rs.getInt("total");
                    System.out.println("Physical Books: " + physicalBooks);
                    updateStatCard(3, physicalBooks);
                }
            }
            
            // Get active rentals
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                    SELECT COUNT(*) as active_count FROM rentals 
                    WHERE status = 'Active' 
                    AND return_date IS NULL""")) {
                if (rs.next()) {
                    int activeRentals = rs.getInt("active_count");
                    System.out.println("Active Rentals: " + activeRentals);
                    updateStatCard(4, activeRentals);
                }
            }
            
            // Get overdue books - with detailed debugging
            System.out.println("\nChecking overdue books...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("""
                    SELECT 
                        COUNT(*) as overdue_count,
                        GROUP_CONCAT(r.id || ':' || b.title || ':' || r.due_date) as overdue_details
                    FROM rentals r
                    JOIN books b ON r.book_id = b.book_id
                    WHERE r.return_date IS NULL 
                    AND r.due_date < datetime('now')
                    AND r.status = 'Active'
                 """)) {
                if (rs.next()) {
                    int overdueCount = rs.getInt("overdue_count");
                    String overdueDetails = rs.getString("overdue_details");
                    
                    System.out.println("Found " + overdueCount + " overdue books");
                    if (overdueDetails != null) {
                        System.out.println("\nOverdue Books Details:");
                        String[] details = overdueDetails.split(",");
                        for (String detail : details) {
                            String[] parts = detail.split(":");
                            if (parts.length >= 3) {
                                System.out.println("Rental ID: " + parts[0] + 
                                                 ", Book: " + parts[1] + 
                                                 ", Due Date: " + parts[2]);
                            }
                        }
                    }
                    
                    // Let's also check the current date for comparison
                    try (Statement dateStmt = conn.createStatement();
                         ResultSet dateRs = dateStmt.executeQuery("SELECT datetime('now') as current_date")) {
                        if (dateRs.next()) {
                            System.out.println("\nCurrent Date: " + dateRs.getString("current_date"));
                        }
                    }
                    
                    updateStatCard(5, overdueCount);
                } else {
                    System.out.println("No overdue books found");
                    updateStatCard(5, 0);
                }
            }
            
            // Get total users
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM users")) {
                if (rs.next()) {
                    int totalUsers = rs.getInt("total");
                    System.out.println("Total Users: " + totalUsers);
                    totalUsersLabel.setText(String.format("%,d", totalUsers));
                }
            }
            
        } catch (SQLException e) {
            System.out.println("ERROR: SQL Exception occurred");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            JOptionPane.showMessageDialog(this,
                "Error updating dashboard stats: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("=== Dashboard Stats Update Complete ===\n");
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
        System.out.println("\n=== Creating Rental Management Panel ===");
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
        
        // Load user fees data
        System.out.println("Loading user fees data...");
        loadUserFeesSummary(userFeesModel);
        System.out.println("User fees data loaded");
        
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
        
        // Add payment status buttons
        JButton markAllPaidButton = new JButton("Mark All Paid");
        markAllPaidButton.setBackground(new Color(46, 204, 113)); // Green
        markAllPaidButton.setForeground(Color.WHITE);
        markAllPaidButton.setFocusPainted(false);
        markAllPaidButton.setBorderPainted(false);

        JButton markAllPendingButton = new JButton("Mark All Pending");
        markAllPendingButton.setBackground(new Color(231, 76, 60)); // Red
        markAllPendingButton.setForeground(Color.WHITE);
        markAllPendingButton.setFocusPainted(false);
        markAllPendingButton.setBorderPainted(false);

        // Add action listeners for the buttons
        markAllPaidButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to mark all rentals as paid?",
                "Confirm Action",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                updateAllPaymentStatus("paid");
            }
        });

        markAllPendingButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to mark all rentals as pending?",
                "Confirm Action",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                updateAllPaymentStatus("pending");
            }
        });
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(Box.createHorizontalStrut(20)); // Add some spacing
        searchPanel.add(markAllPaidButton);
        searchPanel.add(markAllPendingButton);
        
        // Rentals table
        String[] columns = {"ID", "Book", "User", "Rental Date", "Due Date", "Return Date", "Status", "Payment Status", "Late Fee"};
        rentalsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only payment status column is editable
            }
        };
        
        rentalsTable = new JTable(rentalsModel);
        rentalsTable.setRowHeight(30);
        rentalsTable.setShowGrid(false);
        rentalsTable.setIntercellSpacing(new Dimension(0, 0));
        rentalsTable.getTableHeader().setBackground(new Color(52, 73, 94));
        rentalsTable.getTableHeader().setForeground(Color.WHITE);
        rentalsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Add payment status column renderer and editor
        TableColumn paymentStatusColumn = rentalsTable.getColumnModel().getColumn(7);
        paymentStatusColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = value.toString();
                    if (status.equals("paid")) {
                        c.setForeground(new Color(46, 204, 113)); // Green
                    } else {
                        c.setForeground(new Color(231, 76, 60));  // Red
                    }
                }
                return c;
            }
        });
        paymentStatusColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(new String[]{"pending", "paid"})));
        
        // Custom renderer for rentals table
        rentalsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                
                // Color coding for different columns
                if (column == 6) { // Status column
                    String status = value.toString();
                    if (status.equals("Active")) {
                        c.setForeground(new Color(46, 204, 113)); // Green for Active
                    } else if (status.equals("Returned")) {
                        c.setForeground(new Color(231, 76, 60));  // Red for Returned
                    }
                } else if (column == 7) { // Payment Status column
                    String status = value.toString();
                    if (status.equals("paid")) {
                        c.setForeground(new Color(46, 204, 113)); // Green for paid
                    } else {
                        c.setForeground(new Color(231, 76, 60));  // Red for pending
                    }
                } else if (column == 8) { // Late Fee column
                    if (value != null && value.toString().startsWith("₱")) {
                        double fee = Double.parseDouble(value.toString().substring(1));
                        if (fee > 0) {
                            c.setForeground(new Color(231, 76, 60)); // Red for fees > 0
                        } else {
                            c.setForeground(new Color(46, 204, 113)); // Green for no fees
                        }
                    }
                }
                return c;
            }
        });

        // Add mouse listener for payment status editing
        rentalsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = rentalsTable.getColumnModel().getColumnIndexAtX(e.getX());
                int row = e.getY() / rentalsTable.getRowHeight();
                
                if (row < rentalsTable.getRowCount() && row >= 0 && column == 7) { // Payment Status column
                    showPaymentStatusDialog(row);
                }
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
        
        // Add search functionality
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                searchRentals(searchTerm);
            } else {
                loadRentals();
            }
        });

        // Add sorting capability to rentalsTable
        TableRowSorter<DefaultTableModel> rentalsSorter = new TableRowSorter<>(rentalsModel);
        rentalsTable.setRowSorter(rentalsSorter);
        
        // Set up column comparators for proper sorting
        rentalsSorter.setComparator(0, Comparator.naturalOrder()); // ID
        rentalsSorter.setComparator(3, (String s1, String s2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
                return sdf.parse(s1).compareTo(sdf.parse(s2));
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        }); // Rental Date
        rentalsSorter.setComparator(4, (String s1, String s2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
                return sdf.parse(s1).compareTo(sdf.parse(s2));
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        }); // Due Date
        rentalsSorter.setComparator(5, (String s1, String s2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
                return sdf.parse(s1).compareTo(sdf.parse(s2));
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        }); // Return Date
        rentalsSorter.setComparator(8, (String s1, String s2) -> {
            // Remove ₱ symbol and parse as double for proper sorting
            double d1 = Double.parseDouble(s1.replace("₱", ""));
            double d2 = Double.parseDouble(s2.replace("₱", ""));
            return Double.compare(d1, d2);
        }); // Late Fee
        
        // Add sorting capability to userFeesTable
        TableRowSorter<DefaultTableModel> userFeesSorter = new TableRowSorter<>(userFeesModel);
        userFeesTable.setRowSorter(userFeesSorter);
        
        // Set up column comparators for proper sorting
        userFeesSorter.setComparator(0, Comparator.naturalOrder()); // User ID
        userFeesSorter.setComparator(3, Comparator.naturalOrder()); // Total Books
        userFeesSorter.setComparator(5, (String s1, String s2) -> {
            // Remove ₱ symbol and parse as double for proper sorting
            double d1 = Double.parseDouble(s1.replace("₱", ""));
            double d2 = Double.parseDouble(s2.replace("₱", ""));
            return Double.compare(d1, d2);
        }); // Total Late Fees
        userFeesSorter.setComparator(6, Comparator.naturalOrder()); // Active Rentals
        userFeesSorter.setComparator(7, Comparator.naturalOrder()); // Overdue Rentals

        return panel;
    }

    private void updateAllPaymentStatus(String status) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE rentals SET payment_status = ?")) {
            
            pstmt.setString(1, status);
            int rowsAffected = pstmt.executeUpdate();
            
            // Refresh the rentals table
            loadRentals();
            
            JOptionPane.showMessageDialog(this,
                rowsAffected + " rentals have been marked as " + status,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error updating payment status: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPaymentStatusDialog(int row) {
        int rentalId = (int) rentalsModel.getValueAt(row, 0);
        String currentStatus = (String) rentalsModel.getValueAt(row, 7);
        String bookTitle = (String) rentalsModel.getValueAt(row, 1);
        String username = (String) rentalsModel.getValueAt(row, 2);
        
        JDialog dialog = new JDialog(this, "Edit Payment Status", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.add(new JLabel("Book: " + bookTitle));
        infoPanel.add(new JLabel("User: " + username));
        infoPanel.add(new JLabel("Current Status: " + currentStatus));
        
        // Status selection panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("New Status:"));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"pending", "paid"});
        statusCombo.setSelectedItem(currentStatus);
        statusPanel.add(statusCombo);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE rentals SET payment_status = ? WHERE id = ?")) {
                
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, rentalId);
                pstmt.executeUpdate();
                
                // Update the table
                rentalsModel.setValueAt(newStatus, row, 7);
                
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    "Payment status updated successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                    "Error updating payment status: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        // Add components to main panel
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(statusPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void loadUserFeesSummary(DefaultTableModel model) {
        System.out.println("\n=== Loading User Fees Summary ===");
        model.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            System.out.println("Database connection established");
            
            // Main query for user fees summary
            String query = """
                WITH user_rentals AS (
                SELECT 
                    u.id,
                    u.username,
                    u.full_name,
                        COUNT(DISTINCT r.book_id) as total_books,
                        GROUP_CONCAT(DISTINCT b.title) as book_titles,
                        COALESCE(SUM(
                            CASE 
                                WHEN r.return_date IS NULL AND r.due_date < datetime('now') 
                                THEN (julianday('now') - julianday(r.due_date) - 1) * ?
                                WHEN r.return_date > r.due_date
                                THEN (julianday(r.return_date) - julianday(r.due_date) - 1) * ?
                                ELSE 0
                            END
                        ), 0) as total_late_fees,
                        COUNT(CASE WHEN r.return_date IS NULL THEN 1 END) as active_rentals,
                        COUNT(CASE WHEN r.return_date IS NULL AND r.due_date < datetime('now') THEN 1 END) as overdue_rentals
                FROM users u
                    LEFT JOIN rentals r ON u.id = r.user_id
                    LEFT JOIN books b ON r.book_id = b.book_id
                WHERE u.role = 'Member'
                    GROUP BY u.id, u.username, u.full_name
                )
                SELECT * FROM user_rentals
                ORDER BY total_late_fees DESC, active_rentals DESC, overdue_rentals DESC
             """;
            
            System.out.println("Executing query with late fee rate: " + lateFeeRate);
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDouble(1, lateFeeRate);
                pstmt.setDouble(2, lateFeeRate);
                
                System.out.println("Query parameters set");
                System.out.println("Executing query...");
                
                ResultSet rs = pstmt.executeQuery();
                System.out.println("Query executed successfully");
                
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    int userId = rs.getInt("id");
                    String username = rs.getString("username");
                    String fullName = rs.getString("full_name");
                    int totalBooks = rs.getInt("total_books");
                    String bookTitles = rs.getString("book_titles");
                    double totalLateFees = rs.getDouble("total_late_fees");
                    int activeRentals = rs.getInt("active_rentals");
                    int overdueRentals = rs.getInt("overdue_rentals");
                    
                    System.out.println("\nProcessing user record #" + rowCount + ":");
                    System.out.println("User ID: " + userId);
                    System.out.println("Username: " + username);
                    System.out.println("Full Name: " + fullName);
                    System.out.println("Total Books: " + totalBooks);
                    System.out.println("Book Titles: " + bookTitles);
                    System.out.println("Total Late Fees: " + totalLateFees);
                    System.out.println("Active Rentals: " + activeRentals);
                    System.out.println("Overdue Rentals: " + overdueRentals);
                    
                    // Format book titles
                    String formattedTitles = "No books rented";
                    if (bookTitles != null && !bookTitles.isEmpty()) {
                        String[] titles = bookTitles.split(",");
                        if (titles.length > 3) {
                            formattedTitles = String.join(", ", Arrays.copyOfRange(titles, 0, 3)) + 
                                            " and " + (titles.length - 3) + " more";
                    } else {
                            formattedTitles = bookTitles;
                        }
                    }
                    
                    System.out.println("Formatted Titles: " + formattedTitles);
                    
                    Object[] row = {
                        userId,
                        username,
                        fullName,
                        totalBooks,
                        formattedTitles,
                        String.format("₱%.2f", totalLateFees),
                        activeRentals,
                        overdueRentals
                    };
                    model.addRow(row);
                    System.out.println("Row added to table model");
                }
                
                System.out.println("\nTotal records processed: " + rowCount);
                
                if (rowCount == 0) {
                    System.out.println("WARNING: No records found in the result set");
                    // Let's check if there are any users at all
                    try (Statement stmt = conn.createStatement();
                         ResultSet userCount = stmt.executeQuery("SELECT COUNT(*) as count FROM users WHERE role = 'Member'")) {
                        if (userCount.next()) {
                            System.out.println("Total member users in database: " + userCount.getInt("count"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR: SQL Exception occurred");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            JOptionPane.showMessageDialog(this,
                "Error loading user fees summary: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        
        System.out.println("=== User Fees Summary Loading Complete ===\n");
    }

    private void loadRentals() {
        rentalsModel.setRowCount(0);
        double totalLateFees = 0.0;
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                 SELECT r.*, b.title, u.username, 
                        CASE 
                            WHEN r.return_date IS NULL AND r.due_date < datetime('now') 
                            THEN (julianday('now') - julianday(r.due_date) - 1) * ?
                            WHEN r.return_date > r.due_date
                            THEN (julianday(r.return_date) - julianday(r.due_date) - 1) * ?
                            ELSE 0
                        END as calculated_fee,
                        CASE 
                            WHEN r.return_date IS NOT NULL THEN 'Returned'
                            ELSE 'Active'
                        END as rental_status
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
                String status = rs.getString("rental_status");
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
                    status,
                    rs.getString("payment_status"),
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
        System.out.println("\n=== Starting Rental Search ===");
        System.out.println("Search Query: " + query);
        
        try (Connection conn = dbManager.getConnection()) {
            // Try to parse the query as a date
            String datePattern = null;
            try {
                // First, check if the query is a month number or name
                String processedQuery = query.trim().toLowerCase();
                System.out.println("Processing query: " + processedQuery);
                
                // Map of month numbers and abbreviations to full names
                Map<String, String> monthMap = new HashMap<>();
                // Full names
                monthMap.put("january", "January");
                monthMap.put("february", "February");
                monthMap.put("march", "March");
                monthMap.put("april", "April");
                monthMap.put("may", "May");
                monthMap.put("june", "June");
                monthMap.put("july", "July");
                monthMap.put("august", "August");
                monthMap.put("september", "September");
                monthMap.put("october", "October");
                monthMap.put("november", "November");
                monthMap.put("december", "December");
                
                // Abbreviations
                monthMap.put("jan", "January");
                monthMap.put("feb", "February");
                monthMap.put("mar", "March");
                monthMap.put("apr", "April");
                monthMap.put("jun", "June");
                monthMap.put("jul", "July");
                monthMap.put("aug", "August");
                monthMap.put("sep", "September");
                monthMap.put("sept", "September");
                monthMap.put("oct", "October");
                monthMap.put("nov", "November");
                monthMap.put("dec", "December");
                
                // Numbers (both padded and unpadded)
                monthMap.put("1", "January");
                monthMap.put("01", "January");
                monthMap.put("2", "February");
                monthMap.put("02", "February");
                monthMap.put("3", "March");
                monthMap.put("03", "March");
                monthMap.put("4", "April");
                monthMap.put("04", "April");
                monthMap.put("5", "May");
                monthMap.put("05", "May");
                monthMap.put("6", "June");
                monthMap.put("06", "June");
                monthMap.put("7", "July");
                monthMap.put("07", "July");
                monthMap.put("8", "August");
                monthMap.put("08", "August");
                monthMap.put("9", "September");
                monthMap.put("09", "September");
                monthMap.put("10", "October");
                monthMap.put("11", "November");
                monthMap.put("12", "December");
                
                String monthName = null;
                String year = null;
                
                // Split the query into parts
                String[] parts = processedQuery.split("\\s+");
                
                // Check if first part is a month identifier
                if (monthMap.containsKey(parts[0])) {
                    monthName = monthMap.get(parts[0]);
                    System.out.println("Identified month: " + monthName);
                    
                    // Check if there's a year
                    if (parts.length > 1) {
                        year = parts[1];
                        System.out.println("Identified year: " + year);
                    }
                }
                
                if (monthName != null) {
                    // Create a Calendar instance
                    Calendar cal = Calendar.getInstance();
                    
                    // Set the year (use current year if not specified)
                    if (year != null) {
                        cal.set(Calendar.YEAR, Integer.parseInt(year));
                    }
                    
                    // Set the month (Calendar months are 0-based)
                    String[] months = {"January", "February", "March", "April", "May", "June", 
                                     "July", "August", "September", "October", "November", "December"};
                    for (int i = 0; i < months.length; i++) {
                        if (months[i].equals(monthName)) {
                            cal.set(Calendar.MONTH, i);
                            break;
                        }
                    }
                    
                    // Set to first day of month
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    String startDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
                    
                    // Set to last day of month
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    String endDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
                    
                    datePattern = startDate + "|" + endDate;
                    System.out.println("Created date range: " + datePattern);
                } else {
                    // If not a month, try other date formats
                String[] dateFormats = {
                    "MMMM d, yyyy",  // May 29, 2025
                    "MMM d, yyyy",   // May 29, 2025
                    "MMMM d yyyy",   // May 29 2025
                    "MMM d yyyy",    // May 29 2025
                    "yyyy-MM-dd"     // 2025-05-29
                };
                
                    System.out.println("Attempting to parse as specific date...");
                for (String format : dateFormats) {
                    try {
                            System.out.println("Trying format: " + format);
                        SimpleDateFormat sdf = new SimpleDateFormat(format);
                        sdf.setLenient(false);
                            java.util.Date date = sdf.parse(processedQuery);
                        datePattern = new SimpleDateFormat("yyyy-MM-dd").format(date);
                            System.out.println("Successfully parsed as single date: " + datePattern);
                        break;
                    } catch (Exception e) {
                            System.out.println("Failed to parse with format " + format + ": " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Date parsing failed: " + e.getMessage());
            }
            
            System.out.println("Final date pattern: " + datePattern);
            
            // Now execute the main search query
            try (PreparedStatement pstmt = conn.prepareStatement("""
                SELECT r.*, b.title, u.username,
                       CASE 
                           WHEN r.return_date IS NULL AND r.due_date < datetime('now') 
                           THEN (julianday('now') - julianday(r.due_date) - 1) * ?
                           WHEN r.return_date > r.due_date
                           THEN (julianday(r.return_date) - julianday(r.due_date) - 1) * ?
                           ELSE 0
                       END as calculated_fee,
                       CASE 
                           WHEN r.return_date IS NOT NULL THEN 'Returned'
                           ELSE 'Active'
                       END as rental_status
                FROM rentals r 
                JOIN books b ON r.book_id = b.book_id 
                JOIN users u ON r.user_id = u.id
                WHERE LOWER(b.title) LIKE LOWER(?)
                   OR LOWER(u.username) LIKE LOWER(?)
                   OR r.id LIKE ?
                   OR LOWER(CASE 
                           WHEN r.return_date IS NOT NULL THEN 'Returned'
                           ELSE 'Active'
                        END) LIKE LOWER(?)
                   OR LOWER(r.payment_status) LIKE LOWER(?)
                   OR CAST(calculated_fee AS TEXT) LIKE ?
                   OR CAST(ROUND(calculated_fee, 2) AS TEXT) LIKE ?
                   OR (? IS NOT NULL AND (
                       strftime('%Y-%m', r.rental_date) = strftime('%Y-%m', ?)
                       OR strftime('%Y-%m', r.due_date) = strftime('%Y-%m', ?)
                       OR strftime('%Y-%m', r.return_date) = strftime('%Y-%m', ?)
                   ))
                ORDER BY r.rental_date DESC
             """)) {
            
            String searchPattern = "%" + query + "%";
                System.out.println("Search pattern: " + searchPattern);
                
                pstmt.setDouble(1, lateFeeRate);
                pstmt.setDouble(2, lateFeeRate);
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
                pstmt.setString(5, searchPattern);
                pstmt.setString(6, searchPattern);
                pstmt.setString(7, searchPattern);
                pstmt.setString(8, searchPattern);
                pstmt.setString(9, searchPattern);
                pstmt.setString(10, datePattern);
                
                if (datePattern != null) {
                    if (datePattern.contains("|")) {
                        String[] dates = datePattern.split("\\|");
                        System.out.println("Using date range: " + dates[0] + " to " + dates[1]);
                        // Use the start date for month comparison
                        pstmt.setString(11, dates[0]);
                        pstmt.setString(12, dates[0]);
                        pstmt.setString(13, dates[0]);
                    } else {
                        System.out.println("Using single date: " + datePattern);
                        pstmt.setString(11, datePattern);
                        pstmt.setString(12, datePattern);
                        pstmt.setString(13, datePattern);
                    }
                } else {
                    pstmt.setString(11, null);
                    pstmt.setString(12, null);
                    pstmt.setString(13, null);
                }
                
                System.out.println("Executing SQL query...");
            ResultSet rs = pstmt.executeQuery();
            rentalsModel.setRowCount(0);
            
                int rowCount = 0;
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
                
            while (rs.next()) {
                    rowCount++;
                    String rentalDateStr = rs.getString("rental_date");
                    String dueDateStr = rs.getString("due_date");
                    String returnDateStr = rs.getString("return_date");
                    double lateFee = rs.getDouble("calculated_fee");
                    String status = rs.getString("rental_status");
                    
                    System.out.println("Found rental: ID=" + rs.getInt("id") + 
                                     ", Date=" + rentalDateStr + 
                                     ", Due=" + dueDateStr + 
                                     ", Return=" + returnDateStr);
                    
                    // Format dates
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
                        System.out.println("Error formatting dates: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                        rs.getString("username"),
                        formattedRentalDate,
                        formattedDueDate,
                        formattedReturnDate,
                        status,
                        rs.getString("payment_status"),
                        String.format("₱%.2f", lateFee)
                };
                rentalsModel.addRow(row);
            }
                
                System.out.println("Found " + rowCount + " matching records");
                System.out.println("=== Search Complete ===\n");
                
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
                e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error searching rentals: " + e.getMessage());
        }
    }

    private String calculateLateFee(String dueDate, String returnDate) {
        if (returnDate == null) {
            // If book is not returned yet, calculate based on current date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
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