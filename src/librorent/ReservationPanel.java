package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ReservationPanel extends BasePanel {
    private JTable bookTable;
    private JTable reservationTable;
    private DefaultTableModel bookTableModel;
    private DefaultTableModel reservationTableModel;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private int currentUserId;
    
    public ReservationPanel() {
        super();
        initializeComponents();
    }
    
    @Override
    protected String getTitle() {
        return "Reservation System";
    }
    
    private void initializeComponents() {
        // Create split pane for reservation operations and reservation history
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(5);
        
        // Top panel for reservation operations
        JPanel reservationPanel = new JPanel(new BorderLayout());
        reservationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Search and filter panel with gradient background
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
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
        searchPanel.setPreferredSize(new Dimension(0, 50));
        
        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search books...");
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterBooks();
            }
        });
        
        String[] filterOptions = {"All Books", "Available", "Reserved"};
        filterCombo = new JComboBox<>(filterOptions);
        filterCombo.setPreferredSize(new Dimension(120, 30));
        filterCombo.addActionListener(e -> filterBooks());
        
        JLabel searchLabel = new JLabel("Search: ");
        searchLabel.setForeground(Color.WHITE);
        JLabel filterLabel = new JLabel("Filter: ");
        filterLabel.setForeground(Color.WHITE);
        
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(filterLabel);
        searchPanel.add(filterCombo);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.addActionListener(e -> {
            loadBooks();
            loadReservations();
        });
        searchPanel.add(refreshButton);
        
        // Book table
        String[] bookColumns = {"ID", "Title", "Author", "Format", "Status", "Available Copies", "Action"};
        bookTableModel = new DefaultTableModel(bookColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only action column is editable
            }
        };
        
        bookTable = new JTable(bookTableModel);
        bookTable.setRowHeight(30);
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Format
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Status
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Available Copies
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // Action
        
        // Custom table renderer for alternating row colors and status colors
        bookTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    
                    // Color status column
                    if (column == 4) { // Status column
                        String status = value.toString();
                        switch (status) {
                            case "Available":
                                c.setForeground(new Color(46, 204, 113)); // Green
                                break;
                            case "Reserved":
                                c.setForeground(new Color(231, 76, 60));  // Red
                                break;
                            default:
                                c.setForeground(new Color(52, 152, 219)); // Blue
                        }
                    } else if (column == 5) { // Available Copies column
                        try {
                            int copies = Integer.parseInt(value.toString());
                            if (copies > 0) {
                                c.setForeground(new Color(46, 204, 113)); // Green
                            } else {
                                c.setForeground(new Color(231, 76, 60));  // Red
                            }
                        } catch (NumberFormatException e) {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
        
        // Add button column
        TableColumn buttonColumn = bookTable.getColumnModel().getColumn(6);
        buttonColumn.setCellRenderer(new ButtonRenderer());
        buttonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane bookScrollPane = new JScrollPane(bookTable);
        bookScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        reservationPanel.add(searchPanel, BorderLayout.NORTH);
        reservationPanel.add(bookScrollPane, BorderLayout.CENTER);
        
        // Bottom panel for reservation history
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("My Reservations"));
        
        String[] reservationColumns = {"Book ID", "Title", "Reservation Date", "Expiry Date", "Status", "Reserved Copies", "Action"};
        reservationTableModel = new DefaultTableModel(reservationColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only action column is editable
            }
        };
        
        reservationTable = new JTable(reservationTableModel);
        reservationTable.setFillsViewportHeight(true);
        reservationTable.setRowHeight(30);
        reservationTable.setShowGrid(true);
        reservationTable.setGridColor(new Color(200, 200, 200));
        reservationTable.getTableHeader().setBackground(new Color(70, 130, 180));
        reservationTable.getTableHeader().setForeground(Color.WHITE);
        reservationTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        reservationTable.setSelectionBackground(new Color(230, 240, 250));
        reservationTable.setSelectionForeground(Color.BLACK);
        reservationTable.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Add custom renderer for tooltips and status colors
        reservationTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Set tooltip for all cells
                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text);
                }
                    
                    // Color status column
                    if (column == 4) { // Status column
                        String status = value.toString();
                        switch (status) {
                            case "Active":
                                c.setForeground(new Color(46, 204, 113)); // Green
                                break;
                            case "Expired":
                                c.setForeground(new Color(231, 76, 60));  // Red
                                break;
                            case "Cancelled":
                                c.setForeground(new Color(149, 165, 166)); // Gray
                                break;
                            default:
                                c.setForeground(new Color(52, 152, 219)); // Blue
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                
                // Set alternating row colors
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                }
                
                return c;
            }
        });
        
        // Add button column to reservation table
        TableColumn reservationButtonColumn = reservationTable.getColumnModel().getColumn(6);
        reservationButtonColumn.setCellRenderer(new ButtonRenderer());
        reservationButtonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane reservationScrollPane = new JScrollPane(reservationTable);
        historyPanel.add(reservationScrollPane, BorderLayout.CENTER);
        
        splitPane.setTopComponent(reservationPanel);
        splitPane.setBottomComponent(historyPanel);
        
        contentArea.add(splitPane, BorderLayout.CENTER);
        
        // Load initial data
        loadBooks();
        loadReservations();
    }
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadReservations();
    }
    
    private void loadBooks() {
        bookTableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY book_id DESC")) {
            
            while (rs.next()) {
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getInt("copies"),
                    "Reserve"
                };
                bookTableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void checkAndUpdateExpiredReservations() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Get all active reservations that have expired
                String query = """
                    SELECT r.id, r.book_id, r.copies 
                    FROM reservations r 
                    WHERE r.status = 'Active' 
                    AND datetime(r.expiration_date) < datetime('now')
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    ResultSet rs = pstmt.executeQuery();
                    
                    while (rs.next()) {
                        int reservationId = rs.getInt("id");
                        int bookId = rs.getInt("book_id");
                        int copies = rs.getInt("copies");
                        
                        // Update reservation status to Expired
                        try (PreparedStatement updateReservation = conn.prepareStatement(
                                "UPDATE reservations SET status = 'Expired' WHERE id = ?")) {
                            updateReservation.setInt(1, reservationId);
                            updateReservation.executeUpdate();
                        }
                        
                        // Update book copies and status
                        try (PreparedStatement updateBook = conn.prepareStatement(
                                "UPDATE books SET copies = copies + ?, status = 'Available' WHERE book_id = ?")) {
                            updateBook.setInt(1, copies);
                            updateBook.setInt(2, bookId);
                            updateBook.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error updating expired reservations: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadReservations() {
        if (currentUserId <= 0) return;
        
        // First check and update any expired reservations
        checkAndUpdateExpiredReservations();
        
        reservationTableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                SELECT r.*, b.title 
                FROM reservations r 
                JOIN books b ON r.book_id = b.book_id 
                WHERE r.user_id = ?
                ORDER BY r.reservation_date DESC
             """)) {
            
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("reservation_date"),
                    rs.getString("expiration_date"),
                    rs.getString("status"),
                    rs.getInt("copies"),
                    "Cancel"
                };
                reservationTableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading reservations: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void filterBooks() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        String filter = (String) filterCombo.getSelectedItem();
        
        bookTableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                SELECT * FROM books 
                WHERE (LOWER(title) LIKE ? OR LOWER(author) LIKE ?)
                AND (? = 'All Books' OR status = ?)
                ORDER BY book_id DESC
             """)) {
            
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, filter);
            pstmt.setString(4, filter.equals("Available") ? "Available" : 
                              filter.equals("Reserved") ? "Reserved" : "Available");
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    "B" + rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getInt("copies"),
                    "Reserve"
                };
                bookTableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error filtering books: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void reserveBook(int bookId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Check if book is available and get available copies
            int availableCopies = 0;
            String bookTitle = "";
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT copies, title FROM books WHERE book_id = ?")) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this,
                        "Book not found",
                        "Reservation Error",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                availableCopies = rs.getInt("copies");
                bookTitle = rs.getString("title");
                
                if (availableCopies <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "No copies available for this book",
                        "Reservation Error",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            // Show dialog to select number of copies
            String[] options = new String[availableCopies];
            for (int i = 0; i < availableCopies; i++) {
                options[i] = String.valueOf(i + 1);
            }
            
            String selectedCopies = (String) JOptionPane.showInputDialog(
                this,
                "How many copies of '" + bookTitle + "' would you like to reserve?\nAvailable copies: " + availableCopies,
                "Select Copies",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (selectedCopies == null) {
                return; // User cancelled
            }
            
            int copiesToReserve = Integer.parseInt(selectedCopies);
            
            // Create reservation
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO reservations (user_id, book_id, reservation_date, expiration_date, status, copies) " +
                    "VALUES (?, ?, datetime('now'), datetime('now', '+24 hours'), 'Active', ?)")) {
                pstmt.setInt(1, currentUserId);
                pstmt.setInt(2, bookId);
                pstmt.setInt(3, copiesToReserve);
                pstmt.executeUpdate();
                
                // Update book copies
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE books SET copies = copies - ? WHERE book_id = ?")) {
                    updateStmt.setInt(1, copiesToReserve);
                    updateStmt.setInt(2, bookId);
                    updateStmt.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this,
                    "Successfully reserved " + copiesToReserve + " copy/copies of '" + bookTitle + "'!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh tables
                loadBooks();
                loadReservations();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error creating reservation: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cancelReservation(int bookId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            try {
                // Get the number of copies from the active reservation
                int reservedCopies = 0;
                try (PreparedStatement getCopiesStmt = conn.prepareStatement(
                        "SELECT copies FROM reservations WHERE book_id = ? AND user_id = ? AND status = 'Active'")) {
                    getCopiesStmt.setInt(1, bookId);
                    getCopiesStmt.setInt(2, currentUserId);
                    ResultSet rs = getCopiesStmt.executeQuery();
                    if (rs.next()) {
                        reservedCopies = rs.getInt("copies");
                    }
                }

            // Update reservation status
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE reservations SET status = 'Cancelled' WHERE book_id = ? AND user_id = ? AND status = 'Active'")) {
                pstmt.setInt(1, bookId);
                pstmt.setInt(2, currentUserId);
                int updated = pstmt.executeUpdate();
                
                if (updated > 0) {
                        // Update book copies and status
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE books SET copies = copies + ?, status = CASE WHEN copies + ? > 0 THEN 'Available' ELSE status END WHERE book_id = ?")) {
                            updateStmt.setInt(1, reservedCopies);
                            updateStmt.setInt(2, reservedCopies);
                            updateStmt.setInt(3, bookId);
                        updateStmt.executeUpdate();
                    }
                        
                        // Commit transaction
                        conn.commit();
                    
                    JOptionPane.showMessageDialog(this,
                        "Reservation cancelled successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh tables
                    loadBooks();
                    loadReservations();
                } else {
                        conn.rollback();
                    JOptionPane.showMessageDialog(this,
                        "No active reservation found for this book",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error cancelling reservation: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Custom button renderer
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
    
    // Custom button editor
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int bookId;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            bookId = Integer.parseInt(table.getValueAt(row, 0).toString().substring(1));
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                if (label.equals("Reserve")) {
                    reserveBook(bookId);
                } else if (label.equals("Cancel")) {
                    cancelReservation(bookId);
                }
            }
            isPushed = false;
            return label;
        }
    }
} 