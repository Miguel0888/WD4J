// File: app/src/main/java/de/bund/zrb/ui/MainWindow.java
package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.commands.debug.*;
import de.bund.zrb.ui.commands.tools.*;
import de.bund.zrb.ui.widgets.StatusBar;
import de.bund.zrb.ui.widgets.UserSelectionCombo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * TestToolUI with dynamic ActionToolbar and drawers.
 */
public class MainWindow {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();

    private JFrame frame;
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // Für View-Toggles & Persistenz:
    private JSplitPane outerSplit;
    private JSplitPane innerSplit;
    private boolean leftDrawerVisible  = true;
    private boolean rightDrawerVisible = true;
    private int savedOuterDividerLocation = 200;  // Default links
    private int savedInnerDividerLocation = 900;  // Default rechts

    private StatusBar statusBar;
    private UserSelectionCombo userCombo;

    public void initUI() {
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Menüleiste
        registerCommands();
        registerShortcuts();
        JMenuBar mb = MenuTreeBuilder.buildMenuBar();
        frame.setJMenuBar(mb);

        // Toolbar
        ActionToolbar toolbar = new ActionToolbar();
        frame.add(toolbar, BorderLayout.NORTH);

        // Center-Aufbau
        outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setOneTouchExpandable(true);

        LeftDrawer leftDrawer = new LeftDrawer();
        outerSplit.setLeftComponent(leftDrawer);

        innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setOneTouchExpandable(true);
        JPanel mainPanel = createMainPanel();
        innerSplit.setLeftComponent(mainPanel);

        RightDrawer rightDrawer = new RightDrawer(browserService);
        innerSplit.setRightComponent(rightDrawer);
        outerSplit.setRightComponent(innerSplit);

        loadDrawerSettings();
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        frame.add(outerSplit, BorderLayout.CENTER);

        // >>> Statusbar (links Meldungen, rechts User-Selector)
        userCombo = new UserSelectionCombo(UserRegistry.getInstance());
        statusBar = new StatusBar(userCombo);
        frame.add(statusBar, BorderLayout.SOUTH);

//        // Status-Text immer updaten, wenn der aktuelle User wechselt
//        de.bund.zrb.service.UserContextMappingService.getInstance()
//                .addPropertyChangeListener(evt -> {
//                    if (!"currentUser".equals(evt.getPropertyName())) return;
//                    de.bund.zrb.service.UserRegistry.User u =
//                            (de.bund.zrb.service.UserRegistry.User) evt.getNewValue();
//                    String name = (u == null) ? "<Keinen>" : u.getUsername();
//                    statusBar.setMessage("Aktiver Benutzer: " + name);
//                });

        // Browser starten NACHDEM Statusbar steht → wir können dort Meldungen setzen
        initBrowser();

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveDrawerSettings();
                statusBar.setMessage("🛑 Browser wird beendet…");
                browserService.terminateBrowser();
            }
        });

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

        try {
            statusBar.setMessage("🚀 Browser wird gestartet…");
            browserService.launchBrowser(config);
            statusBar.setMessage("✅ Browser gestartet");
        } catch (Exception e) {
            statusBar.setMessage("❌ Browser-Start fehlgeschlagen");
            JOptionPane.showMessageDialog(
                    frame,
                    "Fehler beim Starten des Browsers:\n" + e.getMessage(),
                    "Browser-Start fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    private void registerShortcuts() {
        ShortcutManager.loadShortcuts();
        ShortcutManager.registerGlobalShortcuts(frame.getRootPane());
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private void registerCommands() {
        // Vorhandene/alte Commands
        commandRegistry.register(new ShowShortcutConfigMenuCommand(frame));
        commandRegistry.register(new SettingsCommand());
        commandRegistry.register(new ExpressionEditorCommand());
        commandRegistry.register(new RegexPresetsCommand());
        commandRegistry.register(new PlayTestSuiteCommand(tabbedPane));
        commandRegistry.register(new StopPlaybackCommand());
        commandRegistry.register(new StartRecordCommand());
        commandRegistry.register(new StopRecordCommand());
        commandRegistry.register(new ToggleRecordCommand());

        commandRegistry.register(new LaunchBrowserCommand(browserService));
        commandRegistry.register(new TerminateBrowserCommand(browserService));
        commandRegistry.register(new SwitchTabCommand(browserService));
        commandRegistry.register(new NewTabCommand(browserService));
        commandRegistry.register(new CloseTabCommand(browserService));
        commandRegistry.register(new ReloadTabCommand(browserService));
        commandRegistry.register(new GoBackCommand(browserService));
        commandRegistry.register(new GoForwardCommand(browserService));

        commandRegistry.register(new CaptureScreenshotCommand());
        commandRegistry.register(new ShowOtpDialogCommand());
        commandRegistry.register(new ShowGrowlTesterCommand());
        commandRegistry.register(new ShowNetworkDebuggerCommand());
        commandRegistry.register(new ShowEventMonitorCommand());
        commandRegistry.register(new ShowSelectorsCommand(browserService));
        commandRegistry.register(new ShowDomEventsCommand(browserService));

        commandRegistry.register(new UserRegistryCommand());
        commandRegistry.register(new CycleUserCommand());
        commandRegistry.register(new NavigationHomeCommand());
        commandRegistry.register(new LoginUserCommand());

        // View-Toggles (Shortcut- & Toolbar-fähig)
        commandRegistry.register(new ToggleLeftDrawerCommand(this));
        commandRegistry.register(new ToggleRightDrawerCommand(this));

        // Video:
        commandRegistry.register(new ToggleVideoRecordCommand());
    }

    // ====== View-Menü („Ansicht“) =================================================

    private JMenu buildViewMenu() {
        JMenu view = new JMenu("Ansicht");

        Optional<MenuCommand> toggleLeft = CommandRegistryImpl.getInstance().getById("view.toggleLeftDrawer");
        if (toggleLeft.isPresent()) {
            JMenuItem mi = new JMenuItem(toggleLeft.get().getLabel());
            mi.addActionListener(ev -> toggleLeft.get().perform());
            view.add(mi);
        }

        Optional<MenuCommand> toggleRight = CommandRegistryImpl.getInstance().getById("view.toggleRightDrawer");
        if (toggleRight.isPresent()) {
            JMenuItem mi = new JMenuItem(toggleRight.get().getLabel());
            mi.addActionListener(ev -> toggleRight.get().perform());
            view.add(mi);
        }

        return view;
    }

    // ====== API für Toggle-Commands ==============================================

    /** Linke Seitenleiste ein-/ausblenden und Position speichern. */
    public void toggleLeftDrawer() {
        if (outerSplit == null) return;

        if (leftDrawerVisible) {
            savedOuterDividerLocation = outerSplit.getDividerLocation();
            outerSplit.setDividerLocation(0);
            if (outerSplit.getLeftComponent() != null) {
                outerSplit.getLeftComponent().setVisible(false);
            }
        } else {
            if (outerSplit.getLeftComponent() != null) {
                outerSplit.getLeftComponent().setVisible(true);
            }
            outerSplit.setDividerLocation(savedOuterDividerLocation);
        }
        leftDrawerVisible = !leftDrawerVisible;

        // sofort persistieren
        SettingsService.getInstance().set("leftDividerLocation", outerSplit.getDividerLocation());

        frame.revalidate();
        frame.repaint();
    }

    /** Rechte Seitenleiste ein-/ausblenden und Position speichern. */
    public void toggleRightDrawer() {
        if (innerSplit == null) return;

        if (rightDrawerVisible) {
            savedInnerDividerLocation = innerSplit.getDividerLocation();
            innerSplit.setDividerLocation(innerSplit.getMaximumDividerLocation());
            if (innerSplit.getRightComponent() != null) {
                innerSplit.getRightComponent().setVisible(false);
            }
        } else {
            if (innerSplit.getRightComponent() != null) {
                innerSplit.getRightComponent().setVisible(true);
            }
            innerSplit.setDividerLocation(savedInnerDividerLocation);
        }
        rightDrawerVisible = !rightDrawerVisible;

        // sofort persistieren
        SettingsService.getInstance().set("rightDividerLocation", innerSplit.getDividerLocation());

        frame.revalidate();
        frame.repaint();
    }

    // ====== Persistenz der Divider-Positionen ====================================

    private void loadDrawerSettings() {
        Integer left = SettingsService.getInstance().get("leftDividerLocation", Integer.class);
        Integer right = SettingsService.getInstance().get("rightDividerLocation", Integer.class);
        if (left != null)  savedOuterDividerLocation = left.intValue();
        if (right != null) savedInnerDividerLocation = right.intValue();
    }

    private void saveDrawerSettings() {
        if (outerSplit != null) {
            SettingsService.getInstance().set("leftDividerLocation", outerSplit.getDividerLocation());
        }
        if (innerSplit != null) {
            SettingsService.getInstance().set("rightDividerLocation", innerSplit.getDividerLocation());
        }
    }

    public void setStatus(String text) {
        if (statusBar != null) statusBar.setMessage(text);
    }

    // ====== Startpunkt ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new MainWindow().initUI();
            }
        });
    }
}
