// File: app/src/main/java/de/bund/zrb/ui/MainWindow.java
package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commands.*;
import de.bund.zrb.ui.tabs.UIHelper;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

public class MainWindow extends JFrame {

    private CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();
    private JSplitPane outerSplit;
    private JSplitPane innerSplit;
    private boolean leftDrawerVisible = true;
    private boolean rightDrawerVisible = true;
    private int savedOuterDividerLocation = 250;
    private int savedInnerDividerLocation = 800;

    public MainWindow() {
        super("WD4J");
        initUI();
    }

    private void initUI() {
        // 1) existierende Commands registrieren
        commandRegistry.registerDefaults();

        // 2) SplitPane anlegen
        outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        outerSplit.setContinuousLayout(true);
        innerSplit.setContinuousLayout(true);
        outerSplit.setOneTouchExpandable(true);
        innerSplit.setOneTouchExpandable(true);

        loadDrawerSettings();

        // 3) Drawer erstellen
        LeftDrawer leftDrawer = new LeftDrawer();
        RightDrawer rightDrawer = new RightDrawer();
        JPanel centerPane = new JPanel(new BorderLayout());
        centerPane.add(new JLabel("Mitte", SwingConstants.CENTER), BorderLayout.CENTER);

        outerSplit.setLeftComponent(leftDrawer);
        outerSplit.setRightComponent(innerSplit);
        innerSplit.setLeftComponent(centerPane);
        innerSplit.setRightComponent(rightDrawer);

        // 4) Toggle-Befehle registrieren
        commandRegistry.register(new ToggleLeftDrawerCommand(this));
        commandRegistry.register(new ToggleRightDrawerCommand(this));

        // 5) bestehende Befehle nach den Toggles registrieren
        commandRegistry.register(new PlayTestSuiteCommand());
        commandRegistry.register(new StopPlaybackCommand());
        commandRegistry.register(new LaunchBrowserCommand());
        commandRegistry.register(new SwitchTabCommand("testsuite", "Test Suites"));

        // 6) Shortcuts registrieren (danach)
        commandRegistry.registerGlobalShortcuts(this);

        // 7) Menü aufbauen
        setJMenuBar(buildMenuBar());

        // 8) Toolbar erzeugen (mit evtl. Standardkonfiguration)
        ActionToolbar toolbar = new ActionToolbar(commandRegistry);
        add(toolbar, BorderLayout.NORTH);

        // 9) Frame-Layout setzen
        getContentPane().add(outerSplit, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                BrowserServiceImpl.getInstance().stop();
                saveDrawerSettings();
            }
        });

        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    /** Menübar erzeugen: Datei, Ansicht, Einstellungen, Hilfe etc. */
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setOpaque(true);

        // Menü "Datei"
        JMenu fileMenu = new JMenu("Datei");
        for (MenuElement elem : MenuTreeBuilder.buildMenuTree(commandRegistry, "file").getSubElements()) {
            fileMenu.add(elem.getComponent());
        }
        mb.add(fileMenu);

        // Menü "Ansicht" (neu)
        JMenu viewMenu = new JMenu("Ansicht");
        // Toggle Left
        Optional<MenuCommand> toggleLeft = commandRegistry.getById("view.toggleLeftDrawer");
        if (toggleLeft.isPresent()) {
            JMenuItem mItem = new JMenuItem(toggleLeft.get().getLabel());
            mItem.addActionListener(a -> toggleLeft.get().perform());
            viewMenu.add(mItem);
        }
        // Toggle Right
        Optional<MenuCommand> toggleRight = commandRegistry.getById("view.toggleRightDrawer");
        if (toggleRight.isPresent()) {
            JMenuItem mItem = new JMenuItem(toggleRight.get().getLabel());
            mItem.addActionListener(a -> toggleRight.get().perform());
            viewMenu.add(mItem);
        }
        mb.add(viewMenu);

        // Menü "Einstellungen"
        JMenu settingsMenu = new JMenu("Einstellungen");
        for (MenuElement elem : MenuTreeBuilder.buildMenuTree(commandRegistry, "file.configure").getSubElements()) {
            settingsMenu.add(elem.getComponent());
        }
        mb.add(settingsMenu);

        return mb;
    }

    /** Seitliche Panels ein-/ausblenden: Links */
    public void toggleLeftDrawer() {
        if (leftDrawerVisible) {
            savedOuterDividerLocation = outerSplit.getDividerLocation();
            outerSplit.setDividerLocation(0);
            outerSplit.getLeftComponent().setVisible(false);
        } else {
            outerSplit.getLeftComponent().setVisible(true);
            outerSplit.setDividerLocation(savedOuterDividerLocation);
        }
        leftDrawerVisible = !leftDrawerVisible;
        SettingsService.getInstance().set("leftDividerLocation", outerSplit.getDividerLocation());
    }

    /** Seitliche Panels ein-/ausblenden: Rechts */
    public void toggleRightDrawer() {
        if (rightDrawerVisible) {
            savedInnerDividerLocation = innerSplit.getDividerLocation();
            innerSplit.setDividerLocation(innerSplit.getMaximumDividerLocation());
            innerSplit.getRightComponent().setVisible(false);
        } else {
            innerSplit.getRightComponent().setVisible(true);
            innerSplit.setDividerLocation(savedInnerDividerLocation);
        }
        rightDrawerVisible = !rightDrawerVisible;
        SettingsService.getInstance().set("rightDividerLocation", innerSplit.getDividerLocation());
    }

    /** Einstellungen laden: zuletzt gespeicherte Divider-Positionen */
    private void loadDrawerSettings() {
        Integer left = SettingsService.getInstance().get("leftDividerLocation", Integer.class);
        Integer right = SettingsService.getInstance().get("rightDividerLocation", Integer.class);
        savedOuterDividerLocation = (left != null) ? left : 250;
        savedInnerDividerLocation = (right != null) ? right : 800;
        outerSplit.setDividerLocation(savedOuterDividerLocation);
        innerSplit.setDividerLocation(savedInnerDividerLocation);
    }

    /** Einstellungen speichern: Divider-Positionen beim Beenden */
    private void saveDrawerSettings() {
        SettingsService.getInstance().set("leftDividerLocation", outerSplit.getDividerLocation());
        SettingsService.getInstance().set("rightDividerLocation", innerSplit.getDividerLocation());
    }
}
