package librorent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class MainApplication extends JFrame {
    private JPanel sidebar;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DatabaseManager dbManager;
    private int currentUserId;
    
    public MainApplication(int userId) {
        this.currentUserId = userId;
        // Check for required JVM argument
        checkNativeAccess();
        
        // Initialize database
        dbManager = DatabaseManager.getInstance();
        
        setTitle("LibroRent - Book Rental System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Create main container with BorderLayout
        setLayout(new BorderLayout());
        
        // Create sidebar
        createSidebar();
        
        // Create content panel with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add content panels
        UserDashboardPanel dashboardPanel = new UserDashboardPanel();
        BookListingPanel bookListingPanel = new BookListingPanel();
        RentalReturnPanel rentalReturnPanel = new RentalReturnPanel();
        ReservationPanel reservationPanel = new ReservationPanel();
        LateFeePanel lateFeePanel = new LateFeePanel();
        
        // Set current user ID in all panels
        dashboardPanel.setCurrentUserId(userId);
        bookListingPanel.setCurrentUserId(userId);
        rentalReturnPanel.setCurrentUserId(userId);
        reservationPanel.setCurrentUserId(userId);
        
        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(bookListingPanel, "BOOK_LISTING");
        contentPanel.add(rentalReturnPanel, "RENTAL_RETURN");
        contentPanel.add(reservationPanel, "RESERVATION");
        contentPanel.add(lateFeePanel, "LATE_FEE");
        
        // Add components to frame
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        
        // Add window listener to close database connection
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Clean up any resources
                try {
                    // Close any open connections in the connection pool
                    if (dbManager != null) {
                        // No need to explicitly close connection as we're using try-with-resources
                        // and connection-per-request model
                        System.out.println("Application closing - cleaning up resources");
                    }
                } catch (Exception ex) {
                    System.err.println("Error during cleanup: " + ex.getMessage());
                }
            }
        });
    }
    
    private void createSidebar() {
        sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(new Color(51, 51, 51));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        
        // Add logo/header
        JLabel headerLabel = new JLabel("LibroRent");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        sidebar.add(headerLabel);
        
        // Add menu items
        addMenuItem("👤 My Dashboard", "DASHBOARD");
        addMenuItem("📚 Book Listing", "BOOK_LISTING");
        
        addMenuItem("📖 Rental & Return", "RENTAL_RETURN");
        addMenuItem("⏰ Reservations", "RESERVATION");
        addMenuItem("💰 Late Fees", "LATE_FEE");
       
        
        // Add some spacing
        sidebar.add(Box.createVerticalGlue());
        
        // Add divider
        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setForeground(new Color(100, 100, 100));
        divider.setMaximumSize(new Dimension(230, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));
        
        // Add logout button
        JButton logoutButton = new JButton("🚪 Logout");
        logoutButton.setFont(new Font("Arial", Font.PLAIN, 14));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(51, 51, 51));
        logoutButton.setBorderPainted(false);
        logoutButton.setFocusPainted(false);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(230, 40));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        logoutButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                logoutButton.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                logoutButton.setBackground(new Color(51, 51, 51));
            }
        });
        
        // Add logout action
        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginForm(null).setVisible(true);
            }
        });
        
        sidebar.add(logoutButton);
        sidebar.add(Box.createVerticalStrut(10));
    }
    
    private void addMenuItem(String text, String cardName) {
        JButton menuItem = new JButton(text);
        menuItem.setFont(new Font("Arial", Font.PLAIN, 14));
        menuItem.setForeground(Color.WHITE);
        menuItem.setBackground(new Color(51, 51, 51));
        menuItem.setBorderPainted(false);
        menuItem.setFocusPainted(false);
        menuItem.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuItem.setMaximumSize(new Dimension(230, 40));
        menuItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        menuItem.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                menuItem.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                menuItem.setBackground(new Color(51, 51, 51));
            }
        });
        
        // Add click action
        menuItem.addActionListener(e -> {
            cardLayout.show(contentPanel, cardName);
            // Update selected state
            for (Component comp : sidebar.getComponents()) {
                if (comp instanceof JButton) {
                    comp.setBackground(new Color(51, 51, 51));
                }
            }
            menuItem.setBackground(new Color(70, 70, 70));
        });
        
        sidebar.add(menuItem);
        sidebar.add(Box.createVerticalStrut(5));
    }
    
    private void checkNativeAccess() {
        if (!DatabaseManager.isNativeLibraryLoaded()) {
            JOptionPane.showMessageDialog(this,
                "Warning: SQLite JDBC driver not found.\n" +
                "Please ensure sqlite-jdbc is in your classpath.\n\n" +
                "The application may not function correctly without this driver.",
                "SQLite Driver Warning",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Show login form first
            LoginForm loginForm = new LoginForm(null);
            loginForm.setVisible(true);
        });
    }
} 