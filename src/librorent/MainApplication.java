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
    private boolean isExpanded = false;
    private static final int COLLAPSED_WIDTH = 60;
    private static final int EXPANDED_WIDTH = 250;
    
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
        sidebar.setPreferredSize(new Dimension(COLLAPSED_WIDTH, 0));
        sidebar.setBackground(new Color(51, 51, 51));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        
        // Add hamburger menu button
        JButton hamburgerButton = new JButton("☰");
        hamburgerButton.setFont(new Font("Arial Unicode MS", Font.PLAIN, 24));
        hamburgerButton.setForeground(Color.WHITE);
        hamburgerButton.setBackground(new Color(51, 51, 51));
        hamburgerButton.setBorderPainted(false);
        hamburgerButton.setFocusPainted(false);
        hamburgerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        hamburgerButton.setMaximumSize(new Dimension(COLLAPSED_WIDTH, 40));
        hamburgerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect for hamburger
        hamburgerButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hamburgerButton.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                hamburgerButton.setBackground(new Color(51, 51, 51));
            }
        });
        
        // Add toggle action
        hamburgerButton.addActionListener(e -> toggleSidebar());
        
        sidebar.add(hamburgerButton);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Add menu items with proper icons
        addMenuItem("📊", "My Dashboard", "DASHBOARD");
        addMenuItem("📚", "Book Listing", "BOOK_LISTING");
        addMenuItem("📖", "Rental & Return", "RENTAL_RETURN");
        addMenuItem("⏰", "Reservations", "RESERVATION");
        addMenuItem("💰", "Late Fees", "LATE_FEE");
        
        // Add some spacing
        sidebar.add(Box.createVerticalGlue());
        
        // Add divider
        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setForeground(new Color(100, 100, 100));
        divider.setMaximumSize(new Dimension(COLLAPSED_WIDTH - 10, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));
        
        // Add logout button
        JButton logoutButton = new JButton("🚪");
        logoutButton.setFont(new Font("Arial Unicode MS", Font.PLAIN, 15));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(51, 51, 51));
        logoutButton.setBorderPainted(false);
        logoutButton.setFocusPainted(false);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(COLLAPSED_WIDTH - 10, 40));
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
    
    private void toggleSidebar() {
        isExpanded = !isExpanded;
        int newWidth = isExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        
        // Update sidebar width
        sidebar.setPreferredSize(new Dimension(newWidth, 0));
        
        // Update all menu items
        for (Component comp : sidebar.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                // Skip hamburger button and handle it separately
                if (button == sidebar.getComponent(0)) {
                    button.setMaximumSize(new Dimension(newWidth, 40));
                    continue;
                }
                
                // Handle logout button separately
                if (button == sidebar.getComponent(sidebar.getComponentCount() - 2)) {
                    button.setMaximumSize(new Dimension(newWidth - 10, 40));
                    continue;
                }
                
                // Handle regular menu items
                Object fullText = button.getClientProperty("fullText");
                if (fullText != null) {
                    if (isExpanded) {
                        // Show full text
                        String icon = button.getText();
                        button.setText(icon + " " + fullText.toString());
                        button.setMaximumSize(new Dimension(EXPANDED_WIDTH - 10, 40));
                        button.setAlignmentX(Component.LEFT_ALIGNMENT);
                    } else {
                        // Show only icon
                        String icon = button.getText().split(" ")[0];
                        button.setText(icon);
                        button.setMaximumSize(new Dimension(COLLAPSED_WIDTH - 10, 40));
                        button.setAlignmentX(Component.CENTER_ALIGNMENT);
                    }
                }
            }
        }
        
        // Update divider width
        for (Component comp : sidebar.getComponents()) {
            if (comp instanceof JSeparator) {
                JSeparator divider = (JSeparator) comp;
                divider.setMaximumSize(new Dimension(newWidth - 10, 1));
            }
        }
        
        // Repaint and revalidate
        sidebar.revalidate();
        sidebar.repaint();
    }
    
    private void addMenuItem(String icon, String text, String cardName) {
        JButton menuItem = new JButton(icon);
        menuItem.putClientProperty("fullText", text);
        menuItem.setFont(new Font("Arial Unicode MS", Font.PLAIN, 20));
        menuItem.setForeground(Color.WHITE);
        menuItem.setBackground(new Color(51, 51, 51));
        menuItem.setBorderPainted(false);
        menuItem.setFocusPainted(false);
        menuItem.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuItem.setMaximumSize(new Dimension(COLLAPSED_WIDTH - 10, 40));
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