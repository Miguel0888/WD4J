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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

    // Drawer is considered "visible/open" only if width >= MIN_DRAWER_WIDTH
    private static final int MIN_DRAWER_WIDTH = 50;

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

    // last known good divider position when drawer is open
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
            frame.setBounds(
                    winState.getX(),
                    winState.getY(),
                    winState.getWidth(),
                    winState.getHeight()
            );
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
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

        // Stelle Divider-Positionen wieder her (raw)
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        // Stelle Sichtbarkeit / eingeklappt-sein wieder her
        applyInitialLeftDrawerVisibility();
        applyInitialRightDrawerVisibility();

        // Installiere Listener, die die "last good width" aktuell halten,
        // damit Toggle nach einem "start hidden" trotzdem weiß, wohin er wieder aufklappen soll.
        installSplitListeners();

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

                // 1. Sammle aktuellen Fensterzustand
                boolean maximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                int x = frame.getX();
                int y = frame.getY();
                int w = frame.getWidth();
                int h = frame.getHeight();

                uiStateService.updateMainWindowState(x, y, w, h, maximized);

                // 2. Sammle aktuellen Drawer-Zustand
                int leftWidthToPersist;
                if (outerSplit != null) {
                    if (leftDrawerVisible) {
                        int currentLoc = outerSplit.getDividerLocation();
                        // Update last good width if drawer currently open with usable width
                        if (currentLoc >= MIN_DRAWER_WIDTH) {
                            savedOuterDividerLocation = currentLoc;
                        }
                        leftWidthToPersist = savedOuterDividerLocation;
                    } else {
                        // Drawer zu -> nicht 0/max persistieren, sondern last good width
                        leftWidthToPersist = savedOuterDividerLocation;
                    }
                } else {
                    leftWidthToPersist = savedOuterDividerLocation;
                }

                int rightWidthToPersist;
                if (innerSplit != null) {
                    int maxLoc = innerSplit.getMaximumDividerLocation();
                    if (rightDrawerVisible) {
                        int currentLoc = innerSplit.getDividerLocation();
                        // Update last good width if drawer currently open with usable width
                        if (currentLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                            savedInnerDividerLocation = currentLoc;
                        }
                        rightWidthToPersist = savedInnerDividerLocation;
                    } else {
                        rightWidthToPersist = savedInnerDividerLocation;
                    }
                } else {
                    rightWidthToPersist = savedInnerDividerLocation;
                }

                // 3. Persistiere UI State ins Repository
                uiStateService.updateLeftDrawerState(leftDrawerVisible, leftWidthToPersist);
                uiStateService.updateRightDrawerState(rightDrawerVisible, rightWidthToPersist);

                uiStateService.persist();

                // 4. Fahre Browser sauber herunter
                statusBar.setMessage("🛑 Browser wird beendet…");
                browserService.terminateBrowser();
            }
        });

        frame.setVisible(true);
    }

    /**
     * Restore left drawer visibility.
     *
     * Problem in der alten Version:
     * - Wenn der Drawer als "eingeklappt" gespeichert war, wurde hier
     *   savedOuterDividerLocation mit 0 überschrieben.
     *   Danach wusste toggleLeftDrawer() nicht mehr wohin aufklappen.
     *
     * Fix:
     * - Do not overwrite savedOuterDividerLocation when starting hidden.
     * - Only write savedOuterDividerLocation if we actually see a valid width (>= MIN_DRAWER_WIDTH).
     */
    private void applyInitialLeftDrawerVisibility() {
        if (outerSplit == null) {
            return;
        }

        Component leftComp = outerSplit.getLeftComponent();
        if (leftComp == null) {
            return;
        }

        if (!leftDrawerVisible) {
            // Drawer soll versteckt starten.
            // IMPORTANT: Do not clobber savedOuterDividerLocation with 0 here.
            // Keep last good width for future expand.
            int currentLoc = outerSplit.getDividerLocation();
            if (currentLoc >= MIN_DRAWER_WIDTH) {
                // only update cache if this is a meaningful width
                savedOuterDividerLocation = currentLoc;
            }

            leftComp.setVisible(false);
            outerSplit.setDividerLocation(0);
        } else {
            // Drawer soll sichtbar starten.
            // Stelle gespeicherte Breite wieder her, aber clamp auf MIN_DRAWER_WIDTH.
            if (savedOuterDividerLocation < MIN_DRAWER_WIDTH) {
                savedOuterDividerLocation = MIN_DRAWER_WIDTH;
            }

            leftComp.setVisible(true);
            outerSplit.setDividerLocation(savedOuterDividerLocation);
        }
    }

    /**
     * Restore right drawer visibility.
     *
     * Gleicher Fix wie links: beim Start im eingeklappten Zustand wird
     * savedInnerDividerLocation NICHT mehr kaputtgeschrieben.
     */
    private void applyInitialRightDrawerVisibility() {
        if (innerSplit == null) {
            return;
        }

        Component rightComp = innerSplit.getRightComponent();
        if (rightComp == null) {
            return;
        }

        int maxLoc = innerSplit.getMaximumDividerLocation();

        if (!rightDrawerVisible) {
            int currentLoc = innerSplit.getDividerLocation();
            if (currentLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                // only update if current divider is actually giving us a visible width
                savedInnerDividerLocation = currentLoc;
            }

            rightComp.setVisible(false);
            innerSplit.setDividerLocation(maxLoc);
        } else {
            // Visible start
            // Ensure savedInnerDividerLocation is sane (not maxLoc)
            if (savedInnerDividerLocation <= 0 || savedInnerDividerLocation >= maxLoc) {
                savedInnerDividerLocation = Math.max(0, maxLoc - 300); // fallback ~300px right pane
            }

            rightComp.setVisible(true);
            innerSplit.setDividerLocation(savedInnerDividerLocation);
        }
    }

    /**
     * Install listeners that keep track of "last good width" while user drags.
     *
     * This is the missing puzzle piece to make the *first* toggle after
     * "start hidden" work reliably:
     *
     * - Whenever the user drags a divider to a usable width (>= MIN_DRAWER_WIDTH
     *   for left, or <= maxLoc-MIN_DRAWER_WIDTH for right), update the cached
     *   saved*DividerLocation immediately.
     *
     * - When it is collapsed (width < MIN_DRAWER_WIDTH / at maxLoc), do not
     *   update the saved*DividerLocation. This preserves the last meaningful width.
     */
    private void installSplitListeners() {
        if (outerSplit != null) {
            outerSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    // Read current divider position
                    int loc = outerSplit.getDividerLocation();
                    Component leftComp = outerSplit.getLeftComponent();

                    // Consider "open" only if component is visible AND divider is beyond threshold
                    if (leftComp != null && leftComp.isVisible() && loc >= MIN_DRAWER_WIDTH) {
                        // Update last good width for left drawer
                        savedOuterDividerLocation = loc;
                    }
                }
            });
        }

        if (innerSplit != null) {
            innerSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    int loc = innerSplit.getDividerLocation();
                    int maxLoc = innerSplit.getMaximumDividerLocation();
                    Component rightComp = innerSplit.getRightComponent();

                    // Consider "open" only if component is visible AND divider is sufficiently left
                    if (rightComp != null && rightComp.isVisible() && loc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                        // Update last good width for right drawer
                        savedInnerDividerLocation = loc;
                    }
                }
            });
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
     *
     * Fix:
     * - Use savedOuterDividerLocation captured by listener / startup.
     * - When currently visible: collapse and mark invisible.
     * - When currently hidden: expand using last good width (>= MIN_DRAWER_WIDTH).
     */
    public void toggleLeftDrawer() {
        if (outerSplit == null) {
            return;
        }

        Component leftComp = outerSplit.getLeftComponent();
        if (leftComp == null) {
            return;
        }

        int currentLoc = outerSplit.getDividerLocation();
        boolean actuallyVisible =
                leftComp.isVisible() &&
                        currentLoc >= MIN_DRAWER_WIDTH;

        if (actuallyVisible) {
            // Save current width as last good width
            if (currentLoc >= MIN_DRAWER_WIDTH) {
                savedOuterDividerLocation = currentLoc;
            }

            // Hide component and move divider to 0
            leftComp.setVisible(false);
            outerSplit.setDividerLocation(0);

            leftDrawerVisible = false;
        } else {
            // Show again using last good width
            int restoreLoc = savedOuterDividerLocation;
            if (restoreLoc < MIN_DRAWER_WIDTH) {
                restoreLoc = MIN_DRAWER_WIDTH;
            }

            leftComp.setVisible(true);
            outerSplit.setDividerLocation(restoreLoc);

            leftDrawerVisible = true;
        }

        frame.revalidate();
        frame.repaint();
    }

    /**
     * Toggle right drawer visibility and remember divider in memory.
     * Do not persist to disk immediately. Persist on exit only.
     *
     * Fix symmetrical to left: we rely on savedInnerDividerLocation kept up to date by listener.
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
        int currentLoc = innerSplit.getDividerLocation();

        boolean actuallyVisible =
                rightComp.isVisible() &&
                        currentLoc <= (maxLoc - MIN_DRAWER_WIDTH);

        if (actuallyVisible) {
            // Save current divider location as last good width
            if (currentLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                savedInnerDividerLocation = currentLoc;
            }

            // Hide right drawer: push divider fully right and hide component
            rightComp.setVisible(false);
            innerSplit.setDividerLocation(maxLoc);

            rightDrawerVisible = false;
        } else {
            // Expand right drawer using last good width
            int restoreLoc = savedInnerDividerLocation;
            if (restoreLoc <= 0 || restoreLoc >= maxLoc) {
                restoreLoc = Math.max(0, maxLoc - 300); // fallback ~300px
            }

            rightComp.setVisible(true);
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
