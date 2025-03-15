package app;

import app.config.AppSecurityManager;
import app.controller.MainController;
import app.ui.*;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static MainController controller;

    // Tabs als Felder
    private static NavigationTab navigationTab;
    private static DebugTab debugTab;
    private static ScriptTab scriptTab;
    private static ConsoleTab consoleTab;
    private static ScreenshotTab screenshotTab;
    private static SettingsTab settingsTab;

    public static void main(String[] args) {
        System.setSecurityManager(new AppSecurityManager()); // Setzt den Security Manager
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        controller = new MainController();

        // Tabs initialisieren
        navigationTab = new NavigationTab(controller);
        debugTab = new DebugTab(controller);
        scriptTab = new ScriptTab(controller);
        consoleTab = new ConsoleTab();
        screenshotTab = new ScreenshotTab(controller);
        settingsTab = new SettingsTab(); // Settings mit JSON-Speicherung

        // Toolbars hinzufügen
        JPanel toolBarPanel = new JPanel(new GridLayout(5, 1));
        toolBarPanel.add(settingsTab.getToolbar());
        toolBarPanel.add(navigationTab.getToolbar());
        toolBarPanel.add(debugTab.getToolbar());
        toolBarPanel.add(scriptTab.getToolbar());
        toolBarPanel.add(screenshotTab.getToolbar());

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
    }

    private static JTabbedPane createTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Console", consoleTab.getPanel());
        tabbedPane.addTab("Screenshots", screenshotTab.getPanel());
        tabbedPane.addTab("Settings", settingsTab.getPanel());

        return tabbedPane;
    }

    public static ScriptTab getScriptTab() {
        return scriptTab;
    }

    public static ConsoleTab getConsoleTab() {
        return consoleTab;
    }

    public static SettingsTab getSettingsTab() {
        return settingsTab;
    }
}
