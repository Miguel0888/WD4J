package app.ui;

import app.config.Settings;

import javax.swing.*;
import java.awt.*;

public class SettingsTab {
    private JPanel panel;
    private JTextField otpSecretField;
    private JCheckBox headlessCheckbox;
    private JTextField profilePathField;
    private final Settings settings;

    public SettingsTab() {
        settings = Settings.getInstance();
        panel = new JPanel(new GridLayout(4, 2, 10, 10));

        // UI Elemente
        JLabel otpLabel = new JLabel("OTP Secret:");
        otpSecretField = new JTextField(settings.getOtpSecret());
        JButton saveOtpButton = new JButton("Speichern");

        JLabel headlessLabel = new JLabel("Headless Mode:");
        headlessCheckbox = new JCheckBox("", settings.isHeadlessMode());

        JLabel profilePathLabel = new JLabel("Profile Path:");
        profilePathField = new JTextField(settings.getProfilePath());

        JButton saveButton = new JButton("Speichern");

        // Listener
        saveOtpButton.addActionListener(e -> {
            settings.setOtpSecret(otpSecretField.getText());
            JOptionPane.showMessageDialog(null, "OTP gespeichert!");
        });

        saveButton.addActionListener(e -> {
            settings.setHeadlessMode(headlessCheckbox.isSelected());
            settings.setProfilePath(profilePathField.getText());
            JOptionPane.showMessageDialog(null, "Einstellungen gespeichert!");
        });

        // Panel zusammenbauen
        panel.add(otpLabel);
        panel.add(otpSecretField);
        panel.add(saveOtpButton);

        panel.add(headlessLabel);
        panel.add(headlessCheckbox);

        panel.add(profilePathLabel);
        panel.add(profilePathField);
        panel.add(saveButton);
    }

    public JPanel getPanel() {
        return panel;
    }
}
