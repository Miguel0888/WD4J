package app;



import app.controller.MainController;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }
    
    private static void createAndShowGUI() {
        // ToDo: May use dependency injection via Spring or other frameworks
        MainController controller = new MainController();

        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton navigateButton = new JButton("Navigate");
        JTextField addressBar = new JTextField("https://www.google.com");
        JComboBox<String> browserSelector = new JComboBox<>(new String[]{"Chrome"});

        toolBar.add(navigateButton);
        toolBar.add(addressBar);
        toolBar.add(browserSelector);

        // Layout
        frame.setLayout(new BorderLayout());
        frame.add(toolBar, BorderLayout.NORTH);

        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Event Listeners
        controller.setupListeners(navigateButton, addressBar, browserSelector);

        // Launch Browser on Start
        controller.onLaunchBrowser();

        // Close Browser on Exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });
    }
}
