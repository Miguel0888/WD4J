package app.ui;

import app.Main;
import app.controller.MainController;

import javax.swing.*;

public class BrowserTab {
    private final MainController controller;
    private final JToolBar browserToolbar;
    private JComboBox<String> browserSelector;
    private JTextField portField;
    private JCheckBox useProfileCheckbox;
    private JTextField profilePathField;
    private JCheckBox headlessCheckbox, disableGpuCheckbox, noRemoteCheckbox, startMaximizedCheckbox;
    private JButton launchButton, terminateButton;

    public static String lastProfilePath = ""; // ToDo: Make this configurable

    public BrowserTab(MainController controller) {
        this.controller = controller;
        browserToolbar = createBrowserToolBar();
    }

    private JToolBar createBrowserToolBar()
    {
        JToolBar browserToolBar = new JToolBar();
        browserToolBar.setFloatable(false);

        portField = new JTextField("9222", 5);
        useProfileCheckbox = new JCheckBox("", true);
        profilePathField = new JTextField(lastProfilePath, 15);
        useProfileCheckbox.addActionListener(e -> {
            boolean isSelected = useProfileCheckbox.isSelected();
            profilePathField.setEnabled(isSelected);
            if (!isSelected) {
                lastProfilePath = profilePathField.getText();
                profilePathField.setText(null);
            } else {
                profilePathField.setText(lastProfilePath);
            }
        });

        headlessCheckbox = new JCheckBox("Headless");
        disableGpuCheckbox = new JCheckBox("Dis. GPU");
        noRemoteCheckbox = new JCheckBox("No Remote");
        startMaximizedCheckbox = new JCheckBox("Maximized");

        browserSelector = new JComboBox<>(new String[]{"Firefox", "Chrome", "Edge", "Safari"});

        launchButton = new JButton("Launch Browser");
        terminateButton = new JButton("Terminate Browser");

        // Events Listeners
        launchButton.addActionListener(controller::onLaunch);
        terminateButton.addActionListener(controller::onTerminate);

        browserToolBar.add(new JLabel("Browser:"));
        browserToolBar.add(browserSelector);
        browserToolBar.add(new JLabel("Port:"));
        browserToolBar.add(portField);
        browserToolBar.add(useProfileCheckbox);
        browserToolBar.add(new JLabel("Profile Path:"));
        browserToolBar.add(profilePathField);
        browserToolBar.add(headlessCheckbox);
        browserToolBar.add(disableGpuCheckbox);
        browserToolBar.add(noRemoteCheckbox);
        browserToolBar.add(startMaximizedCheckbox);
        browserToolBar.add(launchButton);
        browserToolBar.add(terminateButton);
        return browserToolBar;
    }

    public JToolBar getToolbar() {
        return browserToolbar;
    }

    public JComboBox<String> getBrowserSelector() {
        return browserSelector;
    }

    public JTextField getPortField() {
        return portField;
    }

    public JCheckBox getUseProfileCheckbox() {
        return useProfileCheckbox;
    }

    public JTextField getProfilePathField() {
        return profilePathField;
    }

    public JCheckBox getHeadlessCheckbox() {
        return headlessCheckbox;
    }

    public JCheckBox getDisableGpuCheckbox() {
        return disableGpuCheckbox;
    }

    public JCheckBox getNoRemoteCheckbox() {
        return noRemoteCheckbox;
    }

    public JCheckBox getStartMaximizedCheckbox() {
        return startMaximizedCheckbox;
    }

    public JButton getLaunchButton() {
        return launchButton;
    }

    public JButton getTerminateButton() {
        return terminateButton;
    }
}
