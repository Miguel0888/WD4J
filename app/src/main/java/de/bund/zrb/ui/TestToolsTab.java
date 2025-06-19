package de.bund.zrb.ui;

import de.bund.zrb.controller.MainController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

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
        return "Tools";
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

        JLabel separator = new JLabel(" | ");
        JLabel selectorLabel = new JLabel("Selector: ");

        // 🔽 Dropdown mit Textfeld-Funktionalität
        JComboBox<String> selectorTestField = new JComboBox<>();
        selectorTestField.setEditable(true); // Erlaubt direkte Eingabe
        selectorTestField.setMaximumSize(new Dimension(350, 24));
        selectorTestField.setPreferredSize(new Dimension(350, 24));
        selectorTestField.setToolTipText("Enter a XPATH selector to test");

        // 🛠 Historie der letzten Eingaben
        List<String> selectorHistory = new ArrayList<>();

        JComboBox<String> selectorTestVariant = new JComboBox<>(new String[]{"Change Text"});
        selectorTestVariant.setMaximumSize(new Dimension(100, 24));
        selectorTestVariant.setPreferredSize(new Dimension(100, 24));
        JToggleButton selectorToggleButton = new JToggleButton("Test");
        selectorToggleButton.addActionListener(e -> {
            boolean isSelected = selectorToggleButton.isSelected();
            selectorTestField.setEnabled(!isSelected);
            selectorTestVariant.setEnabled(!isSelected);

            String selector = (String) selectorTestField.getEditor().getItem();
            if (!isSelected || (selector != null && !selector.isEmpty())) {
                String variant = (String) selectorTestVariant.getSelectedItem();
                if (!isSelected || (variant != null && !variant.isEmpty())) {
                    controller.testSelector(selector, variant, isSelected);

                    // ✅ Eingabe zur Historie hinzufügen
                    if (!selectorHistory.contains(selector)) {
                        selectorHistory.add(0, selector); // Neueste oben
                        if (selectorHistory.size() > 10) selectorHistory.remove(10); // Begrenzung
                        selectorTestField.removeAllItems();
                        selectorHistory.forEach(selectorTestField::addItem);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a valid test variant!", "Error", JOptionPane.ERROR_MESSAGE);
                    selectorToggleButton.setSelected(false); // Toggle zurücksetzen
                }
            } else {
                JOptionPane.showMessageDialog(null, "Please enter a valid XPATH selector!", "Error", JOptionPane.ERROR_MESSAGE);
                selectorToggleButton.setSelected(false); // Toggle zurücksetzen
            }
        });

        toolbar.add(new JLabel("Tools: "));
        toolbar.add(screenshotButton);

        // Rest rechts ausrichten
        toolbar.add(Box.createHorizontalGlue());
//        toolbar.add(separator);
        toolbar.add(selectorLabel);
        toolbar.add(selectorTestField);
        toolbar.add(selectorTestVariant);
        toolbar.add(selectorToggleButton);

        return toolbar;
    }
}
