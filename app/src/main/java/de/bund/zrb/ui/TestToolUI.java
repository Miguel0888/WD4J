package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.commands.debug.ShowDomEventsCommand;
import de.bund.zrb.ui.commands.debug.ShowSelectorsCommand;
import de.bund.zrb.ui.commands.tools.CaptureScreenshotCommand;

import javax.swing.*;
import java.awt.*;

/**
 * TestToolUI with dynamic ActionToolbar and drawers.
 */
public class TestToolUI {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final CommandRegistry commandRegistry = CommandRegistryImpl.getInstance();

    private JFrame frame;

    public void initUI() {
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        initBrowser();

        registerCommands();
        registerShortcuts();

        // Menübaum aufbauen (nachdem alle Commands da sind!)
        frame.setJMenuBar(MenuTreeBuilder.buildMenuBar());

        // ✅ Nutze deinen ActionToolbar Singleton-Style
        ActionToolbar toolbar = new ActionToolbar();
        frame.add(toolbar, BorderLayout.NORTH);

        // Outer SplitPane: Links und Rest
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setOneTouchExpandable(true);

        LeftDrawer leftDrawer = new LeftDrawer();
        outerSplit.setLeftComponent(leftDrawer);

        // Inner SplitPane: Center + Rechts
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setOneTouchExpandable(true);

        JPanel mainPanel = createMainPanel();
        innerSplit.setLeftComponent(mainPanel);

        RightDrawer rightDrawer = new RightDrawer(browserService);
        innerSplit.setRightComponent(rightDrawer);

        outerSplit.setRightComponent(innerSplit);

        // Start-Größen
        outerSplit.setDividerLocation(200);
        innerSplit.setDividerLocation(900);

        frame.add(outerSplit, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void initBrowser() {
        BrowserConfig config = new BrowserConfig();
        config.setBrowserType("firefox");
        config.setHeadless(false);
        config.setNoRemote(false);
        config.setDisableGpu(false);
        config.setStartMaximized(true);
        config.setUseProfile(false);
        config.setPort(9222);

        // Browser automatisch starten
        try {
            browserService.launchBrowser(config);
        } catch (Exception e) {
            // Zeige eine Fehlermeldung im Dialog
            JOptionPane.showMessageDialog(
                    frame,                                  // Elternkomponente
                    "Fehler beim Starten des Browsers:\n" + e.getMessage(), // Nachricht
                    "Browser-Start fehlgeschlagen",         // Dialog-Titel
                    JOptionPane.ERROR_MESSAGE               // Icon-Typ
            );
            e.printStackTrace(); // Optional: für Log-Ausgabe in der Konsole
        }

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("🛑 Browser wird beendet...");
                browserService.terminateBrowser();
            }
        });
    }

    private void registerShortcuts() {
        ShortcutManager.loadShortcuts();
        ShortcutManager.registerGlobalShortcuts(frame.getRootPane());
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Recorder", new RecorderPanel());
        tabbedPane.addTab("Test Runner", new RunnerPanel());

        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private void registerCommands() {
        commandRegistry.register(new ShowShortcutConfigMenuCommand(frame));
        commandRegistry.register(new SettingsCommand());
        commandRegistry.register(new PlayTestSuiteCommand());
        commandRegistry.register(new StartRecordCommand());

        commandRegistry.register(new LaunchBrowserCommand(browserService));
        commandRegistry.register(new TerminateBrowserCommand(browserService));
        commandRegistry.register(new NewTabCommand(browserService));
        commandRegistry.register(new CloseTabCommand(browserService));
        commandRegistry.register(new ReloadTabCommand(browserService));
        commandRegistry.register(new GoBackCommand(browserService));
        commandRegistry.register(new GoForwardCommand(browserService));

        commandRegistry.register(new CaptureScreenshotCommand(browserService));
        commandRegistry.register(new ShowSelectorsCommand(browserService));
        commandRegistry.register(new ShowDomEventsCommand(browserService));

        commandRegistry.register(new UserRegistryCommand());
        commandRegistry.register(new UserSelectionCommand(UserRegistry.getInstance()));
        commandRegistry.register(new NavigationHomeCommand(BrowserServiceImpl.getInstance()));


    }


    private static class RecorderPanel extends JPanel {
        public RecorderPanel() {
            super(new BorderLayout());
            add(new JLabel("Recorder-Modus: Hier werden Tests aufgezeichnet und editiert."), BorderLayout.NORTH);
            add(new JScrollPane(new JTable()), BorderLayout.CENTER);
        }
    }

    private static class RunnerPanel extends JPanel {
        public RunnerPanel() {
            super(new BorderLayout());
            add(new JLabel("Test Runner: Hier können Tests verwaltet und ausgeführt werden."), BorderLayout.NORTH);
            add(new JScrollPane(new JList<>()), BorderLayout.CENTER);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestToolUI().initUI());
    }
}
