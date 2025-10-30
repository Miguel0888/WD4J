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
 * Main application window.
 *
 * Responsibility of this class:
 * - Build Swing UI
 * - Bind UI events
 * - Track drawer state (position + visibility)
 * - Persist that state on exit
 *
 * IMPORTANT:
 * Do not restore drawer layout at startup.
 * Only observe and persist on shutdown.
 */
public class MainWindow {

    // Consider drawer "visible/open" only if width >= MIN_DRAWER_WIDTH (in px)
    private static final int MIN_DRAWER_WIDTH = 50;

    private final BrowserServiceImpl browserService = BrowserServiceImpl.getInstance();
    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();

    // Persistenter UI State (Writer am Ende, Reader nur für Start-Defaults der Cache-Werte)
    private final UiStateService uiStateService = new UiStateService(new FileUiStateRepository());

    private JFrame frame;
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // SplitPane-Layout
    private JSplitPane outerSplit; // [LeftDrawer | innerSplit]
    private JSplitPane innerSplit; // [MainPanel  | RightDrawer]

    // Live-Flags, die wir beim Exit persistieren
    private boolean leftDrawerVisible  = true;
    private boolean rightDrawerVisible = true;

    // Letzter "guter" Divider-Wert (nicht < MIN_DRAWER_WIDTH)
    private int savedOuterDividerLocation = 200;  // default for left drawer
    private int savedInnerDividerLocation = 900;  // default for right drawer

    private StatusBar statusBar;
    private UserSelectionCombo userCombo;

    public void initUI() {

        // 1. Lade gespeicherten Zustand NUR um Start-Defaults für Cache zu haben
        //    (Wir stellen NICHTS am UI wieder her!)
        UiState persisted = uiStateService.getUiState();
        UiState.WindowState winState   = persisted.getMainWindow();
        UiState.DrawerState leftState  = persisted.getLeftDrawer();
        UiState.DrawerState rightState = persisted.getRightDrawer();

        // Cached "last known good" Werte (Fallback für Persist am Ende)
        // Wenn da Müll drinsteht (< MIN_DRAWER_WIDTH), clampen wir hier direkt nach oben.
        this.savedOuterDividerLocation = leftState.getWidth();
        if (this.savedOuterDividerLocation < MIN_DRAWER_WIDTH) {
            this.savedOuterDividerLocation = MIN_DRAWER_WIDTH;
        }

        this.savedInnerDividerLocation = rightState.getWidth();
        if (this.savedInnerDividerLocation < MIN_DRAWER_WIDTH) {
            this.savedInnerDividerLocation = MIN_DRAWER_WIDTH;
        }

        // Sichtbarkeitsflags initialisieren (nur als Ausgangspunkt für Persist am Ende)
        this.leftDrawerVisible  = leftState.isVisible();
        this.rightDrawerVisible = rightState.isVisible();

        // 2. Baue JFrame
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Stelle Fenstergröße / Position wieder her (das darfst du weiter nutzen, ist unkritisch)
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

        // Menüleiste (Commands müssen vorher registriert sein)
        registerCommands();
        registerShortcuts();
        JMenuBar mb = MenuTreeBuilder.buildMenuBar();
        frame.setJMenuBar(mb);

        // Toolbar oben
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

        // WIR STELLEN HIER NICHT MEHR DEN ALTEN STATE WIEDER HER.
        // Wir nehmen nur Default-Startpositionen, damit die UI überhaupt sinnvoll aussieht.
        //
        // outerSplit steuert Breite des linken Drawers:
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        // innerSplit steuert Breite des MainPanels (nicht direkt die Breite des rechten Drawers):
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        // Listener installieren, NACHDEM die Splits existieren
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

                // 1. Fensterzustand sichern
                boolean maximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                int x = frame.getX();
                int y = frame.getY();
                int w = frame.getWidth();
                int h = frame.getHeight();
                uiStateService.updateMainWindowState(x, y, w, h, maximized);

                // 2. Sichtbarkeit "jetzt gerade" berechnen
                // LEFT:
                // left drawer gilt als sichtbar, wenn dividerLocation >= MIN_DRAWER_WIDTH
                int currentLeftLoc = (outerSplit != null) ? outerSplit.getDividerLocation() : savedOuterDividerLocation;
                boolean currentLeftVisible = currentLeftLoc >= MIN_DRAWER_WIDTH;

                // RIGHT:
                // Bei innerSplit bedeutet dividerLocation = Breite vom linken Bereich (MainPanel).
                // Rechter Drawer ist "sichtbar", wenn MainPanel nicht alles frisst.
                int currentRightLoc = (innerSplit != null) ? innerSplit.getDividerLocation() : savedInnerDividerLocation;
                int maxLoc          = (innerSplit != null) ? innerSplit.getMaximumDividerLocation() : -1;

                boolean currentRightVisible = false;
                if (maxLoc > 0) {
                    // MainPanel-Breite = currentRightLoc.
                    // Wenn MainPanel >= MIN_DRAWER_WIDTH, dann ist rechter Drawer überhaupt im Bild
                    currentRightVisible = currentRightLoc <= (maxLoc - MIN_DRAWER_WIDTH);
                }

                // 3. Letzte sinnvolle Breite updaten, aber nur wenn Schwellwert erfüllt ist
                // LEFT:
                if (currentLeftLoc >= MIN_DRAWER_WIDTH) {
                    savedOuterDividerLocation = currentLeftLoc;
                }
                // RIGHT:
                if (maxLoc > 0 && currentRightLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                    savedInnerDividerLocation = currentRightLoc;
                }

                // 4. Jetzt Persist schreiben
                uiStateService.updateLeftDrawerState(
                        currentLeftVisible,
                        savedOuterDividerLocation
                );

                uiStateService.updateRightDrawerState(
                        currentRightVisible,
                        savedInnerDividerLocation
                );

                // Flush ui.json
                uiStateService.persist();

                // 5. Browser runterfahren
                statusBar.setMessage("🛑 Browser wird beendet…");
                browserService.terminateBrowser();
            }
        });

        frame.setVisible(true);
    }

    /**
     * Install observers on both split panes.
     *
     * Keep last known "good" divider location up to date while user drags.
     * "Good" means:
     * - left drawer: divider >= MIN_DRAWER_WIDTH
     * - right drawer: main panel width leaves at least MIN_DRAWER_WIDTH room for right drawer
     */
    private void installSplitListeners() {

        if (outerSplit != null) {
            outerSplit.addPropertyChangeListener(
                    JSplitPane.DIVIDER_LOCATION_PROPERTY,
                    new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            // Read current divider position
                            int loc = outerSplit.getDividerLocation();

                            // Only accept if drawer is "open enough"
                            if (loc >= MIN_DRAWER_WIDTH) {
                                // remember last good width
                                savedOuterDividerLocation = loc;
                            }
                            // else: ignore. keep previous good value
                        }
                    }
            );
        }

        if (innerSplit != null) {
            innerSplit.addPropertyChangeListener(
                    JSplitPane.DIVIDER_LOCATION_PROPERTY,
                    new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {

                            int loc    = innerSplit.getDividerLocation();
                            int maxLoc = innerSplit.getMaximumDividerLocation();

                            // Für right drawer:
                            // innerSplit.dividerLocation = Breite der linken Komponente (MainPanel)
                            // Rechter Drawer hat "brauchbare" Breite,
                            // wenn der Divider NICHT ganz rechts klebt.
                            //
                            // D. h. das MainPanel darf NICHT so breit sein,
                            // dass rechts nix übrig bleibt.
                            //
                            // Also: akzeptiere loc nur, wenn rechts >= MIN_DRAWER_WIDTH Platz bleibt.
                            if (maxLoc > 0 && loc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                                savedInnerDividerLocation = loc;
                            }
                            // else: ignore. keep previous good value.
                        }
                    }
            );
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
            mi.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent ev) {
                    toggleLeft.get().perform();
                }
            });
            view.add(mi);
        }

        Optional<MenuCommand> toggleRight = CommandRegistryImpl.getInstance().getById("view.toggleRightDrawer");
        if (toggleRight.isPresent()) {
            JMenuItem mi = new JMenuItem(toggleRight.get().getLabel());
            mi.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent ev) {
                    toggleRight.get().perform();
                }
            });
            view.add(mi);
        }

        return view;
    }

    /**
     * Toggle left drawer.
     *
     * Rule:
     * - If currently "visible enough" (divider >= MIN_DRAWER_WIDTH), collapse:
     *   remember current divider as last good, then set divider to 0.
     * - Else expand:
     *   restore last good divider (savedOuterDividerLocation, clamped).
     */
    public void toggleLeftDrawer() {
        if (outerSplit == null) {
            return;
        }

        int currentLoc = outerSplit.getDividerLocation();
        boolean currentlyVisible = currentLoc >= MIN_DRAWER_WIDTH;

        if (currentlyVisible) {
            // remember this width
            if (currentLoc >= MIN_DRAWER_WIDTH) {
                savedOuterDividerLocation = currentLoc;
            }

            // collapse
            outerSplit.setDividerLocation(0);
            leftDrawerVisible = false;

        } else {
            // expand using last known good width
            int restoreLoc = savedOuterDividerLocation;
            if (restoreLoc < MIN_DRAWER_WIDTH) {
                restoreLoc = MIN_DRAWER_WIDTH;
            }

            outerSplit.setDividerLocation(restoreLoc);
            leftDrawerVisible = true;
        }

        frame.revalidate();
        frame.repaint();
    }

    /**
     * Toggle right drawer.
     *
     * Reminder for right side math:
     * innerSplit dividerLocation == width of the LEFT component (mainPanel).
     *
     * We treat the right drawer as "visible" if the divider is NOT slammed
     * all the way to the far right. Concretely: mainPanel width (loc) must
     * leave at least MIN_DRAWER_WIDTH px for the right drawer.
     *
     * Collapse:
     * - remember current divider if it's a good width for reopening
     * - then shove divider all the way to maxLoc (mainPanel takes all space)
     *
     * Expand:
     * - restore savedInnerDividerLocation (clamped so that right drawer
     *   actually appears)
     */
    public void toggleRightDrawer() {
        if (innerSplit == null) {
            return;
        }

        int maxLoc = innerSplit.getWidth()
                - innerSplit.getDividerSize()
                - innerSplit.getInsets().right;
        int currentLoc = innerSplit.getDividerLocation();

        boolean currentlyVisible = false;
        if (maxLoc > 0) {
            currentlyVisible = (currentLoc <= (maxLoc - MIN_DRAWER_WIDTH));
        }

        if (currentlyVisible) {
            // remember this divider position as last "good" open size
            if (maxLoc > 0 && currentLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                savedInnerDividerLocation = currentLoc;
            }

            // collapse: push divider to maxLoc (right drawer invisible)
            if (maxLoc > 0) {
                innerSplit.setDividerLocation(maxLoc);
            }
            rightDrawerVisible = false;

        } else {
            // expand using last known good value
            int restoreLoc = savedInnerDividerLocation;

            // clamp restoreLoc so that right drawer really sichtbar wird,
            // also NICHT maxLoc, und nicht so weit rechts, dass rechts 0px bleibt
            if (maxLoc > 0) {
                if (restoreLoc <= 0) {
                    // fallback: ~300px Drawer
                    restoreLoc = Math.max(0, maxLoc - 300);
                }
                if (restoreLoc >= maxLoc) {
                    restoreLoc = Math.max(0, maxLoc - 300);
                }
                // UND: Stelle sicher, dass rechter Drawer >= MIN_DRAWER_WIDTH Platz hat
                if (restoreLoc > (maxLoc - MIN_DRAWER_WIDTH)) {
                    restoreLoc = Math.max(0, maxLoc - MIN_DRAWER_WIDTH);
                }
            }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow().initUI();
            }
        });
    }
}
