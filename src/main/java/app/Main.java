package app;



import app.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        MainController controller = new MainController();

        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Port-Eingabe
        JTextField portField = new JTextField("9222", 5); // Standardport: 9222

        // Dropdown für Browserauswahl
        JComboBox<String> browserSelector = new JComboBox<>(
                new String[]{"Firefox", "Chrome", "Edge", "Safari"}
        );

        // Buttons
        JButton launchButton = new JButton("Launch Browser");
        JButton navigateButton = new JButton("Navigate");

        // Adressfeld
        JTextField addressBar = new JTextField("https://www.google.com");

        // Toolbar zusammenstellen
        toolBar.add(addressBar);
        toolBar.add(navigateButton);
        toolBar.add(browserSelector);
        toolBar.add(new JLabel("Port:"));
        toolBar.add(portField);
        toolBar.add(launchButton);

        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(toolBar, BorderLayout.NORTH);

        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Event Listeners
        controller.setupListeners(portField, browserSelector, launchButton, navigateButton, addressBar);

        // Close Browser on Exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });
    }
}
