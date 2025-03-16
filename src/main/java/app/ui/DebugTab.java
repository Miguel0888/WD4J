package app.ui;

import app.Main;
import app.controller.MainController;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DebugTab {
    private final MainController controller;
    private final JPanel panel;
    private JToolBar toolbar;

    private final JTextArea console; // Textfeld für Events
    private final JScrollPane consoleScrollable;

    public static final Map<String, JCheckBoxMenuItem> eventCheckboxes = new HashMap<>();

    private static boolean keepEventDropDownOpen = false; // Schalter, der festlegt, ob das Menü sich schließen darf
    private JButton eventDropdownButton;

    public DebugTab(MainController controller) {
        this.controller = controller;
        toolbar = createDebugToolBar();

        // Panel für Events
        panel = new JPanel(new BorderLayout());
        console = new JTextArea();
        console.setEditable(false);
        consoleScrollable = new JScrollPane(console);
        panel.add(consoleScrollable, BorderLayout.CENTER);
    }

    private JToolBar createDebugToolBar() {
        JToolBar eventToolbar = new JToolBar();

        JButton clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.addActionListener(e -> console.setText(""));

        // Erzeuge ein JPopupMenu, das sich nicht mehr automatisch schließt
        eventDropdownButton = new JButton("Select Events");
        JPopupMenu eventMenu = new JPopupMenu() {
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
        eventToolbar.add(new JLabel("Debug WebDriver: "));
        eventToolbar.add(eventDropdownButton);
        eventToolbar.add(Box.createHorizontalGlue());

        JToggleButton togglePlayPauseButton = new JToggleButton("Stop");
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

        JButton clearLogButton = new JButton("Clear Debug Output");
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

    public JToolBar getToolbar() {
        return toolbar;
    }

    private void updateEventDropdownLabel() {
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

    public JPanel getPanel() {
        return panel;
    }

    public JTextArea getConsole() {
        return console;
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> console.append(message + "\n"));
    }

    public void clearLog() {
        SwingUtilities.invokeLater(() -> console.setText(""));
    }
}
