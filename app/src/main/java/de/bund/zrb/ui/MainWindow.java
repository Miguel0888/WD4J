package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.commands.debug.*;
import de.bund.zrb.ui.commands.tools.*;
import de.bund.zrb.ui.widgets.StatusBar;
import de.bund.zrb.ui.widgets.UserSelectionCombo;
import de.bund.zrb.ui.state.FileUiStateRepository;
import de.bund.zrb.ui.state.UiState;
import de.bund.zrb.ui.state.UiStateService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * TestToolUI with dynamic ActionToolbar and drawers.
 *
 * Responsibility of this class:
 * - Build Swing UI
 * - Bind UI events
 * - Ask UiStateService to restore and persist state
 */
public class MainWindow {

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();

    // Persistenter UI State (Use-Case-Service)
    private final UiStateService uiStateService = new UiStateService(new FileUiStateRepository());

    private JFrame frame;
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // Drawer / SplitPane Layout
    private JSplitPane outerSplit;
    private JSplitPane innerSplit;
    private boolean leftDrawerVisible  = true;
    private boolean rightDrawerVisible = true;
    private int savedOuterDividerLocation = 200;  // default for left drawer
    private int savedInnerDividerLocation = 900;  // default for right drawer

    private StatusBar statusBar;
    private UserSelectionCombo userCombo;

    public void initUI() {
        // 1. Lade gespeicherten Zustand aus UiStateService
        UiState persisted = uiStateService.getUiState();
        UiState.WindowState winState = persisted.getMainWindow();
        UiState.DrawerState leftState = persisted.getLeftDrawer();
        UiState.DrawerState rightState = persisted.getRightDrawer();

        // Übernehme in-memory Defaults aus Persistenz in die lokalen Felder
        this.leftDrawerVisible = leftState.isVisible();
        this.rightDrawerVisible = rightState.isVisible();
        this.savedOuterDividerLocation = leftState.getWidth();
        this.savedInnerDividerLocation = rightState.getWidth();

        // 2. Baue JFrame
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Stelle Fenstergröße / Position wieder her
        if (winState.isMaximized()) {
            // Maximiert: setze Bounds halbwegs sinnvoll und dann maximiere
            frame.setBounds(
                    winState.getX(),
                    winState.getY(),
                    winState.getWidth(),
                    winState.getHeight()
            );
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            // Normaler Zustand
            frame.setBounds(
                    winState.getX(),
                    winState.getY(),
                    winState.getWidth(),
                    winState.getHeight()
            );
        }

        // Menüleiste
        registerCommands();
        registerShortcuts();
        JMenuBar mb = MenuTreeBuilder.buildMenuBar();
        frame.setJMenuBar(mb);

        // Toolbar oben
        ActionToolbar toolbar = new ActionToolbar();
        frame.add(toolbar, BorderLayout.NORTH);

        // Center-Aufbau (Split Panes etc.)
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

        // Stelle Divider-Positionen wieder her
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        // Stelle Sichtbarkeit der Drawer wieder her
        applyInitialLeftDrawerVisibility();
        applyInitialRightDrawerVisibility();

        frame.add(outerSplit, BorderLayout.CENTER);

        // Statusbar unten
        userCombo = new UserSelectionCombo(UserRegistry.getInstance());
        statusBar = new StatusBar(userCombo);
        frame.add(statusBar, BorderLayout.SOUTH);

        // Browser starten NACH Statusbar
        initBrowser();

        // WindowListener zum Persistieren beim Exit
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                // 1. Fensterzustand sichern
                boolean maximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                int x = frame.getX();
                int y = frame.getY();
                int w = frame.getWidth();
                int h = frame.getHeight();

                uiStateService.updateMainWindowState(x, y, w, h, maximized);

                // 2. Linken Drawer-Zustand sichern
                // WENN sichtbar -> echten Divider speichern
                // WENN versteckt -> den zuletzt sinnvollen Wert behalten
                int leftWidthToPersist;
                if (outerSplit != null) {
                    if (leftDrawerVisible) {
                        // Drawer gerade offen -> echten Wert nehmen
                        int currentLoc = outerSplit.getDividerLocation();
                        if (currentLoc > 0) {
                            savedOuterDividerLocation = currentLoc;
                        }
                        leftWidthToPersist = savedOuterDividerLocation;
                    } else {
                        // Drawer gerade zu -> NICHT 0 speichern, sondern den gemerkten Wert
                        leftWidthToPersist = savedOuterDividerLocation;
                    }
                } else {
                    leftWidthToPersist = savedOuterDividerLocation;
                }

                // 3. Rechten Drawer-Zustand sichern
                // Gleiches Prinzip spiegelverkehrt
                int rightWidthToPersist;
                if (innerSplit != null) {
                    int maxLoc = innerSplit.getMaximumDividerLocation();
                    if (rightDrawerVisible) {
                        // Drawer gerade offen -> echten Wert nehmen
                        int currentLoc = innerSplit.getDividerLocation();
                        if (currentLoc < maxLoc) {
                            savedInnerDividerLocation = currentLoc;
                        }
                        rightWidthToPersist = savedInnerDividerLocation;
                    } else {
                        // Drawer gerade zu -> NICHT maxLoc speichern, sondern gemerkte Breite
                        rightWidthToPersist = savedInnerDividerLocation;
                    }
                } else {
                    rightWidthToPersist = savedInnerDividerLocation;
                }

                // 4. Werte an UiStateService geben
                uiStateService.updateLeftDrawerState(leftDrawerVisible, leftWidthToPersist);
                uiStateService.updateRightDrawerState(rightDrawerVisible, rightWidthToPersist);

                // 5. Persistieren
                uiStateService.persist();

                // 6. Browser runterfahren
                statusBar.setMessage("🛑 Browser wird beendet…");
                browserService.terminateBrowser();
            }
        });

        frame.setVisible(true);
    }

    private void applyInitialLeftDrawerVisibility() {
        if (outerSplit == null) {
            return;
        }

        if (!leftDrawerVisible) {
            // Drawer war versteckt → Divider auf 0 und Komponente ausblenden
            savedOuterDividerLocation = outerSplit.getDividerLocation();
            outerSplit.setDividerLocation(0);

            if (outerSplit.getLeftComponent() != null) {
                outerSplit.getLeftComponent().setVisible(false);
            }
        } else {
            // Drawer war sichtbar → Stelle gespeicherte Position her
            if (outerSplit.getLeftComponent() != null) {
                outerSplit.getLeftComponent().setVisible(true);
            }
            outerSplit.setDividerLocation(savedOuterDividerLocation);
        }
    }

    private void applyInitialRightDrawerVisibility() {
        if (innerSplit == null) {
            return;
        }

        if (!rightDrawerVisible) {
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

        commandRegistry.register(new ToggleLeftDrawerCommand(this));
        commandRegistry.register(new ToggleRightDrawerCommand(this));

        commandRegistry.register(new ToggleVideoRecordCommand());
    }

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

    /**
     * Toggle left drawer visibility and remember divider position in memory.
     * Do not persist to disk immediately. Persist on exit only.
     */
    public void toggleLeftDrawer() {
        if (outerSplit == null) {
            return;
        }

        Component leftComp = outerSplit.getLeftComponent();
        if (leftComp == null) {
            return;
        }

        // Determine actual visibility state from UI, not nur aus leftDrawerVisible
        boolean actuallyVisible =
                leftComp.isVisible() &&
                        outerSplit.getDividerLocation() > 0;

        if (actuallyVisible) {
            // Collapse / hide left drawer

            // Only save divider location if it is >0 (usable width)
            int currentLoc = outerSplit.getDividerLocation();
            if (currentLoc > 0) {
                savedOuterDividerLocation = currentLoc;
            }

            // Hide component and move divider hard left
            leftComp.setVisible(false);
            outerSplit.setDividerLocation(0);

            leftDrawerVisible = false;
        } else {
            // Expand / show left drawer

            // Ensure component is visible again
            leftComp.setVisible(true);

            // Restore last known divider location, but fall back to something sane
            int restoreLoc = (savedOuterDividerLocation > 0)
                    ? savedOuterDividerLocation
                    : 200; // fallback default

            outerSplit.setDividerLocation(restoreLoc);

            leftDrawerVisible = true;
        }

        frame.revalidate();
        frame.repaint();
    }

    /**
     * Toggle right drawer visibility and remember divider in memory.
     * Do not persist to disk immediately. Persist on exit only.
     */
    public void toggleRightDrawer() {
        if (innerSplit == null) {
            return;
        }

        Component rightComp = innerSplit.getRightComponent();
        if (rightComp == null) {
            return;
        }

        int maxLoc = innerSplit.getMaximumDividerLocation();

        // Determine actual visibility from UI, not nur aus rightDrawerVisible
        boolean actuallyVisible =
                rightComp.isVisible() &&
                        innerSplit.getDividerLocation() < maxLoc;

        if (actuallyVisible) {
            // Collapse / hide right drawer

            // Save current divider location only if not already collapsed
            int currentLoc = innerSplit.getDividerLocation();
            if (currentLoc < maxLoc) {
                savedInnerDividerLocation = currentLoc;
            }

            // Hide component and shove divider all the way right
            rightComp.setVisible(false);
            innerSplit.setDividerLocation(maxLoc);

            rightDrawerVisible = false;
        } else {
            // Expand / show right drawer

            rightComp.setVisible(true);

            // Restore previous divider location, fallback if needed
            int restoreLoc = (savedInnerDividerLocation < maxLoc)
                    ? savedInnerDividerLocation
                    : Math.max(0, maxLoc - 300); // fallback: 300px Breite rechts

            innerSplit.setDividerLocation(restoreLoc);

            rightDrawerVisible = true;
        }

        frame.revalidate();
        frame.repaint();
    }

    public void setStatus(String text) {
        if (statusBar != null) {
            statusBar.setMessage(text);
        }
    }

    // Optional: eigener main() hier könnte bleiben, aber du hast schon einen Main in de.bund.zrb.Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow().initUI();
            }
        });
    }
}
