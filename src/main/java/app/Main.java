package app;



import app.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static String lastProfilePath;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        MainController controller = new MainController();

        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Toolbar für die erste Zeile
        JToolBar browserToolBar = new JToolBar();
        browserToolBar.setFloatable(false);

        // Port-Eingabe
        JTextField portField = new JTextField("9222", 5); // Standardport: 9222

        // Checkbox zur Steuerung der Profilverwendung
        JCheckBox useProfileCheckbox = new JCheckBox("", true); // Standardmäßig aktiviert
        // ProfilePath-Eingabe
        JTextField profilePathField = new JTextField("C:\\BrowserProfile", 15);
        // Synchronisierung des Status der Checkbox und der Textbox
        useProfileCheckbox.addActionListener(e -> {
            boolean isSelected = useProfileCheckbox.isSelected();
            profilePathField.setEnabled(isSelected); // Textbox aktivieren/deaktivieren
            if (!isSelected) {
                lastProfilePath = profilePathField.getText(); // Letzten Profilpfad speichern
                profilePathField.setText(null); // Textbox-Inhalt auf null setzen
            }
            else {
                profilePathField.setText(lastProfilePath); // Letzten Profilpfad wiederherstellen
            }
        });

        // Checkboxen für Optionen
        JCheckBox headlessCheckbox = new JCheckBox("Headless");
        JCheckBox disableGpuCheckbox = new JCheckBox("Disable GPU");
        JCheckBox noRemoteCheckbox = new JCheckBox("No Remote");

        // Dropdown für Browserauswahl
        JComboBox<String> browserSelector = new JComboBox<>(
                new String[]{"Firefox", "Chrome", "Edge", "Safari"}
        );

        // Buttons für Browsersteuerung
        JButton launchButton = new JButton("Launch Browser");
        JButton terminateButton = new JButton("Terminate Browser");

        // Erste Zeile zusammenstellen
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
        browserToolBar.add(launchButton);
        browserToolBar.add(terminateButton);

        // Toolbar für die zweite Zeile
        JToolBar navigationToolBar = new JToolBar();
        navigationToolBar.setFloatable(false);

        // Adresszeile und Navigations-Button
        JTextField addressBar = new JTextField("https://www.google.com", 30);
        JButton navigateButton = new JButton("Navigate");

        // Zweite Zeile zusammenstellen
        navigationToolBar.add(new JLabel("URL:"));
        navigationToolBar.add(addressBar);
        navigationToolBar.add(navigateButton);

        // Layout
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayout(2, 1)); // Zwei Zeilen
        toolBarPanel.add(browserToolBar);
        toolBarPanel.add(navigationToolBar);

        frame.setLayout(new BorderLayout());
        frame.add(toolBarPanel, BorderLayout.NORTH);

        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Event Listeners
        controller.setupListeners(
                portField,
                profilePathField,
                headlessCheckbox,
                disableGpuCheckbox,
                noRemoteCheckbox,
                browserSelector,
                launchButton,
                terminateButton,
                navigateButton,
                addressBar
        );

        // Close Browser on Exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });


        // ToDo: Implement this additional features (optional, not part of playwright, except of the headless mode)
        // Disable als UI-Options
        useProfileCheckbox.setEnabled(false);
        profilePathField.setEnabled(false);
        headlessCheckbox.setEnabled(false);
        disableGpuCheckbox.setEnabled(false);
        noRemoteCheckbox.setEnabled(false);

    }


}
