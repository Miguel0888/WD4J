package de.bund.zrb.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Editor Panel: UI zum Bearbeiten von Testfällen.
 */
public class EditorPanel extends JPanel {

    private final JTextField nameField = new JTextField();
    private final JList<String> givenList = new JList<>();
    private final JList<String> whenList = new JList<>();
    private final JList<String> thenList = new JList<>();
    private final JComboBox<String> selectorBox = new JComboBox<>();
    private final JButton saveButton = new JButton("Speichern");

    public EditorPanel() {
        super(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Name
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Testfall-Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(nameField, gbc);
        row++;

        // Vorbedingungen
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(new JLabel("Vorbedingungen (Given):"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(givenList), gbc);
        row++;

        // Aktionen
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Aktionen (When):"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(whenList), gbc);
        row++;

        // Nachbedingungen
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Nachbedingungen (Then):"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(thenList), gbc);
        row++;

        // Selektor-Auswahl
        gbc.gridx = 0; gbc.gridy = row;
        formPanel.add(new JLabel("Selektor-Auswahl:"), gbc);
        gbc.gridx = 1;
        formPanel.add(selectorBox, gbc);
        row++;

        // Speichern-Button
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(saveButton, gbc);

        add(formPanel, BorderLayout.NORTH);
    }
}
