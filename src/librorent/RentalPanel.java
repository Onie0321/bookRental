package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RentalPanel extends BasePanel {
    private JTable bookTable;
    private JTable rentalTable;
    private DefaultTableModel bookTableModel;
    private DefaultTableModel rentalTableModel;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private int currentUserId;
    
    public RentalPanel() {
        super();
        initializeComponents();
    }
    
    @Override
    protected String getTitle() {
        return "Rental Management";
    }
    
    private void initializeComponents() {
        // Create split pane for rental operations and rental history
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(5);
        
        // Top panel for rental operations
        JPanel rentalPanel = new JPanel(new BorderLayout());
        rentalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Search and filter panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search books...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterBooks();
            }
        });
        
        String[] filterOptions = {"All Books", "Available", "Unavailable"};
        filterCombo = new JComboBox<>(filterOptions);
        filterCombo.addActionListener(e -> filterBooks());
        
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Filter: "));
        searchPanel.add(filterCombo);
        
        // Book table
        String[] bookColumns = {"ID", "Title", "Author", "Format", "Status", "Action"};
        bookTableModel = new DefaultTableModel(bookColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column is editable
            }
        };
        
        bookTable = new JTable(bookTableModel);
        bookTable.setRowHeight(30);
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        // Add button column
        TableColumn buttonColumn = bookTable.getColumnModel().getColumn(5);
        buttonColumn.setCellRenderer(new ButtonRenderer());
        buttonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane bookScrollPane = new JScrollPane(bookTable);
        
        rentalPanel.add(searchPanel, BorderLayout.NORTH);
        rentalPanel.add(bookScrollPane, BorderLayout.CENTER);
        
        // Bottom panel for rental history
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("My Rentals"));
        
        String[] rentalColumns = {"Book ID", "Title", "Rental Date", "Due Date", "Status", "Action"};
        rentalTableModel = new DefaultTableModel(rentalColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column is editable
            }
        };
        
        rentalTable = new JTable(rentalTableModel);
        rentalTable.setRowHeight(30);
        
        // Add button column to rental table
        TableColumn returnButtonColumn = rentalTable.getColumnModel().getColumn(5);
        returnButtonColumn.setCellRenderer(new ButtonRenderer());
        returnButtonColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane rentalScrollPane = new JScrollPane(rentalTable);
        historyPanel.add(rentalScrollPane, BorderLayout.CENTER);
        
        splitPane.setTopComponent(rentalPanel);
        splitPane.setBottomComponent(historyPanel);
        
        contentArea.add(splitPane, BorderLayout.CENTER);
    }
    
    private void loadData() {
        if (currentUserId <= 0) {
            return;
        }
        
        // Clear existing data
        bookTableModel.setRowCount(0);
        rentalTableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get user's name
            String userName = "";
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT full_name FROM users WHERE id = ?")) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userName = rs.getString("full_name");
                }
            }
            
            // Create welcome message
            JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            welcomePanel.setBackground(new Color(240, 240, 240));
            JLabel welcomeLabel = new JLabel("Welcome, " + userName + "!");
            welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
            welcomePanel.add(welcomeLabel);
            
            // Add welcome panel to the top of the rental panel
            if (contentArea.getComponentCount() > 0) {
                Component mainComponent = contentArea.getComponent(0);
                if (mainComponent instanceof JSplitPane) {
                    JSplitPane splitPane = (JSplitPane) mainComponent;
                    Component topComponent = splitPane.getTopComponent();
                    if (topComponent instanceof JPanel) {
                        JPanel topPanel = (JPanel) topComponent;
                        topPanel.add(welcomePanel, BorderLayout.NORTH);
                    }
                }
            }
            
            // Load available books
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM books WHERE copies > 0 ORDER BY book_id DESC")) {
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("format"),
                        rs.getInt("copies") + " available",
                        "Rent"
                    };
                    bookTableModel.addRow(row);
                }
            }
            
            // Load user's active rentals
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT r.*, b.title FROM rentals r " +
                    "JOIN books b ON r.book_id = b.book_id " +
                    "WHERE r.user_id = ? AND r.status = 'Active'")) {
                
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("rental_date"),
                        rs.getString("due_date"),
                        rs.getString("status"),
                        "Return"
                    };
                    rentalTableModel.addRow(row);
                }
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading data: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void filterBooks() {
        String searchText = searchField.getText().toLowerCase();
        String filterOption = (String) filterCombo.getSelectedItem();
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(bookTableModel);
        bookTable.setRowSorter(sorter);
        
        RowFilter<DefaultTableModel, Object> filter = new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String title = entry.getStringValue(1).toLowerCase();
                String author = entry.getStringValue(2).toLowerCase();
                String status = entry.getStringValue(4).toLowerCase();
                
                boolean matchesSearch = title.contains(searchText) || author.contains(searchText);
                boolean matchesFilter = true;
                
                if (filterOption != null) {
                    switch (filterOption) {
                        case "Available":
                            matchesFilter = status.contains("available");
                            break;
                        case "Unavailable":
                            matchesFilter = !status.contains("available");
                            break;
                    }
                }
                
                return matchesSearch && matchesFilter;
            }
        };
        
        sorter.setRowFilter(filter);
    }
    
    private void rentBook(int bookId) {
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this,
                "Please log in to rent books",
                "Authentication Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if book has available copies
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT copies FROM books WHERE book_id = ?")) {
                    pstmt.setInt(1, bookId);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next() || rs.getInt("copies") <= 0) {
                        throw new SQLException("No copies available for this book");
                    }
                }
                
                // Calculate dates
                LocalDate today = LocalDate.now();
                LocalDate dueDate = today.plusDays(14);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                // Insert rental record
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO rentals (user_id, book_id, rental_date, due_date, status) VALUES (?, ?, ?, ?, 'Active')")) {
                    pstmt.setInt(1, currentUserId);
                    pstmt.setInt(2, bookId);
                    pstmt.setString(3, today.format(formatter));
                    pstmt.setString(4, dueDate.format(formatter));
                    pstmt.executeUpdate();
                }
                
                // Decrease available copies
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE books SET copies = copies - 1 WHERE book_id = ?")) {
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                
                // Update UI
                loadData(); // Reload all data to reflect changes
                
                JOptionPane.showMessageDialog(this,
                    "Book rented successfully! Due date: " + dueDate.format(formatter),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error renting book: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void returnBook(int bookId) {
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this,
                "Please log in to return books",
                "Authentication Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update rental record
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE rentals SET status = 'Returned', return_date = ? WHERE book_id = ? AND user_id = ? AND status = 'Active'")) {
                    pstmt.setString(1, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    pstmt.setInt(2, bookId);
                    pstmt.setInt(3, currentUserId);
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("No active rental found for this book");
                    }
                }
                
                // Increase available copies
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE books SET copies = copies + 1 WHERE book_id = ?")) {
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                
                // Update UI
                loadData(); // Reload all data to reflect changes
                
                JOptionPane.showMessageDialog(this,
                    "Book returned successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error returning book: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Button renderer for the action column
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            return this;
        }
    }
    
    // Button editor for the action column
    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
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
            label = value.toString();
            button.setText(label);
            bookId = (int) table.getValueAt(row, 0);
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                if (label.equals("Rent")) {
                    rentBook(bookId);
                } else if (label.equals("Return")) {
                    returnBook(bookId);
                }
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
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadData(); // Load data for the new user
    }
} 