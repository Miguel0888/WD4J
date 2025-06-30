package de.bund.zrb.ui;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.RecorderService;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.controller.CallbackWebSocketServer;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class RightDrawer extends JPanel {

    private final ActionTable actionTable;

    public RightDrawer() {
        super(new BorderLayout());

        this.actionTable = new ActionTable();

        // ✅ ToggleButton für Record/Stop
        JToggleButton recordToggle = new JToggleButton("\u2B24"); // gefüllter Kreis
        recordToggle.setBackground(Color.RED);
        recordToggle.setFocusPainted(false);
        recordToggle.setToolTipText("Start/Stop Recording");

        recordToggle.addActionListener(e -> {
            if (recordToggle.isSelected()) {
                startRecording();
                recordToggle.setText("■ Stop");
                recordToggle.setBackground(Color.GRAY);
            } else {
                stopRecording();
                recordToggle.setText("\u2B24"); // wieder Kreis
                recordToggle.setBackground(Color.RED);
            }
        });

        // Speichern-Button
        JButton saveBtn = new JButton("💾 Speichern als Testsuite");
        saveBtn.addActionListener(e -> saveAsTestSuite());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(recordToggle);
        topPanel.add(saveBtn);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(actionTable), BorderLayout.CENTER);

        // 📡 Live-Updates vom RecorderService abonnieren
        RecorderService.getInstance().addListener(updatedActions -> {
            actionTable.setActions(updatedActions);
        });
    }

    private void startRecording() {
        System.out.println("🚦 Recording gestartet …");
        CallbackWebSocketServer.toggleCallbackServer(true);
    }

    private void stopRecording() {
        System.out.println("⏸️ Recording gestoppt.");
        CallbackWebSocketServer.toggleCallbackServer(false);
    }

    private void saveAsTestSuite() {
        List<TestAction> actions = actionTable.getActions();

        if (actions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Aktionen zum Speichern vorhanden.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String suiteName = JOptionPane.showInputDialog(this, "Name der Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (suiteName == null || suiteName.trim().isEmpty()) return;

        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID().toString());
        testCase.setName("TestCase-" + suiteName);
        testCase.setWhen(actions);

        TestSuite suite = new TestSuite();
        suite.setId(UUID.randomUUID().toString());
        suite.setName(suiteName);
        suite.getTestCases().add(testCase);

        // Speichern
        SettingsService.getInstance().save("testsuites/" + suiteName + ".json", suite);

        JOptionPane.showMessageDialog(this, "Testsuite gespeichert: " + suiteName);
    }

}
