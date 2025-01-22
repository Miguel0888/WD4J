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

        JButton launchButton = new JButton("Launch Browser");
        JComboBox<String> browserSelector = new JComboBox<>(
                new String[]{"Firefox", "Chrome", "Edge", "Safari"}
        );

        JButton navigateButton = new JButton("Navigate");
        JTextField addressBar = new JTextField("https://www.google.com");

        toolBar.add(launchButton);
        toolBar.add(browserSelector);
        toolBar.add(navigateButton);
        toolBar.add(addressBar);

        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(toolBar, BorderLayout.NORTH);

        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Event Listeners
        controller.setupListeners(launchButton, browserSelector, navigateButton, addressBar);

        // Close Browser on Exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });
    }

}
