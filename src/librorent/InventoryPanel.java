package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class InventoryPanel extends BasePanel {
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private JLabel totalBooksLabel;
    private JLabel totalCopiesLabel;
    private JLabel availableCopiesLabel;
    private JLabel rentedCopiesLabel;
    
    public InventoryPanel() {
        initializeComponents();
        createLayout();
        loadData();
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "Format", "Genre", "Total Copies", "Available", "Rented", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table with custom styling
        inventoryTable = new JTable(tableModel);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setRowHeight(30);
        inventoryTable.setShowGrid(false);
        inventoryTable.setIntercellSpacing(new Dimension(0, 0));
        inventoryTable.getTableHeader().setBackground(new Color(230, 126, 34));  // Orange
        inventoryTable.getTableHeader().setForeground(Color.WHITE);
        inventoryTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Set column widths
        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Format
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Genre
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Total Copies
        inventoryTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Available
        inventoryTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Rented
        inventoryTable.getColumnModel().getColumn(8).setPreferredWidth(100);  // Status
        
        // Custom table renderer for alternating row colors
        inventoryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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
        
        // Create statistics labels with custom styling
        totalBooksLabel = createStatLabel("Total Books: 0");
        totalCopiesLabel = createStatLabel("Total Copies: 0");
        availableCopiesLabel = createStatLabel("Available Copies: 0");
        rentedCopiesLabel = createStatLabel("Rented Copies: 0");
    }
    
    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
    
    private void createLayout() {
        // Create header with gradient
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(230, 126, 34);  // Orange
                Color color2 = new Color(211, 84, 0);    // Darker Orange
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setPreferredSize(new Dimension(0, 60));
        
        JLabel headerLabel = new JLabel("Inventory Management");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Add refresh button
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
        refreshButton.addActionListener(e -> loadData());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Create statistics panel with gradient
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 20, 20)) {
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
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create stat cards
        statsPanel.add(createStatCard(totalBooksLabel, new Color(52, 152, 219)));      // Blue
        statsPanel.add(createStatCard(totalCopiesLabel, new Color(46, 204, 113)));     // Green
        statsPanel.add(createStatCard(availableCopiesLabel, new Color(155, 89, 182))); // Purple
        statsPanel.add(createStatCard(rentedCopiesLabel, new Color(231, 76, 60)));     // Red
        
        // Add components to content area
        contentArea.setLayout(new BorderLayout(10, 10));
        contentArea.setBackground(new Color(245, 245, 245));
        contentArea.add(headerPanel, BorderLayout.NORTH);
        contentArea.add(statsPanel, BorderLayout.CENTER);
        
        // Add table with scroll pane
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        contentArea.add(scrollPane, BorderLayout.SOUTH);
    }
    
    private JPanel createStatCard(JLabel label, Color color) {
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
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        card.add(label, BorderLayout.CENTER);
        
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
    
    private void loadData() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Load inventory data
            String query = """
                SELECT b.*, 
                       (SELECT COUNT(*) FROM rentals r 
                        WHERE r.book_id = b.book_id 
                        AND r.return_date IS NULL) as rented_copies
                FROM books b
                ORDER BY b.title
                """;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                int totalBooks = 0;
                int totalCopies = 0;
                int totalAvailable = 0;
                int totalRented = 0;
                
                while (rs.next()) {
                    int bookId = rs.getInt("book_id");
                    int copies = rs.getInt("copies");
                    int rentedCopies = rs.getInt("rented_copies");
                    int availableCopies = Math.max(0, copies - rentedCopies); // Ensure non-negative value
                    String status = availableCopies > 0 ? "Available" : "Unavailable";
                    
                    Object[] row = {
                        "B" + bookId,
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("format"),
                        rs.getString("genre"),
                        copies,
                        availableCopies,
                        rentedCopies,
                        status
                    };
                    tableModel.addRow(row);
                    
                    totalBooks++;
                    totalCopies += copies;
                    totalAvailable += availableCopies;
                    totalRented += rentedCopies;
                }
                
                // Update statistics labels
                totalBooksLabel.setText("Total Books: " + totalBooks);
                totalCopiesLabel.setText("Total Copies: " + totalCopies);
                availableCopiesLabel.setText("Available Copies: " + totalAvailable);
                rentedCopiesLabel.setText("Rented Copies: " + totalRented);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading inventory data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    protected String getTitle() {
        return "Inventory Management";
    }
} 