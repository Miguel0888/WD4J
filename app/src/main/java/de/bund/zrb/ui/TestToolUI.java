package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.ui.commandframework.*;

import javax.swing.*;
import java.awt.*;

/**
 * TestToolUI with decoupled CommandFramework.
 */
public class TestToolUI {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();

    private final CommandRegistry commandRegistry = new CommandRegistryImpl();

    private JFrame frame;
    private JMenuBar menuBar;
    private JPanel mainPanel;

    public void initUI() {
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);

        registerCommands();
        menuBar = buildMenuBar();
        mainPanel = createMainPanel();

        frame.setJMenuBar(menuBar);
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void registerCommands() {
        commandRegistry.register("browser.start", new Command() {
            public void execute(CommandContext context) {
                BrowserConfig config = new BrowserConfig();
                config.setBrowserType("firefox");
                config.setHeadless(false);
                config.setNoRemote(false);
                config.setDisableGpu(false);
                config.setStartMaximized(true);
                config.setUseProfile(false);
                config.setPort(9222);

                browserService.launchBrowser(config);
            }
        });

        commandRegistry.register("browser.stop", new Command() {
            public void execute(CommandContext context) {
                browserService.terminateBrowser();
            }
        });

        commandRegistry.register("navigation.newTab", new Command() {
            public void execute(CommandContext context) {
                browserService.createNewTab();
            }
        });

        commandRegistry.register("navigation.closeTab", new Command() {
            public void execute(CommandContext context) {
                browserService.closeActiveTab();
            }
        });

        commandRegistry.register("navigation.reload", new Command() {
            public void execute(CommandContext context) {
                browserService.reload();
            }
        });
    }

    private JMenuBar buildMenuBar() {
        // Menübaum deklarieren
        MenuBuilder builder = new MenuBuilder();

        MenuBuilder.MenuNode root = builder.getRoot();

        MenuBuilder.MenuNode browserMenu = new MenuBuilder.MenuNode(new MenuItemConfig(null, "Browser", null, null));
        browserMenu.addChild(new MenuBuilder.MenuNode(new MenuItemConfig("browser.start", "Starten", null, null)));
        browserMenu.addChild(new MenuBuilder.MenuNode(new MenuItemConfig("browser.stop", "Beenden", null, null)));

        MenuBuilder.MenuNode navigationMenu = new MenuBuilder.MenuNode(new MenuItemConfig(null, "Navigation", null, null));
        navigationMenu.addChild(new MenuBuilder.MenuNode(new MenuItemConfig("navigation.newTab", "Neuer Tab", null, null)));
        navigationMenu.addChild(new MenuBuilder.MenuNode(new MenuItemConfig("navigation.closeTab", "Tab schließen", null, null)));
        navigationMenu.addChild(new MenuBuilder.MenuNode(new MenuItemConfig("navigation.reload", "Neu laden", null, null)));

        root.addChild(browserMenu);
        root.addChild(navigationMenu);

        // Swing-Adapter verwenden
        SwingMenuAdapter swingMenuAdapter = new SwingMenuAdapter(commandRegistry);

        return swingMenuAdapter.build(root);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Recorder", new RecorderPanel());
        tabbedPane.addTab("Test Runner", new RunnerPanel());

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
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
