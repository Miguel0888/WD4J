package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserConfig;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.commandframework.*;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.commands.debug.*;
import de.bund.zrb.ui.commands.tools.*;
import de.bund.zrb.ui.status.StatusTicker;
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
    private static JTabbedPane rightEditorTabsRef;

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

    public static JTabbedPane findRightEditorTabbedPane(Component anyDescendant) {
        // EINFACH: gib die static-Ref zurück.
        return rightEditorTabsRef;
    }

    public void initUI() {

        // 1. Zustand laden
        UiState persisted = uiStateService.getUiState();
        UiState.WindowState winState   = persisted.getMainWindow();
        UiState.DrawerState leftState  = persisted.getLeftDrawer();
        UiState.DrawerState rightState = persisted.getRightDrawer();

        // Cache vorbereiten
        this.savedOuterDividerLocation = leftState.getWidth();
        if (this.savedOuterDividerLocation < MIN_DRAWER_WIDTH) {
            this.savedOuterDividerLocation = MIN_DRAWER_WIDTH;
        }

        this.savedInnerDividerLocation = rightState.getWidth();
        if (this.savedInnerDividerLocation < MIN_DRAWER_WIDTH) {
            this.savedInnerDividerLocation = MIN_DRAWER_WIDTH;
        }

        this.leftDrawerVisible  = leftState.isVisible();
        this.rightDrawerVisible = rightState.isVisible();

        // 2. Frame bauen
        frame = new JFrame("Web Test Recorder & Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

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

        // Commands, Menü, Toolbar
        registerCommands();
        registerShortcuts();
        JMenuBar mb = MenuTreeBuilder.buildMenuBar();
        frame.setJMenuBar(mb);

        ActionToolbar toolbar = new ActionToolbar();
        frame.add(toolbar, BorderLayout.NORTH);

        // 3. Center-Aufbau (Split Panes etc.)
        outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setOneTouchExpandable(true);

        LeftDrawer leftDrawer = new LeftDrawer(tabbedPane);
        outerSplit.setLeftComponent(leftDrawer);

        innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit.setOneTouchExpandable(true);

        JPanel mainPanel = createMainPanel();
        innerSplit.setLeftComponent(mainPanel);

        RightDrawer rightDrawer = new RightDrawer(browserService);
        innerSplit.setRightComponent(rightDrawer);

        outerSplit.setRightComponent(innerSplit);

        // Erst mal irgendwas Sinnvolles setzen, damit es nicht wild blinkt.
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);

        frame.add(outerSplit, BorderLayout.CENTER);

        // Statusbar unten
        userCombo = new UserSelectionCombo(UserRegistry.getInstance());
        statusBar = new StatusBar(userCombo);
        frame.add(statusBar, BorderLayout.SOUTH);
        StatusTicker.getInstance().attach(statusBar); // activate event queue

        // Browser erst nach Statusbar, wie gehabt
        initBrowser();

        // Frame anzeigen, Layout ausführen lassen
        frame.setVisible(true);
        frame.validate(); // Stelle sicher, dass innerSplit Größen hat

        // 4. Jetzt gespeicherten Drawer-Zustand wirklich anwenden
        restoreLeftDrawerFromState();
        restoreRightDrawerFromState();

        // 5. Listener installieren (erst NACH restore!)
        installSplitListeners();

        // 6. WindowListener für Persist beim Exit
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                boolean maximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                int x = frame.getX();
                int y = frame.getY();
                int w = frame.getWidth();
                int h = frame.getHeight();
                uiStateService.updateMainWindowState(x, y, w, h, maximized);

                // Sichtbarkeit Links
                int currentLeftLoc = (outerSplit != null) ? outerSplit.getDividerLocation() : savedOuterDividerLocation;
                boolean currentLeftVisible = currentLeftLoc >= MIN_DRAWER_WIDTH;

                // Sichtbarkeit Rechts (Pixel-Logik)
                int currentRightLoc = (innerSplit != null) ? innerSplit.getDividerLocation() : savedInnerDividerLocation;
                int maxLoc = (innerSplit != null)
                        ? getMaxLoc()
                        : -1;

                boolean currentRightVisible = false;
                if (maxLoc > 0) {
                    currentRightVisible = currentRightLoc <= (maxLoc - MIN_DRAWER_WIDTH);
                }

                // Letzte sinnvolle Breite updaten
                if (currentLeftLoc >= MIN_DRAWER_WIDTH) {
                    savedOuterDividerLocation = currentLeftLoc;
                }
                if (maxLoc > 0 && currentRightLoc <= (maxLoc - MIN_DRAWER_WIDTH)) {
                    savedInnerDividerLocation = currentRightLoc;
                }

                // Persist
                uiStateService.updateLeftDrawerState(
                        currentLeftVisible,
                        savedOuterDividerLocation
                );

                uiStateService.updateRightDrawerState(
                        currentRightVisible,
                        savedInnerDividerLocation
                );

                uiStateService.persist();

                statusBar.setMessage("🛑 Browser wird beendet…");
                browserService.terminateBrowser();
            }
        });

        // Convert old format to new one automatically:
//        de.bund.zrb.service.TestRegistry.getInstance().save();
    }

    /**
     * Restore left drawer exactly like a manual toggle sequence would.
     *
     * Rule:
     * - Wenn persisted.visible == true:
     *      Stelle Divider auf savedOuterDividerLocation (mindestens MIN_DRAWER_WIDTH),
     *      und lass ihn offen.
     * - Wenn persisted.visible == false:
     *      Merke savedOuterDividerLocation (aus Persist), klapp ihn zu (Divider = 0).
     *
     * Diese Methode muss NACH frame.validate() laufen,
     * weil outerSplit erst dann korrekte Größen kennt.
     */
    private void restoreLeftDrawerFromState() {
        if (outerSplit == null) {
            return;
        }

        // clamp safety
        if (savedOuterDividerLocation < MIN_DRAWER_WIDTH) {
            savedOuterDividerLocation = MIN_DRAWER_WIDTH;
        }

        if (leftDrawerVisible) {
            // offen starten
            outerSplit.setDividerLocation(savedOuterDividerLocation);
        } else {
            // zu starten
            outerSplit.setDividerLocation(0);
        }
    }

    /**
     * Restore right drawer state using the exact same geometry rules
     * as toggleRightDrawer() und windowClosing().
     *
     * Rule:
     * - Wenn persisted.visible == true:
     *      DividerLocation = savedInnerDividerLocation (geclamped),
     *      also rechter Drawer sichtbar.
     *
     * - Wenn persisted.visible == false:
     *      Divider ganz nach rechts fahren (komplett eingeklappt),
     *      aber savedInnerDividerLocation NICHT verlieren
     *      (das haben wir ja schon aus Persist gelesen).
     *
     * Diese Methode MUSS laufen, nachdem innerSplit gemessen wurde
     * (also nach frame.setVisible(true); frame.validate();).
     */
    private void restoreRightDrawerFromState() {
        if (innerSplit == null) {
            return;
        }

        // berechne "voll eingeklappt"-Position (rechter Drawer verschwunden)
        int fullCollapsePos = getMaxLoc();

        // falls wegen Layout (noch) nichts Sinnvolles drinsteht:
        if (fullCollapsePos < 0) {
            fullCollapsePos = 0;
        }

        // clamp savedInnerDividerLocation jetzt anhand der echten Breite
        int restoreLoc = savedInnerDividerLocation;
        if (restoreLoc <= 0) {
            restoreLoc = Math.max(0, fullCollapsePos - 300); // Fallback analog Toggle
        }
        if (restoreLoc >= fullCollapsePos) {
            restoreLoc = Math.max(0, fullCollapsePos - 300);
        }
        if (restoreLoc > (fullCollapsePos - MIN_DRAWER_WIDTH)) {
            restoreLoc = Math.max(0, fullCollapsePos - MIN_DRAWER_WIDTH);
        }

        if (rightDrawerVisible) {
            // Drawer soll offen starten -> Divider dahin setzen
            innerSplit.setDividerLocation(restoreLoc);
        } else {
            // Drawer soll zu starten -> Divider ganz nach rechts
            innerSplit.setDividerLocation(fullCollapsePos);
        }
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

                            int loc = innerSplit.getDividerLocation();

                            // Berechne die reale "zugeklappt"-Position analog zu toggleRightDrawer()
                            int fullCollapsePos = getMaxLoc();

                            // right drawer gilt als "offen genug", wenn rechts >= MIN_DRAWER_WIDTH Platz bleibt
                            if (fullCollapsePos > 0 && loc <= (fullCollapsePos - MIN_DRAWER_WIDTH)) {
                                savedInnerDividerLocation = loc;
                            }
                            // else: ignore, behalte alten Wert
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

        int maxLoc = getMaxLoc();
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

    /**
     * Calculate the theoretical "fully collapsed" divider position for innerSplit.
     *
     * Meaning:
     * Move divider so far to the right that the right drawer would be completely hidden.
     *
     * Explanation:
     * - dividerLocation == width of left component.
     * - In fully collapsed state, left component should occupy basically the full width.
     *
     * Fallback logic:
     * Return 0 if innerSplit is not yet laid out (width == 0 or null insets).
     */
    // NOTE: Alternatively calculate innerSplit.getMaximumDividerLocation() + innerSplit.getRightComponent().getMinimumSize().getWidth
    private int getMaxLoc() {
        if (innerSplit == null) {
            return 0;
        }

        return innerSplit.getWidth()
                - innerSplit.getDividerSize()
                - innerSplit.getInsets().right;
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
