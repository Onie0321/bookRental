package librorent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.regex.Pattern;

public class SignUpForm extends JDialog {
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel errorLabel;
    private boolean isMaximized = false;
    private Rectangle normalBounds;
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public SignUpForm(Frame parent) {
        super(parent, "Sign Up", true);
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
        JLabel titleLabel = new JLabel("LibroRent - Sign Up");
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
                ((Frame)getOwner()).setExtendedState(Frame.ICONIFIED);
            }
            setVisible(true);
        });
        
        // Maximize/Restore button
        JButton maximizeBtn = createTitleBarButton("□");
        maximizeBtn.addActionListener(e -> toggleMaximize());
        
        // Close button
        JButton closeBtn = createTitleBarButton("×");
        closeBtn.addActionListener(e -> dispose());
        
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
        button.setBackground(new Color(255, 140, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(40, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
                button.setBackground(new Color(255, 165, 0));
            }
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setBackground(new Color(255, 140, 0));
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
        
        // Left panel (Form)
        JPanel leftPanel = new JPanel() {
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
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Form title
        JLabel formTitle = new JLabel("Create Account");
        formTitle.setFont(new Font("Arial", Font.BOLD, 24));
        formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Full Name field
        JLabel fullNameLabel = new JLabel("Full Name");
        fullNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fullNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fullNameField = createTextField("Enter your full name");
        
        // Email field
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailField = createTextField("Enter your email address");
        
        // Username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField = createTextField("Choose a username");
        
        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField = createPasswordField("Create a password");
        
        // Confirm Password field
        JLabel confirmPasswordLabel = new JLabel("Confirm Password");
        confirmPasswordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmPasswordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmPasswordField = createPasswordField("Confirm your password");
        
        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Sign Up button
        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setFont(new Font("Arial", Font.BOLD, 14));
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setMaximumSize(new Dimension(200, 35));
        signUpButton.setBackground(new Color(255, 140, 0));
        signUpButton.setForeground(Color.WHITE);
        signUpButton.setFocusPainted(false);
        signUpButton.setBorderPainted(false);
        signUpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signUpButton.addActionListener(e -> handleSignUp());
        
        // Add hover effect to sign up button
        signUpButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                signUpButton.setBackground(new Color(255, 165, 0));
            }
            public void mouseExited(MouseEvent e) {
                signUpButton.setBackground(new Color(255, 140, 0));
            }
        });
        
        // Login link
        JButton loginLink = new JButton("Already have an account? Log in here");
        loginLink.setFont(new Font("Arial", Font.PLAIN, 12));
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(Color.BLUE);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginLink.addActionListener(e -> {
            dispose();
            new LoginForm((Frame) getParent()).setVisible(true);
        });
        
        // Add components to left panel
        leftPanel.add(formTitle);
        leftPanel.add(Box.createVerticalStrut(30));
        leftPanel.add(fullNameLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(fullNameField);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(emailLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(emailField);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(usernameLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(usernameField);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(passwordLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(passwordField);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(confirmPasswordLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(confirmPasswordField);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(errorLabel);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(signUpButton);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(loginLink);
        
        // Right panel (Branding)
        JPanel rightPanel = new JPanel() {
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
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Logo/Title
        JLabel logoLabel = new JLabel("LibroRent");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 36));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>Create your account and start<br>renting books anytime, anywhere.</div></html>");
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(logoLabel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(welcomeLabel);
        rightPanel.add(Box.createVerticalGlue());
        
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
    
    private void handleSignUp() {
        // Get field values
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = "Member"; // Always set role to Member
        
        // Validate required fields
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("All fields are required");
            return;
        }
        
        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Please enter a valid email address");
            return;
        }
        
        // Validate password match
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            confirmPasswordField.setText("");
            confirmPasswordField.requestFocus();
            return;
        }
        
        // Validate password strength (minimum 8 characters)
        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters long");
            return;
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Check if username already exists
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE username = ?")) {
                pstmt.setString(1, username);
                if (pstmt.executeQuery().next()) {
                    errorLabel.setText("Username already exists");
                    return;
                }
            }
            
            // Check if email already exists
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT email FROM users WHERE email = ?")) {
                pstmt.setString(1, email);
                if (pstmt.executeQuery().next()) {
                    errorLabel.setText("Email already registered");
                    return;
                }
            }
            
            // Insert new user
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO users (full_name, email, username, password, role) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, email);
                pstmt.setString(3, username);
                pstmt.setString(4, password); // In a real application, this should be hashed
                pstmt.setString(5, role);
                pstmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this,
                    "Account created successfully! Please log in.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
                dispose();
                new LoginForm((Frame) getParent()).setVisible(true);
            }
            
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }
} 