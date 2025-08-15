package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Swing tab for one user; delegate recording to service. */
public final class RecorderTab extends JPanel implements RecorderTabUi {

    private final RightDrawer rightDrawer;
    private final UserRegistry.User selectedUser;

    private final ActionTable actionTable = new ActionTable();
    private final JToggleButton recordToggle = new JToggleButton("\u2B24"); // red dot

    private final JComboBox<String> suiteDropdown = new JComboBox<String>();

    private RecordingSession session;

    public RecorderTab(RightDrawer rightDrawer, UserRegistry.User user) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.selectedUser = user;

        // Fill suites
        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            suiteDropdown.addItem(suite.getName());
        }

        // Wire buttons
        recordToggle.setBackground(Color.RED);
        recordToggle.setFocusPainted(false);
        recordToggle.setToolTipText("Start Recording");
        recordToggle.addActionListener(e -> {
            // Delegate to service; UI state is updated via onRecordingStateChanged()
            RecorderCoordinator.getInstance().toggleForUser(selectedUser.getUsername());
        });

        JButton saveButton = new JButton("Neue Testsuite speichern");
        saveButton.addActionListener(e -> saveAsNewTestSuite());

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

        JButton importButton = new JButton("⤵");
        importButton.setFocusable(false);
        importButton.setToolTipText("Testsuite importieren und Recorder füllen");
        importButton.addActionListener(e -> importSuite());

        JButton exportButton = new JButton("⤴");
        exportButton.setFocusable(false);
        exportButton.setToolTipText("In gewählte Suite exportieren");
        exportButton.addActionListener(e -> exportToSuite());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(recordToggle);
        topPanel.add(saveButton);
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(upButton);
        topPanel.add(downButton);

        topPanel.add(Box.createHorizontalStrut(40));
        topPanel.add(suiteDropdown);
        topPanel.add(importButton);
        topPanel.add(exportButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(actionTable), BorderLayout.CENTER);

        // Register with coordinator (inject BrowserService via RightDrawer)
        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());
    }

    // ---------- RecorderTabUi ----------

    @Override public String getUsername() { return selectedUser.getUsername(); }

    @Override public boolean isVisibleActive() {
        // Decide based on Swing visibility; adjust if you have a TabbedPane API
        return isShowing() && isVisible();
    }

    @Override public void setActions(final List<TestAction> actions) {
        // Update table on EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { actionTable.setActions(actions); }
        });
    }

    @Override public void setRecordingUiState(final boolean recording) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (recording) {
                    recordToggle.setSelected(true);
                    recordToggle.setText("\u23F8");
                    recordToggle.setToolTipText("Stop Recording");
                    recordToggle.setBackground(Color.GRAY);
                } else {
                    recordToggle.setSelected(false);
                    recordToggle.setText("\u2B24");
                    recordToggle.setToolTipText("Start Recording");
                    recordToggle.setBackground(Color.RED);
                }
            }
        });
    }

    // ---------- RecorderListener (actions) ----------

    // ÄNDERT: implementiere State-Callback sauber über die bestehende UI-Methode
    @Override
    public void onRecordingStateChanged(boolean recording) {
        setRecordingUiState(recording);
    }

    @Override
    public void onRecorderUpdated(final List<TestAction> actions) {
        setActions(actions);
    }

    // ---------- UI helper ops delegating to session ----------

    private void saveAsNewTestSuite() {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new ArrayList<TestAction>();

        String name = JOptionPane.showInputDialog(this, "Name der Testsuite eingeben:",
                "Neue Testsuite speichern", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        List<TestCase> testCases = splitIntoTestCases(actions, name);
        TestSuite suite = new TestSuite(name, testCases);
        TestRegistry.getInstance().addSuite(suite);
        TestRegistry.getInstance().save();

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(name));

        session.clearRecordedEvents();
    }

    private void exportToSuite() {
        String selectedSuiteName = (String) suiteDropdown.getSelectedItem();
        if (selectedSuiteName == null) return;

        TestSuite suite = TestRegistry.getInstance().getAll().stream()
                .filter(s -> s.getName().equals(selectedSuiteName))
                .findFirst().orElse(null);
        if (suite == null) return;

        List<TestAction> actions = session.getAllTestActionsForDrawer();
        List<TestCase> newCases = splitIntoTestCases(actions, selectedSuiteName + "_Part");
        suite.getTestCases().addAll(newCases);
        TestRegistry.getInstance().save();

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(selectedSuiteName));
        session.clearRecordedEvents();
    }

    private void importSuite() {
        String selectedSuiteName = (String) suiteDropdown.getSelectedItem();
        if (selectedSuiteName == null) return;

        TestSuite suite = TestRegistry.getInstance().getAll().stream()
                .filter(s -> s.getName().equals(selectedSuiteName))
                .findFirst().orElse(null);
        if (suite == null) return;

        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new ArrayList<TestAction>();
        for (TestCase tc : suite.getTestCases()) actions.addAll(tc.getWhen());
        session.setRecordedActions(actions);
    }

    private void insertRow() {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new ArrayList<TestAction>();

        TestAction newAction = new TestAction();
        newAction.setType(TestAction.ActionType.THEN);
        newAction.setAction("screenshot");

        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= actions.size()) {
            actions.add(newAction);
        } else {
            actions.add(selectedRow, newAction);
        }
        session.setRecordedActions(actions);
    }

    private void deleteSelectedRows() {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) return;
        java.util.Iterator<TestAction> it = actions.iterator();
        while (it.hasNext()) {
            if (it.next().isSelected()) it.remove();
        }
        session.setRecordedActions(actions);
    }

    private void moveSelectedRows(int direction) {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) return;

        boolean changed = false;
        if (direction < 0) {
            for (int i = 1; i < actions.size(); i++) {
                if (actions.get(i).isSelected() && !actions.get(i - 1).isSelected()) {
                    Collections.swap(actions, i, i - 1);
                    changed = true;
                }
            }
        } else {
            for (int i = actions.size() - 2; i >= 0; i--) {
                if (actions.get(i).isSelected() && !actions.get(i + 1).isSelected()) {
                    Collections.swap(actions, i, i + 1);
                    changed = true;
                }
            }
        }
        if (changed) session.setRecordedActions(actions);
    }

    private List<TestCase> splitIntoTestCases(List<TestAction> actions, String baseName) {
        List<TestCase> testCases = new ArrayList<TestCase>();
        List<TestAction> current = new ArrayList<TestAction>();
        int counter = 1;

        for (TestAction action : actions) {
            current.add(action);
            if (action.getType() == TestAction.ActionType.GIVEN
                    || action.getType() == TestAction.ActionType.THEN) {
                if (!current.isEmpty()) {
                    testCases.add(new TestCase(baseName + "_" + (counter++), new ArrayList<TestAction>(current)));
                    current.clear();
                }
            }
        }
        if (!current.isEmpty()) {
            testCases.add(new TestCase(baseName + "_" + counter, new ArrayList<TestAction>(current)));
        }
        return testCases;
    }

    public void unregister() {
        if (session != null) {
            try {
                if (session.isRecording()) {
                    session.stop(); // Ensure stop recording before closing
                }
            } catch (Exception ignore) {
                // Swallow to not block tab closing
            }
            de.bund.zrb.service.RecorderCoordinator.getInstance().unregisterTab(this);
            session = null;
        }
    }

}
