package librorent;

import javax.swing.*;
import java.awt.*;

public abstract class BasePanel extends JPanel {
    protected JPanel contentArea;
    
    public BasePanel() {
        setLayout(new BorderLayout());
        
        // Create header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        // Create content area
        contentArea = new JPanel();
        contentArea.setLayout(new BorderLayout());
        contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(contentArea, BorderLayout.CENTER);
    }
    
    protected JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel titleLabel = new JLabel(getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(titleLabel, BorderLayout.WEST);
        
        return header;
    }
    
    protected abstract String getTitle();
} 