package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.RecorderListener;
import de.bund.zrb.service.RecorderService;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

class RecorderSession extends JPanel implements RecorderListener {

    private final RightDrawer rightDrawer;
    private final ActionTable actionTable;
    private final JToggleButton recordToggle;

    private String contextId;
    private RecorderService recorderService;

    public RecorderSession(RightDrawer rightDrawer) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.actionTable = new ActionTable();

        this.recordToggle = new JToggleButton("\u2B24");
        recordToggle.setBackground(Color.RED);
        recordToggle.setFocusPainted(false);
        recordToggle.setToolTipText("Start Recording");
        recordToggle.addActionListener(e -> toggleRecording());

        JButton saveButton = new JButton("Als Testsuite speichern");
        saveButton.addActionListener(e -> saveAsTestSuite());

        JButton addButton = new JButton("+");
        addButton.setFocusable(false);
        addButton.setToolTipText("Neue Zeile einfügen");
        addButton.addActionListener(e -> insertRow());

        JButton deleteButton = new JButton("🗑");
        deleteButton.setFocusable(false);
        deleteButton.setToolTipText("Markierte Zeilen löschen");
        deleteButton.addActionListener(e -> deleteSelectedRows());

        JButton upButton = new JButton("▲");
        upButton.setFocusable(false);
        upButton.setToolTipText("Markierte Zeilen hochschieben");
        upButton.addActionListener(e -> moveSelectedRows(-1));

        JButton downButton = new JButton("▼");
        downButton.setFocusable(false);
        downButton.setToolTipText("Markierte Zeilen runterschieben");
        downButton.addActionListener(e -> moveSelectedRows(1));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(recordToggle);
        topPanel.add(saveButton);
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(upButton);
        topPanel.add(downButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(actionTable), BorderLayout.CENTER);
    }

    private void toggleRecording() {
        boolean shouldStart = recordToggle.isSelected();

        if (shouldStart) {
            this.contextId = rightDrawer.getBrowserService().getBrowser().getPages().getActivePageId();
            this.recorderService = RecorderService.getInstance(contextId);

            rightDrawer.getBrowserService().getRecordingEventRouter().addListener(contextId, recorderService);
            recorderService.addListener(this);

            System.out.println("📌 Start Recording for Context: " + contextId);

            recordToggle.setText("\u23F8");
            recordToggle.setToolTipText("Stop Recording");
            recordToggle.setBackground(Color.GRAY);

        } else {
            unregister();
            System.out.println("🛑 Stop Recording for Context: " + contextId);

            recordToggle.setText("\u2B24");
            recordToggle.setToolTipText("Start Recording");
            recordToggle.setBackground(Color.RED);
        }
    }

    private void saveAsTestSuite() {
        if (recorderService == null) {
            System.out.println("⚠️ Kein aktiver RecorderService!");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Name der Testsuite eingeben:", "Testsuite speichern", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            System.out.println("⚠️ Kein Name eingegeben.");
            return;
        }

        List<TestAction> actions = actionTable.getActions(); // Nutze aktuelle Tabelle!
        TestCase testCase = new TestCase(name, actions);
        TestSuite suite = new TestSuite(name, Collections.singletonList(testCase));

        TestRegistry.getInstance().addSuite(suite);
        TestRegistry.getInstance().save();

        System.out.println("✅ Testsuite gespeichert: " + name);

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(name));
    }

    private void insertRow() {
        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow < 0) {
            actionTable.addAction(new TestAction());
        } else {
            actionTable.getTableModel().insertActionAt(selectedRow, new TestAction());
        }
    }

    private void deleteSelectedRows() {
        List<TestAction> actions = actionTable.getActions();
        // Von hinten nach vorne löschen
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i).isSelected()) {
                actionTable.getTableModel().removeAction(i);
            }
        }
    }

    private void moveSelectedRows(int direction) {
        List<TestAction> actions = actionTable.getActions();
        if (direction < 0) {
            for (int i = 1; i < actions.size(); i++) {
                if (actions.get(i).isSelected() && !actions.get(i - 1).isSelected()) {
                    Collections.swap(actions, i, i - 1);
                }
            }
        } else {
            for (int i = actions.size() - 2; i >= 0; i--) {
                if (actions.get(i).isSelected() && !actions.get(i + 1).isSelected()) {
                    Collections.swap(actions, i, i + 1);
                }
            }
        }
        actionTable.setActions(actions);
    }

    public void unregister() {
        if (contextId != null) {
            recorderService.removeListener(this);
            rightDrawer.getBrowserService().getRecordingEventRouter().removeListener(contextId, recorderService);
            RecorderService.remove(contextId);
            contextId = null;
        }
    }

    @Override
    public void onRecorderUpdated(List<TestAction> actions) {
        SwingUtilities.invokeLater(() -> actionTable.setActions(actions));
    }
}
