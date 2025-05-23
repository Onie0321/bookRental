package librorent;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class FormatTrackingPanel extends BasePanel {
    private JTable formatTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> formatFilter;
    
    public FormatTrackingPanel() {
        // Initialize components
        initializeComponents();
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Add filter panel
        JPanel filterPanel = createFilterPanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);
        
        // Add format table
        JScrollPane scrollPane = new JScrollPane(formatTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        contentArea.add(mainPanel, BorderLayout.CENTER);
        
        // Load data from database
        loadData();
    }
    
    private void initializeComponents() {
        // Create table model
        String[] columns = {"Book ID", "Title", "Format", "Status",};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create table
        formatTable = new JTable(tableModel);
        formatTable.setFillsViewportHeight(true);
        formatTable.setRowHeight(30);
        
        // Set column widths
        formatTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        formatTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        formatTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        formatTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        formatTable.getColumnModel().getColumn(4).setPreferredWidth(150);
    }
    
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Format filter
        panel.add(new JLabel("Format:"));
        String[] formats = {"All", "Physical", "E-Book"};
        formatFilter = new JComboBox<>(formats);
        panel.add(formatFilter);
        
        // Add filter button
        JButton filterButton = new JButton("Apply Filter");
        filterButton.addActionListener(e -> applyFilter());
        panel.add(filterButton);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());
        panel.add(refreshButton);
        
        return panel;
    }
    
    private void loadData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books ORDER BY book_id DESC")) {
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getString("last_updated") != null ? rs.getString("last_updated") : "N/A"
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading books: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void applyFilter() {
        String selectedFormat = (String) formatFilter.getSelectedItem();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM books WHERE ? = 'All' OR format = ? ORDER BY book_id DESC")) {
            
            pstmt.setString(1, selectedFormat);
            pstmt.setString(2, selectedFormat);
            
            ResultSet rs = pstmt.executeQuery();
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("format"),
                    rs.getString("status"),
                    rs.getString("last_updated")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error applying filter: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    protected String getTitle() {
        return "Book Format Tracking";
    }
} 