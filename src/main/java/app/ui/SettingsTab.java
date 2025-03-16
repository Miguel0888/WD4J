package app.ui;

import app.config.SettingsData;
import app.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class SettingsTab {
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

    public JToolBar getSettingsToolbar() {
        JToolBar settingsToolbar = new JToolBar();
        settingsToolbar.setFloatable(false);
        settingsToolbar.add(saveSettingsButton);
        settingsToolbar.add(loadSettingsButton);
        return settingsToolbar;
    }

    public JToolBar getToolbar() {
        return getSettingsToolbar();
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
