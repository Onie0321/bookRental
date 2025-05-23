package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LateFeePanel extends BasePanel {
    private JTable lateFeeTable;
    private DefaultTableModel tableModel;
    private int currentUserId;
    
    private double getLateFeeRate() {
        // Default rate if settings table doesn't exist or rate not found
        return 10.0;
    }
    
    public LateFeePanel() {
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
        
        JLabel titleLabel = new JLabel("Late Fee History");
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
        refreshButton.addActionListener(e -> loadData());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Add table with custom styling
        lateFeeTable.setShowGrid(false);
        lateFeeTable.setIntercellSpacing(new Dimension(0, 0));
        lateFeeTable.getTableHeader().setBackground(new Color(70, 130, 180));
        lateFeeTable.getTableHeader().setForeground(Color.WHITE);
        lateFeeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Custom table renderer for alternating row colors
        lateFeeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    
                    // Color late fee column
                    if (column == 5) { // Late Fee column
                        double lateFee = Double.parseDouble(value.toString().replace("₱", "").replace(",", ""));
                        if (lateFee > 0) {
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
        
        JScrollPane scrollPane = new JScrollPane(lateFeeTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        contentArea.add(mainPanel, BorderLayout.CENTER);
        
        // Load data from database
        loadData();
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "Due Date", "Return Date", "Late Fee"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        lateFeeTable = new JTable(tableModel);
        lateFeeTable.setFillsViewportHeight(true);
        lateFeeTable.setRowHeight(30);
        
        // Set column widths
        lateFeeTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        lateFeeTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        lateFeeTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        lateFeeTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Due Date
        lateFeeTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Return Date
        lateFeeTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Late Fee
    }
    
    private void loadData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Load all returned books with late fees
            String query = "SELECT r.id, r.book_id, b.title, b.author, r.due_date, r.return_date, r.late_fee " +
                          "FROM rentals r " +
                          "JOIN books b ON r.book_id = b.book_id " +
                          "WHERE r.user_id = ? AND r.return_date IS NOT NULL " +
                          "ORDER BY r.return_date DESC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String dueDateStr = rs.getString("due_date");
                    String returnDateStr = rs.getString("return_date");
                    double lateFee = rs.getDouble("late_fee");
                    
                    // If late fee is not set in database, calculate it
                    if (lateFee == 0) {
                        LocalDateTime dueDate;
                        LocalDateTime returnDate;
                        
                        // Parse due date
                        try {
                            dueDate = LocalDateTime.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            try {
                                dueDate = LocalDate.parse(dueDateStr).atTime(23, 59, 59);
                            } catch (Exception ex) {
                                System.err.println("Error parsing due date: " + dueDateStr);
                                continue;
                            }
                        }
                        
                        // Parse return date
                        try {
                            returnDate = LocalDateTime.parse(returnDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        } catch (Exception e) {
                            try {
                                returnDate = LocalDate.parse(returnDateStr).atTime(23, 59, 59);
                            } catch (Exception ex) {
                                System.err.println("Error parsing return date: " + returnDateStr);
                                continue;
                            }
                        }
                        
                        if (returnDate.isAfter(dueDate)) {
                            long minutesLate = java.time.Duration.between(dueDate, returnDate).toMinutes();
                            double dailyFee = getLateFeeRate();
                            lateFee = (minutesLate / 1440.0) * dailyFee;
                        }
                    }
                    
                    // Format dates for display
                    String formattedDueDate = formatDate(dueDateStr);
                    String formattedReturnDate = formatDate(returnDateStr);
                    
                    Object[] row = {
                        "B" + rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        formattedDueDate,
                        formattedReturnDate,
                        String.format("₱%.2f", lateFee)
                    };
                    tableModel.addRow(row);
                }
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading late fee data: " + e.getMessage(),
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
    
    @Override
    protected String getTitle() {
        return "Late Fee History";
    }
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadData(); // Reload data for the new user
    }
} 