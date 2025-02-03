package app;

import app.controller.MainController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class Main {
    private static String lastProfilePath;
    private static JLabel imageContainer; // Bildcontainer für Screenshots
    private static JTextArea eventLog; // Textfeld für Events
    private static JButton playButton, pauseButton; // Play/Pause Buttons

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        MainController controller = new MainController();

        JFrame frame = new JFrame("Web Testing Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Erste Toolbar (Browser-Steuerung)
        JToolBar browserToolBar = new JToolBar();
        browserToolBar.setFloatable(false);

        JTextField portField = new JTextField("9222", 5);
        JCheckBox useProfileCheckbox = new JCheckBox("", true);
        JTextField profilePathField = new JTextField("C:\\BrowserProfile", 15);
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

        JCheckBox headlessCheckbox = new JCheckBox("Headless");
        JCheckBox disableGpuCheckbox = new JCheckBox("Disable GPU");
        JCheckBox noRemoteCheckbox = new JCheckBox("No Remote");

        JComboBox<String> browserSelector = new JComboBox<>(new String[]{"Firefox", "Chrome", "Edge", "Safari"});

        JButton launchButton = new JButton("Launch Browser");
        JButton terminateButton = new JButton("Terminate Browser");
        JButton screenshotButton = new JButton("\uD83D\uDCF8"); // Kamera-Symbol
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
        browserToolBar.add(launchButton);
        browserToolBar.add(terminateButton);
        browserToolBar.add(screenshotButton);

        // Zweite Toolbar (Navigation)
        JToolBar navigationToolBar = new JToolBar();
        navigationToolBar.setFloatable(false);

        JTextField addressBar = new JTextField("https://www.google.com", 30);
        JButton navigateButton = new JButton("Navigate");

        navigationToolBar.add(new JLabel("URL:"));
        navigationToolBar.add(addressBar);
        navigationToolBar.add(navigateButton);

        // Dritte Toolbar (Event-Steuerung)
        JToolBar eventToolBar = new JToolBar();
        eventToolBar.setFloatable(false);

        playButton = new JButton("▶️"); // Play-Symbol
        pauseButton = new JButton("⏸️"); // Pause-Symbol
        playButton.setToolTipText("Start event logging");
        pauseButton.setToolTipText("Pause event logging");

        eventToolBar.add(playButton);
        eventToolBar.add(pauseButton);

        // Toolbar-Panel mit drei Zeilen
        JPanel toolBarPanel = new JPanel();
        toolBarPanel.setLayout(new GridLayout(3, 1));
        toolBarPanel.add(browserToolBar);
        toolBarPanel.add(navigationToolBar);
        toolBarPanel.add(eventToolBar);

        // Tabs für Events & Screenshots
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

        // Hauptlayout
        frame.setLayout(new BorderLayout());
        frame.add(toolBarPanel, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Event Listeners
        controller.setupListeners(
                portField,
                profilePathField,
                headlessCheckbox,
                disableGpuCheckbox,
                noRemoteCheckbox,
                browserSelector,
                launchButton,
                terminateButton,
                navigateButton,
                addressBar,
                eventLog
        );

        screenshotButton.addActionListener(e -> captureScreenshot(controller));

        playButton.addActionListener(e -> controller.startLogging());
        pauseButton.addActionListener(e -> controller.stopLogging());

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                controller.onCloseBrowser();
            }
        });


        // ToDo: Implement this additional features (optional, not part of playwright, except of the headless mode)
        // Disable als UI-Options
        useProfileCheckbox.setEnabled(false);
        profilePathField.setEnabled(false);
        headlessCheckbox.setEnabled(false);
        disableGpuCheckbox.setEnabled(false);
        noRemoteCheckbox.setEnabled(false);
    }

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
