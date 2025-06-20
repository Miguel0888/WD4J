package de.bund.zrb.ui;

import de.bund.zrb.config.SettingsData;
import de.bund.zrb.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class SettingsTab implements UIComponent {
    private JPanel panel;
    private JButton saveSettingsButton, loadSettingsButton;
    private final SettingsData settingsData;

    public SettingsTab(MainController controller) {
        panel = new JPanel(new BorderLayout());
        settingsData = SettingsData.getInstance();

        // Buttons für das Speichern und Laden
        saveSettingsButton = new JButton("Save Settings");
        loadSettingsButton = new JButton("Load Settings");

        saveSettingsButton.addActionListener(e -> saveSettings());
        loadSettingsButton.addActionListener(e -> loadSettings());

        // Layout für den Settings-Tab
        JPanel settingsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        settingsPanel.add(saveSettingsButton);
        settingsPanel.add(loadSettingsButton);

        panel.add(settingsPanel, BorderLayout.NORTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getComponentTitle() {
        return "Settings";
    }

    @Override
    public JMenuItem getMenuItem() {
        // Erstelle ein Untermenü für die Settings-Optionen
        JMenu settingsMenu = new JMenu("Settings");

        JMenuItem loadItem = new JMenuItem("Load Settings");
        loadItem.addActionListener(e -> loadSettings());

        JMenuItem saveItem = new JMenuItem("Save Settings");
        saveItem.addActionListener(e -> saveSettings());

        settingsMenu.add(loadItem);
        settingsMenu.add(saveItem);

        return settingsMenu;  // Das Menü wird dem Hauptmenü hinzugefügt
    }

    private void saveSettings() {
        settingsData.saveSettings();
        JOptionPane.showMessageDialog(null, "Settings saved successfully!");
    }

    private void loadSettings() {
        settingsData.loadSettings();
        JOptionPane.showMessageDialog(null, "Settings loaded successfully!");
    }
}
