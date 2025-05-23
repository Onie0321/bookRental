package librorent;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class UserDashboardPanel extends BasePanel {
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JLabel memberIdLabel;
    private JTable currentRentalsTable;
    private JTable rentalHistoryTable;
    private DefaultTableModel currentRentalsModel;
    private DefaultTableModel rentalHistoryModel;
    private int currentUserId;
    private JButton saveButton;
    private DefaultTableModel historyTableModel;
    private DefaultTableModel currentRentalsTableModel;
    private JPanel rentalHistoryPanel;
    private JPanel currentRentalsPanel;

    public UserDashboardPanel() {
        // Initialize components
        initializeComponents();
        
        // Set current user ID from the session
        this.currentUserId = SessionManager.getInstance().getCurrentUserId();
        
        // Create main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create header panel with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
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
        headerPanel.setPreferredSize(new Dimension(0, 40));
        
        JLabel titleLabel = new JLabel("User Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create content panel with user info and rentals
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        
        // Add user info panel
        JPanel userInfoPanel = createUserInfoPanel();
        contentPanel.add(userInfoPanel, BorderLayout.NORTH);
        
        // Create rentals panel
        JPanel rentalsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        rentalsPanel.setOpaque(false);
        
        // Create current rentals panel
        currentRentalsPanel = new JPanel(new BorderLayout());
        currentRentalsPanel.setOpaque(false);
        createCurrentRentalsPanel();
        
        // Create rental history panel
        rentalHistoryPanel = new JPanel(new BorderLayout());
        rentalHistoryPanel.setOpaque(false);
        createRentalHistoryPanel();
        
        rentalsPanel.add(currentRentalsPanel);
        rentalsPanel.add(rentalHistoryPanel);
        
        contentPanel.add(rentalsPanel, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        contentArea.add(mainPanel, BorderLayout.CENTER);
        
        // Load initial data
        if (currentUserId > 0) {
            loadUserData();
            loadCurrentRentals();
            loadRentalHistory();
        }
    }

    private void initializeComponents() {
        // Create table models
        String[] currentRentalsColumns = {"Book ID", "Title", "Borrowed Date", "Due Date", "Days Left", "Status", "Late Fee"};
        currentRentalsModel = new DefaultTableModel(currentRentalsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String[] rentalHistoryColumns = {"Book ID", "Title", "Borrowed Date", "Return Date", "Status", "Late Fee"};
        rentalHistoryModel = new DefaultTableModel(rentalHistoryColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create tables with custom styling
        currentRentalsTable = new JTable(currentRentalsModel);
        currentRentalsTable.setFillsViewportHeight(true);
        currentRentalsTable.setRowHeight(30);
        currentRentalsTable.setShowGrid(true);
        currentRentalsTable.setGridColor(new Color(200, 200, 200));
        currentRentalsTable.getTableHeader().setBackground(new Color(70, 130, 180));
        currentRentalsTable.getTableHeader().setForeground(Color.WHITE);
        currentRentalsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        currentRentalsTable.setSelectionBackground(new Color(230, 240, 250));
        currentRentalsTable.setSelectionForeground(Color.BLACK);
        currentRentalsTable.setFont(new Font("Arial", Font.PLAIN, 12));

        // Add custom renderer for status column
        currentRentalsTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String status = value.toString();
                    switch (status) {
                        case "Active":
                            c.setForeground(new Color(46, 204, 113)); // Green
                            break;
                        case "Overdue":
                            c.setForeground(new Color(231, 76, 60));  // Red
                            break;
                        case "Returned":
                            c.setForeground(new Color(52, 152, 219)); // Blue
                            break;
                        default:
                            c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        rentalHistoryTable = new JTable(rentalHistoryModel);
        rentalHistoryTable.setFillsViewportHeight(true);
        rentalHistoryTable.setRowHeight(30);
        rentalHistoryTable.setShowGrid(true);
        rentalHistoryTable.setGridColor(new Color(200, 200, 200));
        rentalHistoryTable.getTableHeader().setBackground(new Color(70, 130, 180));
        rentalHistoryTable.getTableHeader().setForeground(Color.WHITE);
        rentalHistoryTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        rentalHistoryTable.setSelectionBackground(new Color(230, 240, 250));
        rentalHistoryTable.setSelectionForeground(Color.BLACK);
        rentalHistoryTable.setFont(new Font("Arial", Font.PLAIN, 12));

        // Set column widths after table creation
        setCurrentRentalsColumnWidths();
        setRentalHistoryColumnWidths();
    }

    private void setCurrentRentalsColumnWidths() {
        try {
            currentRentalsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
            currentRentalsTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
            currentRentalsTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Borrowed Date
            currentRentalsTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Due Date
            currentRentalsTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Days Left
            currentRentalsTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Status
            currentRentalsTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Late Fee
        } catch (Exception e) {
            System.err.println("Error setting current rentals column widths: " + e.getMessage());
        }
    }

    private void setRentalHistoryColumnWidths() {
        try {
            rentalHistoryTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
            rentalHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
            rentalHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Borrowed Date
            rentalHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Return Date
            rentalHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Status
            rentalHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Late Fee
        } catch (Exception e) {
            System.err.println("Error setting rental history column widths: " + e.getMessage());
        }
    }

    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 240, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Member ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1;
        memberIdLabel = new JLabel();
        memberIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        memberIdLabel.setForeground(new Color(0, 102, 204)); // Dark blue color
        memberIdLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        memberIdLabel.setBackground(Color.WHITE);
        memberIdLabel.setOpaque(true);
        panel.add(memberIdLabel, gbc);

        // User name
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        nameField.setBackground(Color.WHITE);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(nameField, gbc);

        // Email
        gbc.gridx = 2;
        gbc.gridy = 0;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 3;
        emailField = new JTextField(20);
        emailField.setEditable(false);
        emailField.setBackground(Color.WHITE);
        emailField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(emailField, gbc);

        // Phone
        gbc.gridx = 2;
        gbc.gridy = 1;
        panel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 3;
        phoneField = new JTextField(20);
        phoneField.setBackground(Color.WHITE);
        phoneField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(phoneField, gbc);

        // Save button
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        saveButton = new JButton("Save Changes");
        saveButton.setBackground(new Color(46, 204, 113)); // Green color
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.addActionListener(e -> saveUserData());
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Current Rentals Section
        JPanel currentRentalsPanel = createSectionPanel("My Current Rentals", currentRentalsTable);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(currentRentalsPanel, gbc);

        // Rental History Section
        JPanel rentalHistoryPanel = createSectionPanel("Rental History", rentalHistoryTable);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(rentalHistoryPanel, gbc);

        return panel;
    }

    private JPanel createSectionPanel(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(70, 130, 180)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Create a container panel for the table with a white background
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Add table with scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        
        // Add empty state message
        JLabel emptyLabel = new JLabel("No " + title.toLowerCase() + " found", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Create a layered panel to handle the empty state and table
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new BorderLayout());
        layeredPane.add(tableContainer, BorderLayout.CENTER);
        layeredPane.add(emptyLabel, BorderLayout.CENTER);
        
        // Add the layered pane to the main panel
        panel.add(layeredPane, BorderLayout.CENTER);
        
        // Add reminder note for current rentals
        if (title.equals("My Current Rentals")) {
            JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            reminderPanel.setBackground(Color.WHITE);
            JLabel reminderLabel = new JLabel("Please return books physically at the counter");
            reminderLabel.setForeground(new Color(150, 0, 0));
            reminderLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            reminderPanel.add(reminderLabel);
            panel.add(reminderPanel, BorderLayout.SOUTH);
        }

        return panel;
    }

    public void setCurrentUserId(int userId) {
        System.out.println("Setting current user ID: " + userId);
        this.currentUserId = userId;
        if (userId > 0) {
            loadUserData();
            loadRentals();
        } else {
            System.out.println("Invalid user ID: " + userId);
            JOptionPane.showMessageDialog(this,
                "Invalid user ID. Please log in again.",
                "Authentication Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUserData() {
        System.out.println("Loading user data for ID: " + currentUserId);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, full_name, email, phone FROM users WHERE id = ?")) {
            
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("User data found in database");
                
                // Set member ID (LIB-XXXXXX format)
                String memberId = String.format("LIB-%06d", currentUserId);
                memberIdLabel.setText(memberId);
                System.out.println("Set member ID: " + memberId);
                
                // Set user data
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                
                System.out.println("User data - Name: " + fullName + ", Email: " + email + ", Phone: " + phone);
                
                // Set the fields with user data
                nameField.setText(fullName != null ? fullName : "");
                emailField.setText(email != null ? email : "");
                phoneField.setText(phone != null ? phone : "");
                
                // Force UI update
                memberIdLabel.revalidate();
                memberIdLabel.repaint();
                nameField.revalidate();
                nameField.repaint();
                emailField.revalidate();
                emailField.repaint();
                phoneField.revalidate();
                phoneField.repaint();
            } else {
                System.out.println("No user data found for ID: " + currentUserId);
                JOptionPane.showMessageDialog(this,
                    "User data not found",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error loading user data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading user data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean verifyUserData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // First check if the users table exists
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'");
                if (!rs.next()) {
                    System.err.println("Users table does not exist!");
                    JOptionPane.showMessageDialog(this,
                        "Database error: Users table not found",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Then check if the user exists
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    System.err.println("User with ID " + currentUserId + " not found in database!");
                    JOptionPane.showMessageDialog(this,
                        "User not found in database",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // Print user data for debugging
                System.out.println("Found user in database:");
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Full Name: " + rs.getString("full_name"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Admin ID: " + rs.getInt("admin_id"));
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Database verification error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void saveUserData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE users SET full_name = ?, phone = ? WHERE id = ?")) {
            
            pstmt.setString(1, nameField.getText().trim());
            pstmt.setString(2, phoneField.getText().trim());
            pstmt.setInt(3, currentUserId);
            
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this,
                    "Profile updated successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to update profile",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error updating profile: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRentals() {
        // Clear existing data
        currentRentalsModel.setRowCount(0);
        rentalHistoryModel.setRowCount(0);
        
        if (currentUserId <= 0) {
            System.out.println("No user ID set, skipping rental load");
            return;
        }
        
        System.out.println("Loading rentals for user ID: " + currentUserId);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Load current rentals
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT r.id, b.title, b.author, r.rental_date, r.due_date, r.late_fee " +
                    "FROM rentals r " +
                    "JOIN books b ON r.book_id = b.book_id " +
                    "WHERE r.user_id = ? AND r.return_date IS NULL " +
                    "ORDER BY r.rental_date DESC")) {
                
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("rental_date"),
                        rs.getString("due_date"),
                        String.format("₱%.2f", rs.getDouble("late_fee"))
                    };
                    currentRentalsModel.addRow(row);
                }
            }
            
            // Load rental history
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT r.id, b.title, b.author, r.rental_date, r.due_date, r.return_date, r.late_fee " +
                    "FROM rentals r " +
                    "JOIN books b ON r.book_id = b.book_id " +
                    "WHERE r.user_id = ? AND r.return_date IS NOT NULL " +
                    "ORDER BY r.return_date DESC")) {
                
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("rental_date"),
                        rs.getString("due_date"),
                        rs.getString("return_date"),
                        String.format("₱%.2f", rs.getDouble("late_fee"))
                    };
                    rentalHistoryModel.addRow(row);
                }
            }
            
            System.out.println("Finished processing rentals. Current: " + (currentRentalsModel.getRowCount() > 0) + 
                ", History: " + (rentalHistoryModel.getRowCount() > 0));
            
            // Force UI update
            currentRentalsTable.revalidate();
            currentRentalsTable.repaint();
            rentalHistoryTable.revalidate();
            rentalHistoryTable.repaint();
            System.out.println("UI updated");
            
        } catch (SQLException e) {
            System.err.println("Error loading rentals: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading rentals: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Unexpected error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Add a method to refresh the dashboard
    public void refreshDashboard() {
        System.out.println("Refreshing dashboard...");
        // Ensure we have a valid user ID
        if (currentUserId > 0) {
            try {
                // Reload user data
                loadUserData();
                // Reload rentals
                loadRentals();
                // Force UI update
                revalidate();
                repaint();
                System.out.println("Dashboard refresh complete");
            } catch (Exception e) {
                System.err.println("Error refreshing dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Cannot refresh dashboard: Invalid user ID");
        }
    }

    // Add a method to handle rental updates
    public void handleRentalUpdate() {
        System.out.println("Handling rental update...");
        try {
            loadRentals();
            revalidate();
            repaint();
        } catch (Exception e) {
            System.err.println("Error handling rental update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected String getTitle() {
        return "User Dashboard";
    }

    private void createRentalHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
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
        headerPanel.setPreferredSize(new Dimension(0, 40));
        
        JLabel titleLabel = new JLabel("Rental History");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(70, 130, 180);   // Steel Blue
                Color color2 = new Color(65, 105, 225);   // Royal Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.setOpaque(true);
        refreshButton.addActionListener(e -> loadRentalHistory());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "Rental Date", "Due Date", "Return Date", "Late Fee"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        JTable historyTable = new JTable(tableModel);
        historyTable.setFillsViewportHeight(true);
        historyTable.setRowHeight(30);
        
        // Set column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Rental Date
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Due Date
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(150);  // Return Date
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Late Fee
        
        // Custom table renderer for alternating row colors
        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    
                    // Color late fee column
                    if (column == 6) { // Late Fee column
                        String feeStr = value.toString();
                        if (feeStr.startsWith("₱")) {
                            double fee = Double.parseDouble(feeStr.replace("₱", "").replace(",", ""));
                            if (fee > 0) {
                                c.setForeground(new Color(231, 76, 60));  // Red
                            } else {
                                c.setForeground(new Color(46, 204, 113)); // Green
                            }
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Store table model for later use
        this.historyTableModel = tableModel;
        
        rentalHistoryPanel.add(panel);
    }
    
    private void createCurrentRentalsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
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
        headerPanel.setPreferredSize(new Dimension(0, 40));
        
        JLabel titleLabel = new JLabel("My Current Rentals");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(70, 130, 180);   // Steel Blue
                Color color2 = new Color(65, 105, 225);   // Royal Blue
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                super.paintComponent(g);
            }
        };
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.setOpaque(true);
        refreshButton.addActionListener(e -> loadCurrentRentals());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "Rental Date", "Due Date", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        JTable currentRentalsTable = new JTable(tableModel);
        currentRentalsTable.setFillsViewportHeight(true);
        currentRentalsTable.setRowHeight(30);
        
        // Set column widths
        currentRentalsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        currentRentalsTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        currentRentalsTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        currentRentalsTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Rental Date
        currentRentalsTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Due Date
        currentRentalsTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Status
        
        // Custom table renderer for alternating row colors and status
        currentRentalsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    
                    // Color status column
                    if (column == 5) { // Status column
                        String status = value.toString();
                        if (status.equals("Overdue")) {
                            c.setForeground(new Color(231, 76, 60));  // Red
                        } else {
                            c.setForeground(new Color(46, 204, 113)); // Green
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(currentRentalsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Store table model for later use
        this.currentRentalsTableModel = tableModel;
        
        currentRentalsPanel.add(panel);
    }
    
    private void loadRentalHistory() {
        if (historyTableModel == null) return;
        
        // Clear existing data
        historyTableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT r.id, r.book_id, b.title, b.author, r.rental_date, r.due_date, r.return_date, r.late_fee " +
                          "FROM rentals r " +
                          "JOIN books b ON r.book_id = b.book_id " +
                          "WHERE r.user_id = ? AND r.return_date IS NOT NULL " +
                          "ORDER BY r.return_date DESC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Object[] row = {
                        "B" + rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        formatDate(rs.getString("rental_date")),
                        formatDate(rs.getString("due_date")),
                        formatDate(rs.getString("return_date")),
                        String.format("₱%.2f", rs.getDouble("late_fee"))
                    };
                    historyTableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading rental history: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadCurrentRentals() {
        if (currentRentalsTableModel == null) return;
        
        // Clear existing data
        currentRentalsTableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT r.id, r.book_id, b.title, b.author, r.rental_date, r.due_date " +
                          "FROM rentals r " +
                          "JOIN books b ON r.book_id = b.book_id " +
                          "WHERE r.user_id = ? AND r.return_date IS NULL " +
                          "ORDER BY r.due_date ASC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String dueDateStr = rs.getString("due_date");
                    LocalDateTime dueDate;
                    try {
                        dueDate = LocalDateTime.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception e) {
                        dueDate = LocalDate.parse(dueDateStr).atTime(23, 59, 59);
                    }
                    
                    String status = LocalDateTime.now().isAfter(dueDate) ? "Overdue" : "Active";
                    
                    Object[] row = {
                        "B" + rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        formatDate(rs.getString("rental_date")),
                        formatDate(dueDateStr),
                        status
                    };
                    currentRentalsTableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading current rentals: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String formatDate(String dateStr) {
        try {
            // Try parsing with time first
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                // If that fails, parse as date only
                LocalDate date = LocalDate.parse(dateStr);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex) {
                return dateStr; // Return original string if parsing fails
            }
        }
    }
} 