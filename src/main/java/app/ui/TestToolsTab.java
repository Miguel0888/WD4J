package app.ui;

import app.controller.MainController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class TestToolsTab implements UIComponent {
    private final MainController controller;
    private JPanel panel;
    private JLabel imageContainer;
    private JButton screenshotButton;
    private JToolBar toolbar;

    public TestToolsTab(MainController controller) {
        this.controller = controller;
        toolbar = createToolbar();
        panel = new JPanel(new BorderLayout());

        imageContainer = new JLabel();
        imageContainer.setHorizontalAlignment(SwingConstants.CENTER);
        imageContainer.setVerticalAlignment(SwingConstants.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageContainer);
        imageScrollPane.setPreferredSize(new Dimension(1024, 400));

        panel.add(imageScrollPane, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getComponentTitle() {
        return "Test Tools";
    }

    private void captureScreenshot(MainController controller) {
        try {
            byte[] imageData = controller.captureScreenshot();
            if (imageData == null || imageData.length == 0) {
                JOptionPane.showMessageDialog(null, "Screenshot failed!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                imageContainer.setIcon(new ImageIcon(image));
                imageContainer.revalidate();
                imageContainer.repaint();
            } else {
                JOptionPane.showMessageDialog(null, "Screenshot data invalid!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error taking screenshot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Falls der Screenshot-Button in einer Toolbar verwendet werden soll.
     */
    public JButton getScreenshotButton() {
        return screenshotButton;
    }

    /**
     * Falls ein separater Screenshot-Toolbar benötigt wird (optional).
     */
    public JToolBar getToolbar() {
        return toolbar;
    }

    private JToolBar createToolbar() {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        screenshotButton = new JButton("\uD83D\uDCF8"); // Kamera-Symbol
        screenshotButton.setToolTipText("Take Screenshot");
        screenshotButton.addActionListener(e -> captureScreenshot(controller));

        toolbar.add(new JLabel("Test Tools: "));
        toolbar.add(screenshotButton);
        return toolbar;
    }
}
