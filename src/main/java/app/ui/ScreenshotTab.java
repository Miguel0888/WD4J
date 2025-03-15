package app.ui;

import javax.swing.*;
import java.awt.*;

public class ScreenshotTab {
    private JPanel panel;
    private JLabel imageContainer;

    public ScreenshotTab() {
        imageContainer = new JLabel();
        imageContainer.setHorizontalAlignment(SwingConstants.CENTER);
        imageContainer.setVerticalAlignment(SwingConstants.CENTER);

        JScrollPane imageScrollPane = new JScrollPane(imageContainer);
        imageScrollPane.setPreferredSize(new Dimension(1024, 400));

        panel = new JPanel(new BorderLayout());
        panel.add(imageScrollPane, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JLabel getImageContainer() {
        return imageContainer;
    }
}

