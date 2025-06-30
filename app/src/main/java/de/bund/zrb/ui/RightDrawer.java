package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.controller.CallbackWebSocketServer;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class RightDrawer extends JPanel {

    private final JTabbedPane recorderTabs = new JTabbedPane();

    public RightDrawer() {
        super(new BorderLayout(8, 8));

        add(recorderTabs, BorderLayout.CENTER);

        addPlusTab();
        addNewRecorderSession(); // Starte mit einem Recorder

        recorderTabs.addChangeListener(e -> {
            int plusTabIndex = recorderTabs.getTabCount() - 1;
            int selectedIndex = recorderTabs.getSelectedIndex();

            if (selectedIndex == plusTabIndex && plusTabIndex > 0) {
                recorderTabs.setSelectedIndex(plusTabIndex - 1);
            }
        });
    }

    private void addNewRecorderSession() {
        RecorderSession session = new RecorderSession();
        String shortId = session.getSessionId().substring(0, 6);

        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, session, null, insertIndex);
        recorderTabs.setTabComponentAt(insertIndex, createTabTitle("📝 " + shortId, session));
        recorderTabs.setSelectedComponent(session);
    }

    private void addPlusTab() {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JButton openButton = new JButton("＋");
        openButton.setMargin(new Insets(0, 0, 0, 0));
        openButton.setBorder(BorderFactory.createEmptyBorder());
        openButton.setFocusable(false);
        openButton.setContentAreaFilled(true);
        openButton.setToolTipText("Neuen Recorder-Tab öffnen");

        openButton.addActionListener(e -> addNewRecorderSession());

        tabPanel.add(openButton);

        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, null, null, insertIndex);
        recorderTabs.setTabComponentAt(insertIndex, tabPanel);

        recorderTabs.setEnabledAt(recorderTabs.getTabCount() - 1, false);
    }

    private Component createTabTitle(String title, Component tabContent) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        tabPanel.add(titleLabel);

        JButton closeButton = new JButton("×");
        closeButton.setMargin(new Insets(0, 5, 0, 5));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setToolTipText("Tab schließen");

        closeButton.addActionListener(e -> {
            int index = recorderTabs.indexOfComponent(tabContent);
            if (index >= 0 && index != recorderTabs.getTabCount() - 1) {
                RecorderSession session = (RecorderSession) tabContent;
                session.stopRecordingIfRunning();
                recorderTabs.remove(index);
            }
        });

        tabPanel.add(closeButton);
        return tabPanel;
    }

    /**
     * Eine Recorder-Session pro Tab
     */
    static class RecorderSession extends JPanel {

        private final String sessionId = UUID.randomUUID().toString();
        private final ActionTable actionTable;
        private final JToggleButton recordToggle;

        public RecorderSession() {
            super(new BorderLayout(8, 8));

            this.actionTable = new ActionTable();

            this.recordToggle = new JToggleButton("\u2B24");
            recordToggle.setBackground(Color.RED);
            recordToggle.setFocusPainted(false);
            recordToggle.setToolTipText("Start Recording");

            JButton saveBtn = new JButton("💾 Speichern als Testsuite");
            saveBtn.addActionListener(e -> saveAsTestSuite());

            recordToggle.addActionListener(e -> toggleRecording());

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            topPanel.add(recordToggle);
            topPanel.add(saveBtn);

            add(topPanel, BorderLayout.NORTH);
            add(new JScrollPane(actionTable), BorderLayout.CENTER);

            // 📡 Live-Updates für diesen Recorder
            de.bund.zrb.service.RecorderService.getInstance().addListener(updatedActions -> {
                actionTable.setActions(updatedActions);
            });
        }

        private void toggleRecording() {
            boolean shouldStart = recordToggle.isSelected();

            if (shouldStart && !CallbackWebSocketServer.isRunning()) {
                startRecording();
                recordToggle.setText("\u23F8"); // ⏸️
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
        }

        private void startRecording() {
            CallbackWebSocketServer.toggleCallbackServer(true);
        }

        private void stopRecording() {
            CallbackWebSocketServer.toggleCallbackServer(false);
        }

        public void stopRecordingIfRunning() {
            if (CallbackWebSocketServer.isRunning()) {
                stopRecording();
            }
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
                TestSuite suite = new TestSuite();
                suite.setId(UUID.randomUUID().toString());
                suite.setName(suiteName);

                TestAction firstAction = actions.get(0);
                if (firstAction.getSelectedSelector() != null && firstAction.getSelectedSelector().startsWith("http")) {
                    suite.getGiven().add(new GivenCondition("url", firstAction.getSelectedSelector()));
                } else {
                    suite.getGiven().add(new GivenCondition("url", "https://www.example.com"));
                }

                suite.getThen().add(new ThenExpectation("screenshot", "final-state.png"));

                TestCase testCase = new TestCase();
                testCase.setId(UUID.randomUUID().toString());
                testCase.setName("TestCase - " + suiteName);
                testCase.getWhen().addAll(actions);

                suite.getTestCases().add(testCase);

                TestRegistry.getInstance().addSuite(suite);
                TestRegistry.getInstance().save();

                ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suiteName));

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Speichern der Testsuite:\n" + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}
