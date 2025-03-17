package app;

import app.config.AppSecurityManager;
import app.controller.MainController;
import app.ui.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static MainController controller;

    // 🔹 Map für die UI-Komponenten
    private static final Map<String, UIComponent> componentsMap = new LinkedHashMap<>();

    public static void main(String[] args) {
        System.setSecurityManager(new AppSecurityManager()); // Setzt den Security Manager
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        controller = new MainController();

        // 🔹 UI-Komponenten initialisieren und in die HashMap speichern
        componentsMap.put("Recorder", new TestRecorderTab(controller));
        componentsMap.put("Browser", new BrowserTab(controller));
        componentsMap.put("Context", new ContextTab(controller));
        componentsMap.put("Navigation", new NavigationTab(controller));
        componentsMap.put("Script", new ScriptTab(controller));
        componentsMap.put("TestTools", new TestToolsTab(controller));
        componentsMap.put("Debug", new DebugTab(controller));
        componentsMap.put("Settings", new SettingsTab(controller));

        // 🔹 Anzahl der tatsächlich vorhandenen Toolbars ermitteln
        int toolbarCount = 0;
        for (UIComponent component : componentsMap.values()) {
            if (component.getToolbar() != null) {
                toolbarCount++;
            }
        }

        // 🔹 Toolbars automatisch hinzufügen (falls vorhanden)
        JPanel toolBarPanel = new JPanel(new GridLayout(toolbarCount, 1));
        for (UIComponent component : componentsMap.values()) {
            JToolBar toolbar = component.getToolbar();
            if (toolbar != null) {
                toolBarPanel.add(toolbar);
            }
        }

        // 🔹 Tabs automatisch erstellen (falls vorhanden)
        JTabbedPane tabbedPane = new JTabbedPane();
        for (UIComponent component : componentsMap.values()) {
            JPanel panel = component.getPanel();
            if (panel != null) {
                tabbedPane.addTab(component.getComponentTitle(), panel);
            }
        }

        // 🔹 Menü generieren (falls vorhanden)
        JMenuBar menuBar = new JMenuBar();
        for (UIComponent component : componentsMap.values()) {
            JMenuItem menuItem = component.getMenuItem();
            if (menuItem instanceof JMenu) {  // Falls es ein ganzes Menü ist, direkt hinzufügen
                menuBar.add((JMenu) menuItem);
            } else if (menuItem != null) {  // Falls es nur ein einzelner Menüpunkt ist
                JMenu dynamicMenu = new JMenu(component.getComponentTitle());
                dynamicMenu.add(menuItem);
                menuBar.add(dynamicMenu);
            }
        }

        // 🔥 Hauptfenster erstellen
        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setJMenuBar(menuBar);
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

    // ToDo: Refactor this section...
    // 🔹 Getter für die Tabs (vorübergehend, bis Map genutzt wird)
    public static BrowserTab getBrowserTab() {
        return (BrowserTab) componentsMap.get("Browser");
    }

    public static NavigationTab getNavigationTab() {
        return (NavigationTab) componentsMap.get("Navigation");
    }

    public static ContextTab getContextTab() {
        return (ContextTab) componentsMap.get("Context");
    }

    public static DebugTab getDebugTab() {
        return (DebugTab) componentsMap.get("Debug");
    }

    public static ScriptTab getScriptTab() {
        return (ScriptTab) componentsMap.get("Script");
    }

    public static TestToolsTab getTestToolsTab() {
        return (TestToolsTab) componentsMap.get("TestTools");
    }

    public static SettingsTab getSettingsTab() {
        return (SettingsTab) componentsMap.get("Settings");
    }
}
