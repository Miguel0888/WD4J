package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserServiceImpl;

import javax.swing.*;
import java.awt.*;

public class TestToolUI {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private JFrame frame;
    private JMenuBar menuBar;
    private JPanel mainPanel;

    public void initUI() {
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        menuBar = createMenuBar();
        mainPanel = createMainPanel();

        frame.setJMenuBar(menuBar);
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu browserMenu = new JMenu("Browser");
        JMenuItem launch = new JMenuItem("Starten");
        launch.addActionListener(e -> browserService.launchBrowser("firefox", false, new java.util.HashMap<>()));
        JMenuItem stop = new JMenuItem("Beenden");
        stop.addActionListener(e -> browserService.terminateBrowser());
        browserMenu.add(launch);
        browserMenu.add(stop);

        JMenu navigationMenu = new JMenu("Navigation");
        JMenuItem newTab = new JMenuItem("Neuer Tab");
        newTab.addActionListener(e -> browserService.createNewTab());
        JMenuItem closeTab = new JMenuItem("Tab schließen");
        closeTab.addActionListener(e -> browserService.closeActiveTab());
        JMenuItem reload = new JMenuItem("Neu laden");
        reload.addActionListener(e -> browserService.reload());
        navigationMenu.add(newTab);
        navigationMenu.add(closeTab);
        navigationMenu.add(reload);

        bar.add(browserMenu);
        bar.add(navigationMenu);

        return bar;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Recorder", new RecorderPanel());
        tabbedPane.addTab("Test Runner", new RunnerPanel());

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    // ToDo: Noch sinnvoll mit Funktionalität füllen
    private static class RecorderPanel extends JPanel {
        public RecorderPanel() {
            super(new BorderLayout());
            add(new JLabel("Recorder-Modus: Hier werden Tests aufgezeichnet und editiert."), BorderLayout.NORTH);
            add(new JScrollPane(new JTable()), BorderLayout.CENTER); // Platzhalter für Action-Tabelle
        }
    }

    private static class RunnerPanel extends JPanel {
        public RunnerPanel() {
            super(new BorderLayout());
            add(new JLabel("Test Runner: Hier können Tests verwaltet und ausgeführt werden."), BorderLayout.NORTH);
            add(new JScrollPane(new JList<>()), BorderLayout.CENTER); // Platzhalter für Test Suite Verwaltung
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestToolUI().initUI());
    }
}
