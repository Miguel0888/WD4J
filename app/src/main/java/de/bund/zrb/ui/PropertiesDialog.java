package de.bund.zrb.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple dialog to edit Given | When | Then.
 */
public class PropertiesDialog extends JDialog {

    public PropertiesDialog(String title) {
        setTitle("Eigenschaften: " + title);
        setModal(true);
        setSize(400, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));

        panel.add(new JLabel("Given:"));
        JTextField givenField = new JTextField();
        panel.add(givenField);

        panel.add(new JLabel("When:"));
        JTextField whenField = new JTextField();
        panel.add(whenField);

        panel.add(new JLabel("Then:"));
        JTextField thenField = new JTextField();
        panel.add(thenField);

        JButton save = new JButton("Speichern");
        save.addActionListener(e -> {
            System.out.println("Given: " + givenField.getText());
            System.out.println("When: " + whenField.getText());
            System.out.println("Then: " + thenField.getText());
            dispose();
        });

        add(panel, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }
}
