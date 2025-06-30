package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.OpenSettingsCommand;
import de.bund.zrb.ui.commands.PlayTestSuiteCommand;
import de.bund.zrb.ui.commands.ShowShortcutConfigMenuCommand;
import de.bund.zrb.ui.commands.StartRecordCommand;

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

        RightDrawer rightDrawer = new RightDrawer();
        innerSplit.setRightComponent(rightDrawer);

        outerSplit.setRightComponent(innerSplit);

        // Start-Größen
        outerSplit.setDividerLocation(200);
        innerSplit.setDividerLocation(900);

        frame.add(outerSplit, BorderLayout.CENTER);

        frame.setVisible(true);
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
        commandRegistry.register(new OpenSettingsCommand());
        commandRegistry.register(new PlayTestSuiteCommand());
        commandRegistry.register(new StartRecordCommand());
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
