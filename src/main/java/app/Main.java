package app;

import app.controller.MainController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Main {
    public static String lastProfilePath = ""; // ToDo: Make this configurable
    public static JLabel imageContainer; // Bildcontainer für Screenshots
    public static JTextArea console; // Textfeld für Events
    public static JTextArea scriptLog; // Textfeld für Scripting
    public static JCheckBox headlessCheckbox;
    public static JCheckBox disableGpuCheckbox;
    public static JCheckBox noRemoteCheckbox;
    public static JCheckBox startMaximizedCheckbox;
    public static JComboBox<String> browserSelector;
    public static JButton launchButton;
    public static JButton terminateButton;
    public static JButton screenshotButton;
    public static JTextField addressBar;
    public static JButton navigateButton;
    public static JCheckBox useProfileCheckbox;
    public static JTextField profilePathField;
    public static JTextField portField;

    public static JCheckBox showSelectors;
    public static JCheckBox showDomEvents;

    private static JButton eventDropdownButton;
    private static JPopupMenu eventMenu;
    public static final Map<String, JCheckBoxMenuItem> eventCheckboxes = new HashMap<>();

    public static MainController controller;
    public static JComboBox<Object> browsingContextDropdown;
    public static JComboBox<Object> userContextDropdown;
    private static JToggleButton togglePlayPauseButton;

    private static boolean keepEventDropDownOpen = false; // Schalter, der festlegt, ob das Menü sich schließen darf

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
//        runAppMapServer();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Build UI
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void createAndShowGUI() {
        controller = new MainController();

        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Toolbar-Panel mit vier Zeilen
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayout(5, 1));
        toolBarPanel.add(createBrowserToolBar());
        toolBarPanel.add(createContextsToolbar());
        toolBarPanel.add(createNavigationToolBar());
        toolBarPanel.add(createDebugToolBar());
        toolBarPanel.add(createScriptToolbar());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Tabs für Events & Screenshots (Content Container)
        JTabbedPane tabbedPane = createTabs();

        // Hauptlayout
        frame.setLayout(new BorderLayout());
        frame.add(toolBarPanel, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Event Listeners
        // ToDo: Remove this, since the ui fields are static in Main.java and can be accessed directly
        controller.setupListeners(
                portField,
                useProfileCheckbox,
                profilePathField,
                headlessCheckbox,
                disableGpuCheckbox,
                noRemoteCheckbox,
                startMaximizedCheckbox,
                browserSelector,
                launchButton,
                terminateButton,
                navigateButton,
                addressBar
        );

        screenshotButton.addActionListener(e -> captureScreenshot(controller));

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });

        // ToDo: Implement this additional features (optional, not part of playwright, except of the headless mode)
        // Disable als UI-Options
        useProfileCheckbox.setEnabled(true);
        profilePathField.setEnabled(true);
        headlessCheckbox.setEnabled(true);
        disableGpuCheckbox.setEnabled(true);
        noRemoteCheckbox.setEnabled(true);
        startMaximizedCheckbox.setEnabled(true);

        useProfileCheckbox.setSelected(true);
        profilePathField.setText(""); // ToDo: Remove this line an set Settings from BrowserTypeImpl, here
        headlessCheckbox.setSelected(false);
        disableGpuCheckbox.setSelected(false);
        noRemoteCheckbox.setSelected(false);
        startMaximizedCheckbox.setSelected(false);
    }

    private static JToolBar createBrowserToolBar()
    {
        JToolBar browserToolBar = new JToolBar();
        browserToolBar.setFloatable(false);

        portField = new JTextField("9222", 5);
        useProfileCheckbox = new JCheckBox("", true);
        profilePathField = new JTextField(lastProfilePath, 15);
        useProfileCheckbox.addActionListener(e -> {
            boolean isSelected = useProfileCheckbox.isSelected();
            profilePathField.setEnabled(isSelected);
            if (!isSelected) {
                lastProfilePath = profilePathField.getText();
                profilePathField.setText(null);
            } else {
                profilePathField.setText(lastProfilePath);
            }
        });

        headlessCheckbox = new JCheckBox("Headless");
        disableGpuCheckbox = new JCheckBox("Dis. GPU");
        noRemoteCheckbox = new JCheckBox("No Remote");
        startMaximizedCheckbox = new JCheckBox("Maximized");

        browserSelector = new JComboBox<>(new String[]{"Firefox", "Chrome", "Edge", "Safari"});

        launchButton = new JButton("Launch Browser");
        terminateButton = new JButton("Terminate Browser");

        browserToolBar.add(new JLabel("Browser:"));
        browserToolBar.add(browserSelector);
        browserToolBar.add(new JLabel("Port:"));
        browserToolBar.add(portField);
        browserToolBar.add(useProfileCheckbox);
        browserToolBar.add(new JLabel("Profile Path:"));
        browserToolBar.add(profilePathField);
        browserToolBar.add(headlessCheckbox);
        browserToolBar.add(disableGpuCheckbox);
        browserToolBar.add(noRemoteCheckbox);
        browserToolBar.add(startMaximizedCheckbox);
        browserToolBar.add(launchButton);
        browserToolBar.add(terminateButton);
        return browserToolBar;
    }

    private static JToolBar createNavigationToolBar()
    {
        JToolBar navigationToolBar = new JToolBar();
        navigationToolBar.setFloatable(false);

        JButton goBackButton = new JButton("\u21A9");
        goBackButton.setToolTipText("Back");
        JButton goForwardButton = new JButton("\u21AA");
        goForwardButton.setToolTipText("Forward");
        JButton reloadButton = new JButton("\uD83D\uDD04");
        reloadButton.setToolTipText("Reload");

        addressBar = new JTextField("https://www.google.com", 30);
        navigateButton = new JButton("Navigate");

        // Kamera-Symbol
        screenshotButton = new JButton("\uD83D\uDCF8");
        screenshotButton.setToolTipText("Take Screenshot");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        goBackButton.addActionListener(e -> controller.goBack());
        goForwardButton.addActionListener(e -> controller.goForward());
        reloadButton.addActionListener(e -> controller.reload());
        navigationToolBar.add(goBackButton);
        navigationToolBar.add(goForwardButton);
        navigationToolBar.add(reloadButton);
        navigationToolBar.add(new JLabel("URL:"));
        navigationToolBar.add(addressBar);
        navigationToolBar.add(screenshotButton);
        navigationToolBar.add(navigateButton);
        return navigationToolBar;
    }

    private static JToolBar createDebugToolBar() {
        JToolBar eventToolbar = new JToolBar();

        // Erzeuge ein JPopupMenu, das sich nicht mehr automatisch schließt
        eventDropdownButton = new JButton("Select Events");
        eventMenu = new JPopupMenu() {
            @Override
            public void setVisible(boolean visible) {
                // Prüfe, ob jemand versucht, das Menü zu schließen
                if (!visible) {
                    // Schließen ist nur erlaubt, wenn der Schalter "allowCloseManually" true ist
                    if (keepEventDropDownOpen) {
                        // Abbrechen -> Menü bleibt offen
                        return;
                    }
                }
                super.setVisible(visible);
            }
        };

        // Optional: Leichtgewichtige Popups deaktivieren, falls dein Look & Feel
        // sonst das Menü bei Item-Klick schließt
        eventMenu.setLightWeightPopupEnabled(false);

        // Füge die Checkboxen hinzu
        for (String event : controller.getEventHandlers().keySet()) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(event);
            eventCheckboxes.put(event, menuItem);

            // Direkt beim Klicken registrieren oder entfernen
            menuItem.addActionListener(e -> {
                if (menuItem.isSelected()) {
                    controller.registerEvent(event);
                } else {
                    controller.deregisterEvent(event);
                }
                updateEventDropdownLabel();
            });

            eventMenu.add(menuItem);
        }

        // Button toggelt Menü an/aus
        eventDropdownButton.addActionListener(e -> {
            if (eventMenu.isVisible()) {
                keepEventDropDownOpen = false;
                eventMenu.setVisible(false); // Manuell schließen
            } else {
                keepEventDropDownOpen = true;
                eventMenu.show(eventDropdownButton, 0, eventDropdownButton.getHeight());
            }
        });

        // Erzeugtes Menü & Button in die Toolbar
        eventToolbar.add(eventDropdownButton);
        eventToolbar.add(Box.createHorizontalGlue());

        togglePlayPauseButton = new JToggleButton("Stop");
        togglePlayPauseButton.addItemListener(e -> {
            if (togglePlayPauseButton.isSelected()) {
                togglePlayPauseButton.setText("Play");
                controller.setEventLoggingEnabled(false);
                console.setEnabled(false);
            } else {
                togglePlayPauseButton.setText("Stop");
                controller.setEventLoggingEnabled(true);
                console.setEnabled(true);
            }
        });
        eventToolbar.add(togglePlayPauseButton);

        JButton clearLogButton = new JButton("Clear Console");
        clearLogButton.addActionListener(e -> {
            // Ask for confirmation
            int result = JOptionPane.showConfirmDialog(null, "Do you really want to clear the console?", "Clear Console", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                console.setText("");
            }
        });
        eventToolbar.add(clearLogButton);

        return eventToolbar;
    }

    private static JToolBar createScriptToolbar() {
        JToolBar scriptToolbar = new JToolBar();

        JButton runScript = new JButton("Run Script");
        runScript.addActionListener(e -> {
            String script = scriptLog.getText();
            // show notification if script is empty
            if (script.isEmpty()) { // ToDo
                JOptionPane.showMessageDialog(null, "Script is empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            controller.runScript(script);
        });

        showSelectors = new JCheckBox("Show Selectors");
        showSelectors.addActionListener(e -> {
            controller.showSelectors(showSelectors.isSelected());
        });

        showDomEvents = new JCheckBox("Show DOM Events");
        showDomEvents.addActionListener(e -> {
            controller.showDomEvents(showDomEvents.isSelected());
        });

        JButton clear = new JButton("Clear Events");
        clear.addActionListener(e -> {
            // Ask for confirmation
            int result = JOptionPane.showConfirmDialog(null, "Do you really want to clear the console?", "Clear Console", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                scriptLog.setText("");
            }
        });

        scriptToolbar.add(showSelectors);
        scriptToolbar.add(showDomEvents);
        scriptToolbar.add(Box.createHorizontalGlue()); // Abstandshalter für rechtsbündige Elemente
        scriptToolbar.add(runScript);
        scriptToolbar.add(clear);

        return scriptToolbar;
    }

    private static JToolBar createContextsToolbar() {
        JToolBar contextToolbar = new JToolBar();

        // User Context Combobox (leere Liste)
        userContextDropdown = new JComboBox<>(new DefaultComboBoxModel<>(new Vector<>()));
        userContextDropdown.addItem("default"); // Standardwert
        userContextDropdown.addActionListener(e -> controller.updateSelectedUserContext());

        // Browsing Context Combobox (leere Liste)
        browsingContextDropdown = new JComboBox<>(new DefaultComboBoxModel<>(new Vector<>()));
        browsingContextDropdown.addActionListener(e -> controller.switchSelectedPage());

        JButton newContext = new JButton("+");
        newContext.setToolTipText("Create new browsing context");
        newContext.addActionListener(e -> controller.createBrowsingContext());
        JButton closeContext = new JButton("-");
        closeContext.setToolTipText("Close browsing context");
        closeContext.addActionListener(e -> controller.closePage());

        // Labels & Dropdowns hinzufügen
        contextToolbar.add(new JLabel("User Context:"));
        contextToolbar.add(userContextDropdown);
        contextToolbar.add(new JLabel("Browsing Context:"));
        contextToolbar.add(browsingContextDropdown);
        contextToolbar.add(newContext);
        contextToolbar.add(closeContext);

        return contextToolbar;
    }


    private static void updateEventDropdownLabel() {
        StringBuilder selectedEvents = new StringBuilder("Selected: ");

        boolean first = true;
        for (Map.Entry<String, JCheckBoxMenuItem> entry : eventCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                if (!first) selectedEvents.append(", ");
                selectedEvents.append(entry.getKey());
                first = false;
            }
        }

        if (selectedEvents.toString().equals("Selected Events: ")) {
            eventDropdownButton.setText("Select Events");
        } else {
            eventDropdownButton.setText(selectedEvents.toString());
        }
    }

    private static void registerSelectedEvents() {
        for (Map.Entry<String, JCheckBoxMenuItem> entry : eventCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                controller.registerEvent(entry.getKey());
            }
        }
    }

    private static void deregisterAllEvents() {
        for (String event : eventCheckboxes.keySet()) {
            controller.deregisterEvent(event);
        }

        // Alle Checkboxen im Menü deaktivieren
        for (JCheckBoxMenuItem item : eventCheckboxes.values()) {
            item.setSelected(false);
        }

        updateEventDropdownLabel();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Content Container
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static JTabbedPane createTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Panel für Events
        console = new JTextArea();
        console.setEditable(false);
        JScrollPane console = new JScrollPane(Main.console);
        tabbedPane.addTab("Console", console);

        // Panel für Screenshots
        imageContainer = new JLabel();
        imageContainer.setHorizontalAlignment(SwingConstants.CENTER);
        imageContainer.setVerticalAlignment(SwingConstants.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageContainer);
        imageScrollPane.setPreferredSize(new Dimension(1024, 400));
        tabbedPane.addTab("Screenshots", imageScrollPane);

        // Panel für Scripting
        scriptLog = new JTextArea();
        scriptLog.setEditable(false);
        JScrollPane scriptScrollPane = new JScrollPane(scriptLog);
        tabbedPane.addTab("Scripting", scriptScrollPane);

        return tabbedPane;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Business Logic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Speichert einen Screenshot im "Screenshots"-Tab.
     */
    private static void captureScreenshot(MainController controller) {
        try {
            byte[] imageData = controller.captureScreenshot();
            if (imageData == null || imageData.length == 0) {
                JOptionPane.showMessageDialog(null, "Screenshot failed!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                imageContainer.setIcon(new ImageIcon(image));
                imageContainer.revalidate();
                imageContainer.repaint();
            } else {
                JOptionPane.showMessageDialog(null, "Screenshot data invalid!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error taking screenshot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
