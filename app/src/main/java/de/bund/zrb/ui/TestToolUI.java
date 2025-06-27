package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.ui.commandframework.*;

import javax.swing.*;
import java.awt.*;

/**
 * TestToolUI with left & right drawers like MainframeMate.
 */
public class TestToolUI {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final CommandRegistry commandRegistry = new CommandRegistryImpl();

    private JFrame frame;

    public void initUI() {
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        frame.setLayout(new BorderLayout());

        registerCommands();
        frame.setJMenuBar(new JMenuBar());

        // Outer SplitPane: Links und Rest
        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setOneTouchExpandable(true);

        LeftDrawer leftDrawer = new LeftDrawer(commandRegistry);
        outerSplit.setLeftComponent(leftDrawer);

        // Inner SplitPane: Center + Rechts
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setOneTouchExpandable(true);

        JPanel mainPanel = createMainPanel();
        innerSplit.setLeftComponent(mainPanel);

        RightDrawer rightDrawer = new RightDrawer(commandRegistry);
        innerSplit.setRightComponent(rightDrawer);

        outerSplit.setRightComponent(innerSplit);

        // Start-Größen
        outerSplit.setDividerLocation(200);
        innerSplit.setDividerLocation(900);

        frame.add(outerSplit, BorderLayout.CENTER);

        frame.setVisible(true);
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
        // Beispiel-Commands
        commandRegistry.register("testsuite.play", new Command() {
            public void execute(CommandContext ctx) {
                String suiteName = (String) ctx.get("suite");
                System.out.println("Running suite: " + suiteName);
            }
        });

        commandRegistry.register("record.start", new Command() {
            public void execute(CommandContext ctx) {
                System.out.println("Recording started.");
            }
        });
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
