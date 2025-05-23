package librorent;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.regex.Pattern;

public class RegisterForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JLabel memberIdLabel;
    private LoginForm loginForm;

    public RegisterForm(LoginForm loginForm) {
        this.loginForm = loginForm;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Register - LibroRent");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Member ID (Auto-generated)
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1;
        memberIdLabel = new JLabel("Will be generated after registration");
        memberIdLabel.setForeground(Color.GRAY);
        formPanel.add(memberIdLabel, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        formPanel.add(confirmPasswordField, gbc);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        nameField.setEditable(false);
        formPanel.add(nameField, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        emailField.setEditable(false);
        formPanel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(20);
        formPanel.add(phoneField, gbc);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");

        registerButton.addActionListener(e -> handleRegistration());
        backButton.addActionListener(e -> {
            dispose();
            if (loginForm != null) {
                loginForm.setVisible(true);
            }
        });

        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);
        mainPanel.add(buttonPanel);

        // Add auto-fetch functionality for name and email
        usernameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { fetchUserInfo(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { fetchUserInfo(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { fetchUserInfo(); }
        });

        add(mainPanel);
    }

    private void fetchUserInfo() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            nameField.setText("");
            emailField.setText("");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT full_name, email FROM users WHERE username = ?")) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // If user exists, populate the fields
                nameField.setText(rs.getString("full_name"));
                emailField.setText(rs.getString("email"));
                
                JOptionPane.showMessageDialog(this,
                    "User already exists. Please use a different username.",
                    "Username Taken",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                // If user doesn't exist, clear the fields
                nameField.setText("");
                emailField.setText("");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all fields",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                "Passwords do not match",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid email address",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid phone number",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (username, password, full_name, email, phone) " +
                 "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real application, use password hashing
            pstmt.setString(3, name);
            pstmt.setString(4, email);
            pstmt.setString(5, phone);
            
            pstmt.executeUpdate();
            
            // Get the generated member ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int memberId = generatedKeys.getInt(1);
                    memberIdLabel.setText(String.format("LIB-%06d", memberId));
                    memberIdLabel.setForeground(Color.BLACK);
                }
            }
            
            JOptionPane.showMessageDialog(this,
                "Registration successful! Your Member ID is " + memberIdLabel.getText() + "\nPlease login.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
            if (loginForm != null) {
                loginForm.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error during registration: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidPhone(String phone) {
        // Basic phone validation - allows digits, spaces, dashes, and parentheses
        String phoneRegex = "^[0-9\\s\\-()]+$";
        return Pattern.matches(phoneRegex, phone);
    }
} 