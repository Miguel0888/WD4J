package app;

import app.config.AppSecurityManager;
import app.controller.MainController;
import app.ui.*;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static MainController controller;

    // Tabs als Felder
    private static BrowserTab browserTab;
    private static NavigationTab navigationTab;
    private static ContextTab contextTab;
    private static DebugTab debugTab;
    private static ScriptTab scriptTab;
    private static TestToolsTab testToolsTab;
    private static SettingsTab settingsTab;

    public static void main(String[] args) {
        System.setSecurityManager(new AppSecurityManager()); // Setzt den Security Manager
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        controller = new MainController();

        // Tabs initialisieren
        browserTab = new BrowserTab(controller);
        navigationTab = new NavigationTab(controller);
        contextTab = new ContextTab(controller);
        debugTab = new DebugTab(controller);
        scriptTab = new ScriptTab(controller);
        testToolsTab = new TestToolsTab(controller);
        settingsTab = new SettingsTab(controller); // Settings mit JSON-Speicherung

        // Toolbars hinzufügen
        JPanel toolBarPanel = new JPanel(new GridLayout(7, 1));
        toolBarPanel.add(browserTab.getToolbar());
        toolBarPanel.add(contextTab.getToolbar());
        toolBarPanel.add(navigationTab.getToolbar());
        toolBarPanel.add(testToolsTab.getToolbar());
        toolBarPanel.add(scriptTab.getToolbar());
        toolBarPanel.add(debugTab.getToolbar());
        toolBarPanel.add(settingsTab.getToolbar());

        // Tabs erstellen
        JTabbedPane tabbedPane = createTabs();

        // Hauptfenster erstellen
        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolBarPanel, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });
    }

    private static JTabbedPane createTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Scripting", scriptTab.getPanel());
        tabbedPane.addTab("Debug", debugTab.getPanel());
        tabbedPane.addTab("Screenshots", testToolsTab.getPanel());
        tabbedPane.addTab("Settings", settingsTab.getPanel());

        return tabbedPane;
    }

    public static ScriptTab getScriptTab() {
        return scriptTab;
    }

    public static DebugTab getDebugTab() {
        return debugTab;
    }

    public static BrowserTab getBrowserTab() {
        return browserTab;
    }

    public static NavigationTab getNavigationTab() {
        return navigationTab;
    }

    public static ContextTab getContextTab() {
        return contextTab;
    }
}
