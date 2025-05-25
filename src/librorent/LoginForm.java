package librorent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginForm extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;
    private JLabel errorLabel;
    private JButton loginButton;
    private JButton signUpLink;
    private boolean isMaximized = false;
    private Rectangle normalBounds;
    
    public LoginForm(Frame parent) {
        super(parent, "Login", true);
        setUndecorated(true);
        initializeComponents();
        createCustomTitleBar();
        pack();
        setLocationRelativeTo(parent);
        normalBounds = getBounds();
    }
    
    private void createCustomTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(255, 140, 0));
        titleBar.setPreferredSize(new Dimension(0, 30));
        
        // Title
        JLabel titleLabel = new JLabel("LibroRent - Login");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        titleBar.add(titleLabel, BorderLayout.WEST);
        
        // Window controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlPanel.setOpaque(false);
        
        // Minimize button
        JButton minimizeBtn = createTitleBarButton("─");
        minimizeBtn.addActionListener(e -> {
            setVisible(false);
            if (getOwner() instanceof Frame) {
                ((Frame)getOwner()).setState(Frame.ICONIFIED);
            }
            setVisible(true);
        });
        
        // Maximize/Restore button
        JButton maximizeBtn = createTitleBarButton("□");
        maximizeBtn.addActionListener(e -> toggleMaximize());
        
        // Close button
        JButton closeBtn = createTitleBarButton("×");
        closeBtn.addActionListener(e -> System.exit(0));
        
        controlPanel.add(minimizeBtn);
        controlPanel.add(maximizeBtn);
        controlPanel.add(closeBtn);
        titleBar.add(controlPanel, BorderLayout.EAST);
        
        // Add mouse listener for window dragging
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (!isMaximized) {
                    setLocation(getLocation().x + e.getX() - getWidth() / 2,
                              getLocation().y + e.getY());
                }
            }
        });
        
        add(titleBar, BorderLayout.NORTH);
    }
    
    private JButton createTitleBarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(51, 51, 51));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(40, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
                button.setBackground(new Color(70, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setBackground(new Color(51, 51, 51));
            }
        });
        
        return button;
    }
    
    private void toggleMaximize() {
        if (isMaximized) {
            setBounds(normalBounds);
            isMaximized = false;
        } else {
            normalBounds = getBounds();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            setBounds(bounds);
            isMaximized = true;
        }
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        // Create main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 140, 0); // Orange
                Color color2 = new Color(255, 69, 0);  // Red-Orange
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        
        // Create split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        
        // Left panel (Branding)
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 140, 0); // Orange
                Color color2 = new Color(255, 69, 0);  // Red-Orange
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Logo/Title
        JLabel logoLabel = new JLabel("LibroRent");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 36));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>Welcome back!<br>Rent your next book easily.</div></html>");
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(logoLabel);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(welcomeLabel);
        leftPanel.add(Box.createVerticalGlue());
        
        // Right panel (Form)
        JPanel rightPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 248, 240); // Warm white
                Color color2 = new Color(255, 228, 196); // Bisque
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Form title
        JLabel formTitle = new JLabel("Login");
        formTitle.setFont(new Font("Arial", Font.BOLD, 24));
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField = createTextField("Enter your username");
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocus();
                }
            }
        });
        
        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField = createPasswordField("Enter your password");
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });
        
        // Show password checkbox
        showPasswordCheck = new JCheckBox("Show Password");
        showPasswordCheck.setFont(new Font("Arial", Font.PLAIN, 12));
        showPasswordCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        showPasswordCheck.addActionListener(e -> togglePasswordVisibility());
        showPasswordCheck.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    showPasswordCheck.setSelected(!showPasswordCheck.isSelected());
                    togglePasswordVisibility();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown()) {
                        passwordField.requestFocus();
                    } else {
                        loginButton.requestFocus();
                    }
                }
            }
        });
        
        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Login button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(200, 35));
        loginButton.setBackground(new Color(255, 140, 0));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(true);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> handleLogin());
        loginButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown()) {
                        showPasswordCheck.requestFocus();
                    } else {
                        signUpLink.requestFocus();
                    }
                }
            }
        });
        
        // Sign up link
        signUpLink = new JButton("Don't have an account? Sign up here");
        signUpLink.setFont(new Font("Arial", Font.PLAIN, 12));
        signUpLink.setBorderPainted(false);
        signUpLink.setContentAreaFilled(false);
        signUpLink.setForeground(Color.BLUE);
        signUpLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signUpLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpLink.addActionListener(e -> {
            dispose();
            new SignUpForm((Frame) getParent()).setVisible(true);
        });
        signUpLink.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    signUpLink.doClick();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown()) {
                        loginButton.requestFocus();
                    } else {
                        usernameField.requestFocus();
                    }
                }
            }
        });
        
        // Add components to right panel
        rightPanel.add(formTitle);
        rightPanel.add(Box.createVerticalStrut(30));
        rightPanel.add(usernameLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(usernameField);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(passwordLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(passwordField);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(showPasswordCheck);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(errorLabel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(loginButton);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(signUpLink);
        
        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        
        // Make the form responsive
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth();
                if (width > 0) {
                    splitPane.setDividerLocation(width / 2);
                }
            }
        });

        // Set initial focus to username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocus());
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setMaximumSize(new Dimension(300, 30));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }
    
    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20);
        field.setMaximumSize(new Dimension(300, 30));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }
    
    private void togglePasswordVisibility() {
        boolean show = showPasswordCheck.isSelected();
        passwordField.setEchoChar(show ? '\0' : '•');
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password",
                "Login Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // First verify database connection
            Connection conn = DatabaseManager.getInstance().getConnection();
            if (conn == null) {
                System.err.println("Database connection is null");
                JOptionPane.showMessageDialog(this,
                    "Database connection error",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Verify users table exists and has required columns
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
                boolean hasRoleColumn = false;
                while (rs.next()) {
                    if ("role".equals(rs.getString("name"))) {
                        hasRoleColumn = true;
                        break;
                    }
                }
                if (!hasRoleColumn) {
                    System.err.println("Users table missing role column");
                    JOptionPane.showMessageDialog(this,
                        "Database schema error: missing role column",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Then attempt login
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT id, username, role FROM users WHERE username = ? AND password = ?")) {
                
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                
                System.out.println("Attempting login for username: " + username);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String role = rs.getString("role");
                    System.out.println("Login successful - User ID: " + userId + ", Role: " + role);
                    
                    // Initialize session
                    SessionManager.getInstance().login(userId);
                    
                    dispose(); // Close login form
                    
                    if (role != null && role.equalsIgnoreCase("Admin")) {
                        System.out.println("Opening Admin Dashboard for user: " + username);
                        SwingUtilities.invokeLater(() -> {
                            AdminDashboard adminDashboard = new AdminDashboard(username);
                            adminDashboard.setVisible(true);
                        });
                    } else {
                        System.out.println("Opening User Dashboard for user ID: " + userId);
                        SwingUtilities.invokeLater(() -> {
                            MainApplication userDashboard = new MainApplication(userId);
                            userDashboard.setVisible(true);
                        });
                    }
                } else {
                    System.out.println("Login failed - Invalid credentials for username: " + username);
                    JOptionPane.showMessageDialog(this,
                        "Invalid username or password",
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error during login: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 