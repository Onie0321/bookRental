package librorent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;

public class BookCatalogPanel extends BasePanel {
    private DefaultTableModel tableModel;
    private JTable bookTable;
    private JComboBox<String> formatComboBox;
    private JComboBox<String> genreComboBox;
    private JComboBox<String> statusComboBox;
    private JPanel filterPanel;

    public BookCatalogPanel() {
        initializeComponents();
        
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
        
        // Add filter panel
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        
        // Add table with custom styling
        bookTable.setShowGrid(false);
        bookTable.setIntercellSpacing(new Dimension(0, 0));
        bookTable.getTableHeader().setBackground(new Color(70, 130, 180));
        bookTable.getTableHeader().setForeground(Color.WHITE);
        bookTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Custom table renderer for alternating row colors
        bookTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        contentArea.add(mainPanel, BorderLayout.CENTER);
        
        // Load initial data
        loadData();
    }

    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "ISBN", "Format", "Genre", "Copies", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        bookTable = new JTable(tableModel);
        bookTable.setFillsViewportHeight(true);
        bookTable.setRowHeight(30);
        
        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // ISBN
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Format
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Genre
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Copies
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Status
        
        // Create filter panel with gradient background
        filterPanel = new JPanel() {
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
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setPreferredSize(new Dimension(0, 50));
        
        // Create filter components with white text
        JLabel formatLabel = new JLabel("Format:");
        formatLabel.setForeground(Color.WHITE);
        formatLabel.setFont(new Font("Arial", Font.BOLD, 12));
        formatComboBox = new JComboBox<>(new String[]{"All", "E-Book", "Physical"});
        formatComboBox.setPreferredSize(new Dimension(100, 30));
        formatComboBox.setBackground(Color.WHITE);
        
        JLabel genreLabel = new JLabel("Genre:");
        genreLabel.setForeground(Color.WHITE);
        genreLabel.setFont(new Font("Arial", Font.BOLD, 12));
        genreComboBox = new JComboBox<>(new String[]{"All", "Fiction", "Non-Fiction", "Science", "History", "Biography"});
        genreComboBox.setPreferredSize(new Dimension(100, 30));
        genreComboBox.setBackground(Color.WHITE);
        
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusComboBox = new JComboBox<>(new String[]{"All", "Available", "Rented"});
        statusComboBox.setPreferredSize(new Dimension(100, 30));
        statusComboBox.setBackground(Color.WHITE);
        
        // Create refresh button with gradient
        JButton refreshButton = new JButton("Refresh") {
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
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.setOpaque(true);
        refreshButton.addActionListener(e -> loadData());
        
        // Add components to filter panel
        filterPanel.add(formatLabel);
        filterPanel.add(formatComboBox);
        filterPanel.add(genreLabel);
        filterPanel.add(genreComboBox);
        filterPanel.add(statusLabel);
        filterPanel.add(statusComboBox);
        filterPanel.add(refreshButton);
        
        // Add filter change listeners
        formatComboBox.addActionListener(e -> loadData());
        genreComboBox.addActionListener(e -> loadData());
        statusComboBox.addActionListener(e -> loadData());
    }
    
    private void loadData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Build query with filters
            StringBuilder query = new StringBuilder("SELECT * FROM books WHERE 1=1");
            
            String format = (String) formatComboBox.getSelectedItem();
            if (!"All".equals(format)) {
                query.append(" AND format = '").append(format).append("'");
            }
            
            String genre = (String) genreComboBox.getSelectedItem();
            if (!"All".equals(genre)) {
                query.append(" AND genre = '").append(genre).append("'");
            }
            
            String status = (String) statusComboBox.getSelectedItem();
            if (!"All".equals(status)) {
                query.append(" AND status = '").append(status).append("'");
            }
            
            query.append(" ORDER BY title");
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query.toString())) {
                
                while (rs.next()) {
                    Object[] row = {
                        "B" + rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("format"),
                        rs.getString("genre"),
                        rs.getInt("copies"),
                        rs.getString("status")
                    };
                    tableModel.addRow(row);
                }
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading book data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    protected String getTitle() {
        return "Book Catalog";
    }
    
    public void refreshData() {
        loadData();
    }
} 