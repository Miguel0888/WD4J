package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.RecorderService;
import de.bund.zrb.controller.CallbackWebSocketServer;
import de.bund.zrb.service.TestRegistry;

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
        recordToggle.setToolTipText("Start Recording");

        recordToggle.addActionListener(e -> {
            boolean shouldStart = recordToggle.isSelected();

            if (shouldStart && !CallbackWebSocketServer.isRunning()) {
                startRecording();
                recordToggle.setText("\u23F8"); // Unicode Pause-Symbol ⏸️
                recordToggle.setToolTipText("Stop Recording");
                recordToggle.setBackground(Color.GRAY);
            } else if (!shouldStart && CallbackWebSocketServer.isRunning()) {
                stopRecording();
                recordToggle.setText("\u2B24");
                recordToggle.setToolTipText("Start Recording");
                recordToggle.setBackground(Color.RED);
            } else {
                System.out.println("⚠️ Recorder war schon im gewünschten Zustand!");
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
        CallbackWebSocketServer.toggleCallbackServer(true);
    }

    private void stopRecording() {
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

        try {
            // ✅ Neue Suite mit ID & Name
            TestSuite suite = new TestSuite();
            suite.setId(UUID.randomUUID().toString());
            suite.setName(suiteName);

            // Suite-Level GIVEN: z. B. erste URL
            TestAction firstAction = actions.get(0);
            if (firstAction.getSelectedSelector() != null && firstAction.getSelectedSelector().startsWith("http")) {
                suite.getGiven().add(new GivenCondition("url", firstAction.getSelectedSelector()));
            } else {
                suite.getGiven().add(new GivenCondition("url", "https://www.example.com"));
            }

            // Suite-Level THEN: Screenshot
            suite.getThen().add(new ThenExpectation("screenshot", "final-state.png"));

            // ✅ Case mit allen Steps
            TestCase testCase = new TestCase();
            testCase.setId(UUID.randomUUID().toString());
            testCase.setName("TestCase - " + suiteName);
            testCase.getWhen().addAll(actions);

            suite.getTestCases().add(testCase);

            // Speichern
            TestRegistry.getInstance().addSuite(suite);
            TestRegistry.getInstance().save();

            // Event feuern
            ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suiteName));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Speichern der Testsuite:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }




}
