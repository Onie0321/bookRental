package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class SearchFilterPanel extends BasePanel {
    private JTable searchResultsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    
    public SearchFilterPanel() {
        // Initialize components
        initializeComponents();
        
        // Add search panel
        JPanel searchPanel = createSearchPanel();
        contentArea.add(searchPanel, BorderLayout.NORTH);
        
        // Add results table
        JScrollPane scrollPane = new JScrollPane(searchResultsTable);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        
        // Add status bar
        JPanel statusBar = createStatusBar();
        contentArea.add(statusBar, BorderLayout.SOUTH);

        // Load initial data
        loadData();
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Title", "Author", "Genre", "Format", "Status", "Available Copies"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        searchResultsTable = new JTable(tableModel);
        searchResultsTable.setFillsViewportHeight(true);
        searchResultsTable.setRowHeight(30);
        
        // Set column widths
        searchResultsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        searchResultsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        searchResultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        searchResultsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        searchResultsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        searchResultsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Search type
        panel.add(new JLabel("Search by:"));
        String[] searchTypes = {"Title", "Author", "Genre"};
        searchTypeCombo = new JComboBox<>(searchTypes);
        panel.add(searchTypeCombo);
        
        // Search field
        panel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        panel.add(searchField);
        
        // Search button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        panel.add(searchButton);
        
        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearSearch());
        panel.add(clearButton);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(240, 240, 240));
        
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        return statusBar;
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        String searchType = (String) searchTypeCombo.getSelectedItem();
        
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a search term",
                "Search Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Clear existing results
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM books WHERE LOWER(" + searchType.toLowerCase() + ") LIKE ? ORDER BY book_id DESC")) {
            
            pstmt.setString(1, "%" + searchTerm.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Object[] row = {
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getInt("copies") // Use actual copies from database
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error searching books: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
        
        // Update status
        JLabel statusLabel = (JLabel) ((JPanel) contentArea.getComponent(2)).getComponent(0);
        statusLabel.setText("Found " + tableModel.getRowCount() + " results");
    }
    
    private void clearSearch() {
        searchField.setText("");
        loadData(); // Load all books when clearing search
        JLabel statusLabel = (JLabel) ((JPanel) contentArea.getComponent(2)).getComponent(0);
        statusLabel.setText("Ready");
    }
    
    private void loadData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY book_id DESC")) {
            
            while (rs.next()) {
                Object[] row = {
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getInt("copies")
                };
                tableModel.addRow(row);
            }
            
            // Update status
            JLabel statusLabel = (JLabel) ((JPanel) contentArea.getComponent(2)).getComponent(0);
            statusLabel.setText("Found " + tableModel.getRowCount() + " results");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    protected String getTitle() {
        return "Search & Filter";
    }
} 