package app.ui;

import app.Main;
import app.config.SettingsData;

import javax.swing.*;
import java.awt.*;

public class SettingsTab {
    private JPanel panel;
    private JComboBox<String> browserSelector;
    private JTextField portField;
    private JCheckBox useProfileCheckbox;
    private JTextField profilePathField;
    private JCheckBox headlessCheckbox, disableGpuCheckbox, noRemoteCheckbox, startMaximizedCheckbox;
    private JButton launchButton, terminateButton, navigateButton;
    private JTextField addressBar;
    private JComboBox<Object> userContextDropdown, browsingContextDropdown;
    private JButton newContextButton, closeContextButton;
    private JButton saveSettingsButton, loadSettingsButton;

    private final SettingsData settingsData;

    public SettingsTab() {
        panel = new JPanel(new BorderLayout());

        settingsData = SettingsData.getInstance();

        // UI-Komponenten
        browserSelector = new JComboBox<>(new String[]{"Firefox", "Chrome", "Edge", "Safari"});
        portField = new JTextField(settingsData.getPort(), 5);
        useProfileCheckbox = new JCheckBox("Use Profile", settingsData.isUseProfile());
        profilePathField = new JTextField(settingsData.getProfilePath(), 15);
        headlessCheckbox = new JCheckBox("Headless", settingsData.isHeadlessMode());
        disableGpuCheckbox = new JCheckBox("Disable GPU", settingsData.isDisableGpu());
        noRemoteCheckbox = new JCheckBox("No Remote", settingsData.isNoRemote());
        startMaximizedCheckbox = new JCheckBox("Maximized", settingsData.isStartMaximized());
        launchButton = new JButton("Launch Browser");
        terminateButton = new JButton("Terminate Browser");
        navigateButton = new JButton("Navigate");
        addressBar = new JTextField("https://www.google.com", 30);
        userContextDropdown = new JComboBox<>();
        browsingContextDropdown = new JComboBox<>();
        newContextButton = new JButton("+");
        closeContextButton = new JButton("-");

        // Buttons für das Speichern und Laden
        saveSettingsButton = new JButton("Save Settings");
        loadSettingsButton = new JButton("Load Settings");

        saveSettingsButton.addActionListener(e -> saveSettings());
        loadSettingsButton.addActionListener(e -> loadSettings());

        // Event Listener
        launchButton.addActionListener(Main.controller::onLaunch);
        terminateButton.addActionListener(Main.controller::onTerminate);
        navigateButton.addActionListener(Main.controller::onNavigate);

        // Layout für den Settings-Tab
        JPanel settingsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        settingsPanel.add(new JLabel("Browser:"));
        settingsPanel.add(browserSelector);
        settingsPanel.add(new JLabel("Port:"));
        settingsPanel.add(portField);
        settingsPanel.add(new JLabel("Profile Path:"));
        settingsPanel.add(profilePathField);
        settingsPanel.add(saveSettingsButton);
        settingsPanel.add(loadSettingsButton);

        panel.add(settingsPanel, BorderLayout.NORTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    public JToolBar getToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel("Browser:"));
        toolbar.add(browserSelector);
        toolbar.add(new JLabel("Port:"));
        toolbar.add(portField);
        toolbar.add(useProfileCheckbox);
        toolbar.add(new JLabel("Profile Path:"));
        toolbar.add(profilePathField);
        toolbar.add(headlessCheckbox);
        toolbar.add(disableGpuCheckbox);
        toolbar.add(noRemoteCheckbox);
        toolbar.add(startMaximizedCheckbox);
        toolbar.add(launchButton);
        toolbar.add(terminateButton);
        return toolbar;
    }

    public JToolBar getNavigationToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(new JLabel("URL:"));
        toolbar.add(addressBar);
        toolbar.add(navigateButton);
        return toolbar;
    }

    public JToolBar getContextToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.add(new JLabel("User Context:"));
        toolbar.add(userContextDropdown);
        toolbar.add(new JLabel("Browsing Context:"));
        toolbar.add(browsingContextDropdown);
        toolbar.add(newContextButton);
        toolbar.add(closeContextButton);
        return toolbar;
    }

    private void saveSettings() {
        settingsData.setPort(portField.getText());
        settingsData.setUseProfile(useProfileCheckbox.isSelected());
        settingsData.setProfilePath(profilePathField.getText());
        settingsData.setHeadlessMode(headlessCheckbox.isSelected());
        settingsData.setDisableGpu(disableGpuCheckbox.isSelected());
        settingsData.setNoRemote(noRemoteCheckbox.isSelected());
        settingsData.setStartMaximized(startMaximizedCheckbox.isSelected());

        JOptionPane.showMessageDialog(null, "Settings saved successfully!");
    }

    private void loadSettings() {
        portField.setText(settingsData.getPort());
        useProfileCheckbox.setSelected(settingsData.isUseProfile());
        profilePathField.setText(settingsData.getProfilePath());
        headlessCheckbox.setSelected(settingsData.isHeadlessMode());
        disableGpuCheckbox.setSelected(settingsData.isDisableGpu());
        noRemoteCheckbox.setSelected(settingsData.isNoRemote());
        startMaximizedCheckbox.setSelected(settingsData.isStartMaximized());

        JOptionPane.showMessageDialog(null, "Settings loaded successfully!");
    }

    // Getter für Buttons und Felder
    public JButton getLaunchButton() { return launchButton; }
    public JButton getTerminateButton() { return terminateButton; }
    public JComboBox<String> getBrowserSelector() { return browserSelector; }
    public JTextField getPortField() { return portField; }
    public JCheckBox getUseProfileCheckbox() { return useProfileCheckbox; }
    public JTextField getProfilePathField() { return profilePathField; }
    public JCheckBox getHeadlessCheckbox() { return headlessCheckbox; }
    public JCheckBox getDisableGpuCheckbox() { return disableGpuCheckbox; }
    public JCheckBox getNoRemoteCheckbox() { return noRemoteCheckbox; }
    public JCheckBox getStartMaximizedCheckbox() { return startMaximizedCheckbox; }
    public JTextField getAddressBar() { return addressBar; }
    public JButton getNavigateButton() { return navigateButton; }
    public JComboBox<Object> getUserContextDropdown() { return userContextDropdown; }
    public JComboBox<Object> getBrowsingContextDropdown() { return browsingContextDropdown; }
    public JButton getNewContextButton() { return newContextButton; }
    public JButton getCloseContextButton() { return closeContextButton; }
}
