package librorent;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RentalReturnPanel extends BasePanel {
    private JTable rentalTable;
    private DefaultTableModel tableModel;
    private JTextField bookIdField;
    private JTextField returnBookIdField;
    private int currentUserId;
    
    private double getLateFeeRate() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT value FROM settings WHERE key = 'late_fee_rate'")) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Double.parseDouble(rs.getString("value"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting late fee rate: " + e.getMessage());
        }
        // Default rate if settings table doesn't exist or rate not found
        return 10.0;
    }
    
    private int getDefaultRentalDuration() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT value FROM settings WHERE key = 'default_rental_duration'")) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value")); // Return seconds directly
            }
        } catch (SQLException e) {
            System.err.println("Error getting default rental duration: " + e.getMessage());
        }
        // Default duration if settings table doesn't exist or duration not found
        return 14 * 24 * 3600; // 14 days in seconds
    }
    
    public RentalReturnPanel() {
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
        
        // Add rental and return panels at the top in two columns
        JPanel operationsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Rental panel with matching design
        JPanel rentalSection = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 255, 255);  // White
                Color color2 = new Color(245, 245, 245);  // Light Gray
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        rentalSection.setLayout(new BoxLayout(rentalSection, BoxLayout.Y_AXIS));
        rentalSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(46, 204, 113)),
                "Rent a Book",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(46, 204, 113)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Rental book input with icon
        JPanel rentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rentPanel.setOpaque(false);
        JLabel rentIcon = new JLabel("📚");
        rentIcon.setFont(new Font("Arial", Font.PLAIN, 16));
        rentPanel.add(rentIcon);
        rentPanel.add(new JLabel("Book ID:"));
        bookIdField = new JTextField(15);
        bookIdField.setPreferredSize(new Dimension(150, 30));
        rentPanel.add(bookIdField);
        rentalSection.add(rentPanel);
        rentalSection.add(Box.createVerticalStrut(15));
        
        // Rent button with gradient
        JButton rentButton = new JButton("Rent Book") {
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
        rentButton.setForeground(Color.BLACK);
        rentButton.setFont(new Font("Arial", Font.BOLD, 14));
        rentButton.setFocusPainted(false);
        rentButton.setBorderPainted(false);
        rentButton.setContentAreaFilled(false);
        rentButton.setPreferredSize(new Dimension(150, 35));
        rentButton.setOpaque(true);
        rentButton.addActionListener(e -> rentBook());
        rentalSection.add(rentButton);
        
        operationsPanel.add(rentalSection);
        operationsPanel.add(createReturnPanel());
        mainPanel.add(operationsPanel, BorderLayout.NORTH);
        
        // Create book listing panel with gradient
        JPanel bookListPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 255, 255);  // White
                Color color2 = new Color(245, 245, 245);  // Light Gray
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        bookListPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Add header to book list panel
        JPanel bookListHeader = new JPanel(new BorderLayout()) {
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
        bookListHeader.setPreferredSize(new Dimension(0, 40));
        
        JLabel bookListLabel = new JLabel("Available Books");
        bookListLabel.setFont(new Font("Arial", Font.BOLD, 18));
        bookListLabel.setForeground(Color.WHITE);
        bookListLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        bookListHeader.add(bookListLabel, BorderLayout.WEST);
        
        // Add search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setOpaque(false);
        JTextField searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 30));
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(70, 130, 180));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.addActionListener(e -> searchBooks(searchField.getText()));
        
        // Add refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setToolTipText("Refresh the rental data");
        refreshButton.addActionListener(e -> loadData());
        
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        bookListHeader.add(searchPanel, BorderLayout.EAST);
        
        bookListPanel.add(bookListHeader, BorderLayout.NORTH);
        
        // Add table with custom styling
        rentalTable.setShowGrid(false);
        rentalTable.setIntercellSpacing(new Dimension(0, 0));
        rentalTable.getTableHeader().setBackground(new Color(70, 130, 180));
        rentalTable.getTableHeader().setForeground(Color.WHITE);
        rentalTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Custom table renderer for alternating row colors and status colors
        rentalTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                    
                    // Color status column
                    if (column == 7) { // Status column
                        String status = value.toString();
                        switch (status) {
                            case "Available":
                                c.setForeground(new Color(46, 204, 113)); // Green
                                break;
                            case "Rented":
                                c.setForeground(new Color(231, 76, 60));  // Red
                                break;
                            default:
                                c.setForeground(new Color(52, 152, 219)); // Blue
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });
        
        // Add row selection listener
        rentalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = rentalTable.getSelectedRow();
                if (selectedRow != -1) {
                    String bookId = rentalTable.getValueAt(selectedRow, 0).toString();
                    // Remove 'B' prefix and set in both fields
                    String numericId = bookId.substring(1);
                    bookIdField.setText(numericId);
                    returnBookIdField.setText(numericId);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(rentalTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bookListPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add book list panel to main panel
        mainPanel.add(bookListPanel, BorderLayout.CENTER);
        
        contentArea.add(mainPanel, BorderLayout.CENTER);
        
        // Load data from database
        loadData();
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Author", "ISBN", "Format", "Genre", "Copies", "Status", "Fee", "Total Rented"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        rentalTable = new JTable(tableModel);
        rentalTable.setFillsViewportHeight(true);
        rentalTable.setRowHeight(30);
        
        // Initialize bookIdField
        bookIdField = new JTextField(10);
        
        // Set column widths
        rentalTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Book ID
        rentalTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Title
        rentalTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Author
        rentalTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // ISBN
        rentalTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Format
        rentalTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Genre
        rentalTable.getColumnModel().getColumn(6).setPreferredWidth(60);   // Copies
        rentalTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Status
        rentalTable.getColumnModel().getColumn(8).setPreferredWidth(80);   // Fee
        rentalTable.getColumnModel().getColumn(9).setPreferredWidth(100);  // Total Rented
    }
    
    private void loadData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Load all books with rental count for current user
            String query = 
                "SELECT b.*, " +
                "       (SELECT COUNT(*) FROM rentals r " +
                "        WHERE r.book_id = b.book_id " +
                "        AND r.user_id = ? " +
                "        AND r.return_date IS NULL) as total_rented " +
                "FROM books b " +
                "ORDER BY b.title";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
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
                        String.format("₱%.2f", rs.getDouble("fee")),
                        rs.getInt("total_rented")
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
    
    private void rentBook() {
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this,
                "Please log in to rent books",
                "Authentication Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String bookId = bookIdField.getText().trim();
        
        if (bookId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a Book ID to rent a book",
                "Empty Book ID",
                JOptionPane.WARNING_MESSAGE);
            bookIdField.requestFocus();
            return;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            System.out.println("Starting rental process for book ID: " + bookId + " by user: " + currentUserId);
            
            // Check if book exists and has available copies
            String bookTitle = "";
            String bookAuthor = "";
            double bookFee = 0.0;
            int totalCopies = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT title, author, copies, status, fee FROM books WHERE book_id = ?")) {
                pstmt.setInt(1, Integer.parseInt(bookId));
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    throw new SQLException("Book not found");
                }
                if (rs.getInt("copies") <= 0) {
                    throw new SQLException("No copies available for this book");
                }
                if (!rs.getString("status").equals("Available")) {
                    throw new SQLException("Book is not available for rental");
                }
                bookTitle = rs.getString("title");
                bookAuthor = rs.getString("author");
                bookFee = rs.getDouble("fee");
                totalCopies = rs.getInt("copies");
                System.out.println("Book is available for rental. Current copies: " + totalCopies + ", Fee: " + bookFee);
            }
            
            // Ask for number of copies to rent
            String copiesInput = JOptionPane.showInputDialog(this,
                "Enter number of copies to rent (1-" + totalCopies + "):",
                "Select Copies",
                JOptionPane.QUESTION_MESSAGE);
            
            if (copiesInput == null) {
                conn.rollback();
                return; // User cancelled
            }
            
            int copiesToRent;
            try {
                copiesToRent = Integer.parseInt(copiesInput);
                if (copiesToRent < 1 || copiesToRent > totalCopies) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                conn.rollback();
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid number between 1 and " + totalCopies,
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Calculate dates with default rental duration
            LocalDateTime now = LocalDateTime.now();
            int durationInSeconds = getDefaultRentalDuration();
            LocalDateTime dueDate = now.plusSeconds(durationInSeconds);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            // Calculate total fee
            double totalFee = bookFee * copiesToRent;
            
            // Format duration details
            int days = durationInSeconds / (24 * 3600);
            int hours = (durationInSeconds % (24 * 3600)) / 3600;
            int minutes = (durationInSeconds % 3600) / 60;
            int seconds = durationInSeconds % 60;
            
            String durationDetails = String.format("%d days, %d hours, %d minutes, %d seconds", 
                days, hours, minutes, seconds);
            
            // Show confirmation dialog with book details and fee
            String message = String.format(
                "Book Details:\n" +
                "Title: %s\n" +
                "Author: %s\n" +
                "Total Copies Available: %d\n" +
                "Copies to Rent: %d\n" +
                "Fee per Copy: ₱%.2f\n" +
                "Total Fee: ₱%.2f\n" +
                "Rental Duration: %s\n" +
                "Due Date: %s\n\n" +
                "IMPORTANT: Please pay the total fee of ₱%.2f in cash at the front desk before proceeding with the rental.\n\n" +
                "Do you want to proceed with the rental?",
                bookTitle, bookAuthor, totalCopies, copiesToRent, bookFee, totalFee, 
                durationDetails, dueDate.format(formatter), totalFee);
            
            int choice = JOptionPane.showConfirmDialog(this,
                message,
                "Confirm Rental",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (choice != JOptionPane.YES_OPTION) {
                conn.rollback();
                return;
            }
            
            // Insert rental records for each copy
            for (int i = 0; i < copiesToRent; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO rentals (user_id, book_id, rental_date, due_date) VALUES (?, ?, ?, ?)")) {
                    pstmt.setInt(1, currentUserId);
                    pstmt.setInt(2, Integer.parseInt(bookId));
                    pstmt.setString(3, now.format(formatter));
                    pstmt.setString(4, dueDate.format(formatter));
                    pstmt.executeUpdate();
                }
            }
            
            // Update book status and decrease available copies
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE books SET copies = copies - ?, status = CASE WHEN copies - ? = 0 THEN 'Rented' ELSE 'Available' END WHERE book_id = ?")) {
                pstmt.setInt(1, copiesToRent);
                pstmt.setInt(2, copiesToRent);
                pstmt.setInt(3, Integer.parseInt(bookId));
                pstmt.executeUpdate();
            }
            
            conn.commit();
            System.out.println("Transaction committed successfully");
            
            // Clear input field
            bookIdField.setText("");
            
            // Refresh rental history
            loadData();
            
            // Refresh User Dashboard panels
            refreshUserDashboard();
            
            JOptionPane.showMessageDialog(this,
                String.format("Book rented successfully!\nCopies rented: %d\nDue date: %s\nTotal fee: ₱%.2f", 
                    copiesToRent, dueDate.format(formatter), totalFee),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error renting book: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void returnBook(String bookId) {
        if (currentUserId <= 0) {
            JOptionPane.showMessageDialog(this,
                "Please log in to return books",
                "Authentication Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (bookId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a Book ID to return a book",
                "Empty Book ID",
                JOptionPane.WARNING_MESSAGE);
            returnBookIdField.requestFocus();
            return;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            // Get all active rentals for this book
            java.util.List<Object[]> activeRentals = new java.util.ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT r.id, r.due_date, b.title, b.author, b.copies, b.book_id " +
                    "FROM rentals r " +
                    "JOIN books b ON r.book_id = b.book_id " +
                    "WHERE r.book_id = ? AND r.user_id = ? AND r.return_date IS NULL " +
                    "ORDER BY r.rental_date DESC")) {
                pstmt.setInt(1, Integer.parseInt(bookId));
                pstmt.setInt(2, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    throw new SQLException("No active rentals found for this book");
                }
                
                do {
                    String dueDateStr = rs.getString("due_date");
                    LocalDateTime dueDate;
                    try {
                        dueDate = LocalDateTime.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception e) {
                        dueDate = LocalDate.parse(dueDateStr).atTime(23, 59, 59);
                    }
                    
                    double lateFee = 0.0;
                    LocalDateTime returnDate = LocalDateTime.now();
                    if (returnDate.isAfter(dueDate)) {
                        long minutesLate = java.time.Duration.between(dueDate, returnDate).toMinutes();
                        double dailyFee = getLateFeeRate();
                        lateFee = (minutesLate / 1440.0) * dailyFee;
                    }
                    
                    activeRentals.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        dueDateStr,
                        lateFee,
                        rs.getInt("book_id")
                    });
                } while (rs.next());
            }
            
            // If there's only one rental, proceed with it
            if (activeRentals.size() == 1) {
                processReturn(conn, (int)activeRentals.get(0)[0], (String)activeRentals.get(0)[1], 
                    (String)activeRentals.get(0)[2], (String)activeRentals.get(0)[3], (double)activeRentals.get(0)[4]);
                return;
            }
            
            // Create custom panel for rental selection
            JPanel selectionPanel = new JPanel(new BorderLayout(10, 10));
            selectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Add title label
            JLabel titleLabel = new JLabel("Select Rentals to Return");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            selectionPanel.add(titleLabel, BorderLayout.NORTH);
            
            // Create panel for rental buttons
            JPanel rentalsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
            rentalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            
            // List to store checkboxes
            java.util.List<JCheckBox> checkboxes = new java.util.ArrayList<>();
            
            // Add rental buttons
            for (int i = 0; i < activeRentals.size(); i++) {
                Object[] rental = activeRentals.get(i);
                JPanel rentalPanel = new JPanel();
                rentalPanel.setLayout(new BoxLayout(rentalPanel, BoxLayout.Y_AXIS));
                rentalPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                
                // Add checkbox
                JCheckBox checkbox = new JCheckBox();
                checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
                checkboxes.add(checkbox);
                
                // Add rental details
                JLabel titleLabel2 = new JLabel("<html><b>" + rental[1] + "</b></html>");
                JLabel dueDateLabel = new JLabel("Due: " + rental[3]);
                JLabel lateFeeLabel = new JLabel(String.format("Late Fee: ₱%.2f", rental[4]));
                
                rentalPanel.add(checkbox);
                rentalPanel.add(Box.createVerticalStrut(5));
                rentalPanel.add(titleLabel2);
                rentalPanel.add(Box.createVerticalStrut(5));
                rentalPanel.add(dueDateLabel);
                rentalPanel.add(Box.createVerticalStrut(5));
                rentalPanel.add(lateFeeLabel);
                
                rentalsPanel.add(rentalPanel);
            }
            
            // Add scroll pane
            JScrollPane scrollPane = new JScrollPane(rentalsPanel);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            selectionPanel.add(scrollPane, BorderLayout.CENTER);
            
            // Add button panel at the bottom
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton returnButton = new JButton("Return Selected");
            returnButton.addActionListener(e -> {
                // Get selected rentals
                java.util.List<Object[]> selectedRentals = new java.util.ArrayList<>();
                double totalLateFee = 0.0;
                
                for (int i = 0; i < checkboxes.size(); i++) {
                    if (checkboxes.get(i).isSelected()) {
                        selectedRentals.add(activeRentals.get(i));
                        totalLateFee += (double)activeRentals.get(i)[4];
                    }
                }
                
                if (selectedRentals.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Please select at least one rental to return",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Show confirmation dialog
                StringBuilder message = new StringBuilder();
                message.append("Selected Rentals:\n\n");
                
                for (Object[] rental : selectedRentals) {
                    message.append(String.format("Title: %s\nDue: %s\nLate Fee: ₱%.2f\n\n",
                        rental[1], rental[3], rental[4]));
                }
                
                if (totalLateFee > 0) {
                    message.append(String.format("Total Late Fee: ₱%.2f\n\n", totalLateFee));
                    message.append("IMPORTANT: Please pay the total late fee in cash at the front desk before proceeding with the returns.\n\n");
                }
                
                message.append("Do you want to proceed with returning these books?");
                
                int choice = JOptionPane.showConfirmDialog(this,
                    message.toString(),
                    "Confirm Returns",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        // Process each selected return
                        for (Object[] rental : selectedRentals) {
                            processReturn(conn, (int)rental[0], (String)rental[1], 
                                (String)rental[2], (String)rental[3], (double)rental[4]);
                        }
                        
                        // Close the selection dialog
                        Window window = SwingUtilities.getWindowAncestor(selectionPanel);
                        if (window != null) {
                            window.dispose();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                            "Error returning books: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            
            buttonPanel.add(returnButton);
            selectionPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            // Show the custom dialog
            JOptionPane.showMessageDialog(this,
                selectionPanel,
                "Select Rentals to Return",
                JOptionPane.PLAIN_MESSAGE);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error returning book: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void processReturn(Connection conn, int rentalId, String bookTitle, String bookAuthor, 
            String dueDateStr, double lateFee) throws SQLException {
        // Recalculate late fee at the time of return to ensure it's current
        LocalDateTime dueDate;
        try {
            dueDate = LocalDateTime.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            dueDate = LocalDate.parse(dueDateStr).atTime(23, 59, 59);
        }
        
        LocalDateTime returnDate = LocalDateTime.now();
        if (returnDate.isAfter(dueDate)) {
            long minutesLate = java.time.Duration.between(dueDate, returnDate).toMinutes();
            double dailyFee = getLateFeeRate(); // Get current late fee rate
            lateFee = (minutesLate / 1440.0) * dailyFee;
        }
        
        // Update rental record with return date and late fee
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE rentals SET return_date = ?, late_fee = ? WHERE id = ?")) {
            pstmt.setString(1, returnDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setDouble(2, lateFee);
            pstmt.setInt(3, rentalId);
            pstmt.executeUpdate();
        }
        
        // Update book status and increase available copies
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE books SET copies = copies + 1, status = 'Available' WHERE book_id = ?")) {
            pstmt.setInt(1, Integer.parseInt(bookIdField.getText().trim()));
            pstmt.executeUpdate();
        }
        
        conn.commit();
        
        // Clear input field
        returnBookIdField.setText("");
        
        // Refresh rental history
        loadData();
        
        // Refresh User Dashboard panels
        refreshUserDashboard();
        
        String successMessage = lateFee > 0 ? 
            String.format("Book returned successfully!\nLate fee: ₱%.2f\n\nIMPORTANT: Please pay the late fee in cash at the front desk.", lateFee) :
            "Book returned successfully!";
        
        JOptionPane.showMessageDialog(this,
            successMessage,
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void searchBooks(String searchTerm) {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = 
                "SELECT b.*, " +
                "       (SELECT COUNT(*) FROM rentals r " +
                "        WHERE r.book_id = b.book_id " +
                "        AND r.user_id = ? " +
                "        AND r.return_date IS NULL) as total_rented " +
                "FROM books b " +
                "WHERE LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(b.isbn) LIKE ? OR LOWER(b.genre) LIKE ? " +
                "ORDER BY b.title";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                pstmt.setInt(1, currentUserId);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                pstmt.setString(4, searchPattern);
                pstmt.setString(5, searchPattern);
                
                ResultSet rs = pstmt.executeQuery();
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
                        String.format("₱%.2f", rs.getDouble("fee")),
                        rs.getInt("total_rented")
                    };
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error searching books: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshUserDashboard() {
        System.out.println("Refreshing User Dashboard...");
        // Find the UserDashboardPanel in the parent container
        Container parent = getParent();
        while (parent != null && !(parent instanceof JPanel)) {
            parent = parent.getParent();
        }
        
        if (parent != null) {
            System.out.println("Found parent container");
            // Find the UserDashboardPanel in the content panel
            for (Component comp : parent.getComponents()) {
                if (comp instanceof UserDashboardPanel) {
                    System.out.println("Found UserDashboardPanel, refreshing data...");
                    UserDashboardPanel dashboard = (UserDashboardPanel) comp;
                    dashboard.handleRentalUpdate();
                    System.out.println("UserDashboardPanel refreshed successfully");
                    break;
                }
            }
        } else {
            System.out.println("Could not find parent container");
        }
    }
    
    @Override
    protected String getTitle() {
        return "Book Rental & Return";
    }
    
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadData(); // Reload data for the new user
    }
    
    private JPanel createRentalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Rental Operations"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Create input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Book ID input
        JLabel bookIdLabel = new JLabel("Book ID:");
        bookIdField = new JTextField(10);
        JButton rentButton = new JButton("Rent Book");
        rentButton.addActionListener(e -> rentBook());
        
        // Add refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setToolTipText("Refresh the rental data");
        refreshButton.addActionListener(e -> loadData());
        
        inputPanel.add(bookIdLabel);
        inputPanel.add(bookIdField);
        inputPanel.add(rentButton);
        inputPanel.add(refreshButton);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createReturnPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 255, 255);  // White
                Color color2 = new Color(245, 245, 245);  // Light Gray
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Return section with gradient header
        JPanel returnSection = new JPanel();
        returnSection.setLayout(new BoxLayout(returnSection, BoxLayout.Y_AXIS));
        returnSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(231, 76, 60)),
                "Return a Book",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(231, 76, 60)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Return book input with icon
        JPanel returnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        returnPanel.setOpaque(false);
        JLabel returnIcon = new JLabel("↩️");
        returnIcon.setFont(new Font("Arial", Font.PLAIN, 16));
        returnPanel.add(returnIcon);
        returnPanel.add(new JLabel("Book ID:"));
        returnBookIdField = new JTextField(15);
        returnBookIdField.setPreferredSize(new Dimension(150, 30));
        returnPanel.add(returnBookIdField);
        returnSection.add(returnPanel);
        returnSection.add(Box.createVerticalStrut(15));
        
        // Return button with gradient
        JButton returnButton = new JButton("Return Book") {
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
        returnButton.setForeground(Color.BLACK);
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setFocusPainted(false);
        returnButton.setBorderPainted(false);
        returnButton.setContentAreaFilled(false);
        returnButton.setPreferredSize(new Dimension(150, 35));
        returnButton.setOpaque(true);
        returnButton.addActionListener(e -> returnBook(returnBookIdField.getText()));
        returnSection.add(returnButton);
        
        panel.add(returnSection);
        return panel;
    }
} 