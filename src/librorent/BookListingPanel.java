package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class BookListingPanel extends BasePanel {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> genreFilter;
    private JComboBox<String> formatFilter;
    private JComboBox<String> statusFilter;
    private JButton searchButton;
    private JButton resetButton;
    private JLabel totalBooksLabel;
    private int currentUserId;

    public BookListingPanel() {
        initializeComponents();
        createLayout();
        loadData();
    }

    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "ISBN", "Format", "Genre", "Copies", "Status", };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table with custom renderer for tooltips
        bookTable = new JTable(tableModel);
        bookTable.setFillsViewportHeight(true);
        bookTable.setRowHeight(30);
        bookTable.setShowGrid(true);
        bookTable.setGridColor(new Color(200, 200, 200));
        bookTable.getTableHeader().setBackground(new Color(70, 130, 180));
        bookTable.getTableHeader().setForeground(Color.WHITE);
        bookTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        bookTable.setSelectionBackground(new Color(230, 240, 250));
        bookTable.setSelectionForeground(Color.BLACK);
        bookTable.setFont(new Font("Arial", Font.PLAIN, 12));

        // Add custom renderer for tooltips
        bookTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text);
                }
                return c;
            }
        });

        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // ISBN
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Format
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Genre
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Copies
        bookTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Status

        // Create search components
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        searchButton.setBackground(new Color(70, 130, 180));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        
        // Create refresh button with gradient
        resetButton = new JButton("Refresh") {
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
        resetButton.setForeground(Color.BLUE);
        resetButton.setFont(new Font("Arial", Font.BOLD, 12));
        resetButton.setFocusPainted(false);
        resetButton.setBorderPainted(false);
        resetButton.setContentAreaFilled(false);
        resetButton.setOpaque(true);

        // Create filter components
        genreFilter = new JComboBox<>(new String[]{"All Genres", "Fiction", "Non-Fiction", "Mystery", "Science Fiction", "Romance", "Biography"});
        formatFilter = new JComboBox<>(new String[]{"All Formats", "E-Book", "Physical"});
        statusFilter = new JComboBox<>(new String[]{"All Status", "Available", "Rented", "Reserved"});

        // Create total books label
        totalBooksLabel = new JLabel("Total Books: 0");
        totalBooksLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Add action listeners
        searchButton.addActionListener(e -> applyFilters());
        resetButton.addActionListener(e -> loadData());
        searchField.addActionListener(e -> applyFilters());
        genreFilter.addActionListener(e -> applyFilters());
        formatFilter.addActionListener(e -> applyFilters());
        statusFilter.addActionListener(e -> applyFilters());
    }

    private void createLayout() {
        // Create header panel with total books count
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
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
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        totalBooksLabel.setForeground(Color.WHITE);
        headerPanel.add(totalBooksLabel);

        // Create search panel
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
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        // Create filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
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
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        filterPanel.add(new JLabel("Genre:"));
        filterPanel.add(genreFilter);
        filterPanel.add(new JLabel("Format:"));
        filterPanel.add(formatFilter);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusFilter);

        // Create top panel to hold header and search/filter
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        // Add components to content area
        contentArea.setLayout(new BorderLayout());
        contentArea.add(topPanel, BorderLayout.NORTH);
        
        // Add table with scroll pane
        JScrollPane scrollPane = new JScrollPane(bookTable);
        contentArea.add(scrollPane, BorderLayout.CENTER);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM books ORDER BY title")) {
            
            ResultSet rs = pstmt.executeQuery();
            int totalBooks = 0;
            while (rs.next()) {
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getString("format"),
                    rs.getString("genre"),
                    rs.getInt("copies"),
                    rs.getString("status"),
                    rs.getString("last_updated")
                };
                tableModel.addRow(row);
                totalBooks++;
            }
            totalBooksLabel.setText("Total Books: " + totalBooks);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedGenre = (String) genreFilter.getSelectedItem();
        String selectedFormat = (String) formatFilter.getSelectedItem();
        String selectedStatus = (String) statusFilter.getSelectedItem();

        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM books WHERE " +
                 "(? = 'All Genres' OR genre = ?) AND " +
                 "(? = 'All Formats' OR format = ?) AND " +
                 "(? = 'All Status' OR status = ?) AND " +
                 "(title LIKE ? OR author LIKE ?) " +
                 "ORDER BY title")) {
            
            pstmt.setString(1, selectedGenre);
            pstmt.setString(2, selectedGenre);
            pstmt.setString(3, selectedFormat);
            pstmt.setString(4, selectedFormat);
            pstmt.setString(5, selectedStatus);
            pstmt.setString(6, selectedStatus);
            pstmt.setString(7, "%" + searchText + "%");
            pstmt.setString(8, "%" + searchText + "%");

            ResultSet rs = pstmt.executeQuery();
            int totalBooks = 0;
            while (rs.next()) {
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getString("format"),
                    rs.getString("genre"),
                    rs.getInt("copies"),
                    rs.getString("status"),
                    rs.getString("last_updated")
                };
                tableModel.addRow(row);
                totalBooks++;
            }
            totalBooksLabel.setText("Total Books: " + totalBooks);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error applying filters: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    @Override
    protected String getTitle() {
        return "Book Catalog";
    }
} 