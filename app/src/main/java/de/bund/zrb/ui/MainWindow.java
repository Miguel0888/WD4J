// File: app/src/main/java/de/bund/zrb/ui/MainWindow.java
package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.commands.debug.ShowDomEventsCommand;
import de.bund.zrb.ui.commands.debug.ShowSelectorsCommand;
import de.bund.zrb.ui.commands.tools.*;

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
        JMenuBar mb = MenuTreeBuilder.buildMenuBar();
        // ---- "Ansicht" direkt rechts neben "Datei" ergänzen:
        mb.add(buildViewMenu(), Math.min(1, mb.getMenuCount())); // Position 1 = rechts neben "Datei"
        frame.setJMenuBar(mb);

        // ✅ Toolbar (Singleton-artig, ohne Param.)
        ActionToolbar toolbar = new ActionToolbar();
        frame.add(toolbar, BorderLayout.NORTH);

        // Outer SplitPane: Links und Rest
        outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setOneTouchExpandable(true);

        LeftDrawer leftDrawer = new LeftDrawer();
        outerSplit.setLeftComponent(leftDrawer);

        // Inner SplitPane: Center + Rechts
        innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setOneTouchExpandable(true);

        JPanel mainPanel = createMainPanel();
        innerSplit.setLeftComponent(mainPanel);

        RightDrawer rightDrawer = new RightDrawer(browserService);
        innerSplit.setRightComponent(rightDrawer);

        outerSplit.setRightComponent(innerSplit);

        // Gespeicherte Divider-Positionen laden
        loadDrawerSettings();

        // Start-Positionen anwenden
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        frame.add(outerSplit, BorderLayout.CENTER);

        // Beim Beenden Positionsspeicherung & Browser beenden
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveDrawerSettings();
                System.out.println("🛑 Browser wird beendet...");
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
            browserService.launchBrowser(config);
        } catch (Exception e) {
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
        commandRegistry.register(new PlayTestSuiteCommand(tabbedPane));
        commandRegistry.register(new StopPlaybackCommand());
        commandRegistry.register(new StartRecordCommand());
        commandRegistry.register(new StopRecordCommand());
        commandRegistry.register(new ToggleRecordCommand());
        commandRegistry.register(new StartEventServiceCommand());
        commandRegistry.register(new StopEventServiceCommand());

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
        commandRegistry.register(new ShowSelectorsCommand(browserService));
        commandRegistry.register(new ShowDomEventsCommand(browserService));

        commandRegistry.register(new UserRegistryCommand());
        commandRegistry.register(new UserSelectionCommand(UserRegistry.getInstance()));
        commandRegistry.register(new NavigationHomeCommand());
        commandRegistry.register(new LoginUserCommand());

        // Neu: View-Toggles (Shortcut- & Toolbar-fähig)
        commandRegistry.register(new ToggleLeftDrawerCommand(this));
        commandRegistry.register(new ToggleRightDrawerCommand(this));
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

    // ====== Startpunkt ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new MainWindow().initUI();
            }
        });
    }
}
