package app;

import app.controller.MainController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static String lastProfilePath = "C:\\BrowserProfile"; // ToDo: Make this configurable
    private static JLabel imageContainer; // Bildcontainer für Screenshots
    private static JTextArea eventLog; // Textfeld für Events
    private static JButton playButton, pauseButton; // Play/Pause Buttons
    private static JCheckBox headlessCheckbox;
    private static JCheckBox disableGpuCheckbox;
    private static JCheckBox noRemoteCheckbox;
    private static JCheckBox startMaximizedCheckbox;
    private static JComboBox<String> browserSelector;
    private static JButton launchButton;
    private static JButton terminateButton;
    private static JButton screenshotButton;
    private static JTextField addressBar;
    private static JButton navigateButton;
    private static JCheckBox useProfileCheckbox;
    private static JTextField profilePathField;
    private static JTextField portField;

    private static final Map<String, JCheckBox> eventCheckboxes = new HashMap<>();
    private static MainController controller;

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

        // Erste Toolbar (Browser-Steuerung)
        JToolBar browserToolBar = createFirstToolbar();

        // Zweite Toolbar (Navigation)
        JToolBar navigationToolBar = createSecondToolbar();

        // Dritte Toolbar (Event-Steuerung)
        JToolBar eventToolBar = createThirdToolbar();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Toolbar-Panel mit drei Zeilen
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayout(3, 1));
        toolBarPanel.add(browserToolBar);
        toolBarPanel.add(navigationToolBar);
        toolBarPanel.add(eventToolBar);
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
                addressBar,
                eventLog
        );

        screenshotButton.addActionListener(e -> captureScreenshot(controller));

//        playButton.addActionListener(e -> controller.startLogging());
//        pauseButton.addActionListener(e -> controller.stopLogging());
        playButton.addActionListener(e -> registerSelectedEvents());
        pauseButton.addActionListener(e -> deregisterAllEvents());

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

    private static JToolBar createFirstToolbar()
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

    private static JToolBar createSecondToolbar()
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

    private static JToolBar createThirdToolbar() {
        JToolBar eventToolbar = new JToolBar();

        // Play & Pause Buttons
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");

        playButton.addActionListener(e -> registerSelectedEvents());
        pauseButton.addActionListener(e -> deregisterAllEvents());

        eventToolbar.add(playButton);
        eventToolbar.add(pauseButton);

        // Dynamische Checkboxen für Events hinzufügen
        for (String event : controller.getEventHandlers().keySet()) {
            JCheckBox checkBox = new JCheckBox(event);
            checkBox.setSelected(false); // Standardmäßig nicht aktiviert
            eventCheckboxes.put(event, checkBox);
            eventToolbar.add(checkBox);
        }

        return eventToolbar;
    }

    private static void registerSelectedEvents() {
        for (Map.Entry<String, JCheckBox> entry : eventCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                controller.registerEvent(entry.getKey());
            }
        }
    }

    private static void deregisterAllEvents() {
        for (String event : eventCheckboxes.keySet()) {
            controller.deregisterEvent(event);
        }
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
