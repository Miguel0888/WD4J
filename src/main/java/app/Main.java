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
    public static JTextArea eventLog; // Textfeld für Events
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

    private static JButton eventDropdownButton;
    private static JPopupMenu eventMenu;
    public static final Map<String, JCheckBoxMenuItem> eventCheckboxes = new HashMap<>();

    public static MainController controller;
    public static JComboBox<Object> browsingContextDropdown;
    public static JComboBox<Object> userContextDropdown;
    private static JToggleButton togglePlayPauseButton;

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
        toolBarPanel.add(createEventToolBar());
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
        // Kamera-Symbol
        screenshotButton = new JButton("\uD83D\uDCF8");
        screenshotButton.setToolTipText("Take Screenshot");

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
        browserToolBar.add(screenshotButton);
        return browserToolBar;
    }

    private static JToolBar createNavigationToolBar()
    {
        JToolBar navigationToolBar = new JToolBar();
        navigationToolBar.setFloatable(false);

        addressBar = new JTextField("https://www.google.com", 30);
        navigateButton = new JButton("Navigate");

        navigationToolBar.add(new JLabel("URL:"));
        navigationToolBar.add(addressBar);
        navigationToolBar.add(navigateButton);
        return navigationToolBar;
    }

    private static JToolBar createEventToolBar() {
        JToolBar eventToolbar = new JToolBar();

        // Play/Pause Toggle Button
        togglePlayPauseButton = new JToggleButton("Play");
        togglePlayPauseButton.addItemListener(e -> {
            if (togglePlayPauseButton.isSelected()) {
                togglePlayPauseButton.setText("Pause");
                registerSelectedEvents();
            } else {
                togglePlayPauseButton.setText("Play");
                deregisterAllEvents();
            }
        });
        eventToolbar.add(togglePlayPauseButton);

        // Multi-Select Dropdown für Events (links)
        eventDropdownButton = new JButton("Select Events");
        eventMenu = new JPopupMenu();

        for (String event : controller.getEventHandlers().keySet()) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(event);
            eventCheckboxes.put(event, menuItem);
            menuItem.addActionListener(e -> updateEventDropdownLabel());
            eventMenu.add(menuItem);
        }

        eventDropdownButton.addActionListener(e -> eventMenu.show(eventDropdownButton, 0, eventDropdownButton.getHeight()));

        eventToolbar.add(eventDropdownButton);

        // Abstandshalter für rechtsbündige Elemente (damit Dropdown & Play links bleiben)
        eventToolbar.add(Box.createHorizontalGlue());

        // "Clear Log"-Button rechtsbündig hinzufügen
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> controller.clearLog());
        eventToolbar.add(clearLogButton);

        return eventToolbar;
    }

    private static JToolBar createScriptToolbar() {
        JToolBar scriptToolbar = new JToolBar();

        showSelectors = new JCheckBox("Show Selectors");

        showSelectors.addActionListener(e -> {
            controller.showSelectors(showSelectors.isSelected());
        });

        scriptToolbar.add(showSelectors);

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
        browsingContextDropdown.addItem("All"); // Standardwert
        browsingContextDropdown.addActionListener(e -> controller.updateSelectedPage());

        // Labels & Dropdowns hinzufügen
        contextToolbar.add(new JLabel("User Context:"));
        contextToolbar.add(userContextDropdown);
        contextToolbar.add(new JLabel("Browsing Context:"));
        contextToolbar.add(browsingContextDropdown);

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

        if (selectedEvents.toString().equals("Selected: ")) {
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
        eventLog = new JTextArea();
        eventLog.setEditable(false);
        JScrollPane eventScrollPane = new JScrollPane(eventLog);
        tabbedPane.addTab("Events", eventScrollPane);

        // Panel für Screenshots
        imageContainer = new JLabel();
        imageContainer.setHorizontalAlignment(SwingConstants.CENTER);
        imageContainer.setVerticalAlignment(SwingConstants.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imageContainer);
        imageScrollPane.setPreferredSize(new Dimension(1024, 400));
        tabbedPane.addTab("Screenshots", imageScrollPane);
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
