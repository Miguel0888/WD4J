package de.bund.zrb.ui;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.PageImpl;
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

class RecorderSession extends JPanel implements RecorderListener {

    private final RightDrawer rightDrawer;
    private final ActionTable actionTable;
    private final JToggleButton recordToggle;
    private final JComboBox<String> suiteDropdown;

    private RecorderService recorderService;
    private UserRegistry.User selectedUser;

    private Page activePage;
    private BrowserContext activeContext;

    RecorderSession(RightDrawer rightDrawer, UserRegistry.User user) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.selectedUser = user;
        this.actionTable = new ActionTable();

        this.recordToggle = new JToggleButton("\u2B24");
        recordToggle.setBackground(Color.RED);
        recordToggle.setFocusPainted(false);
        recordToggle.setToolTipText("Start Recording");
        recordToggle.addActionListener(e -> toggleRecording());

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

        suiteDropdown = new JComboBox<>();
        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            suiteDropdown.addItem(suite.getName());
        }

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
    }

    private void toggleRecording() {
        boolean shouldStart = recordToggle.isSelected();
        BrowserService browserService = rightDrawer.getBrowserService();

        if (shouldStart) {
            if (selectedUser == null) {
                JOptionPane.showMessageDialog(this, "Kein Benutzer zugewiesen!", "Fehler", JOptionPane.ERROR_MESSAGE);
                recordToggle.setSelected(false);
                return;
            }

            String username = selectedUser.getUsername();
            browserService.createUserContext(selectedUser);

            this.activePage = browserService.createNewTab(username);
            this.activeContext = activePage.context();

            this.recorderService = RecorderService.getInstance(activePage);

            UserContextMappingService.getInstance().bindUserToContext(selectedUser.getUsername(), activeContext, selectedUser);

            browserService.getBrowser().getRecordingEventRouter().addPageListener(activePage, recorderService);
            recorderService.addListener(this);

            System.out.println("✅ Recorder gestartet: "
                    + "User=" + username
                    + ", Context=" + activeContext
                    + ", Page=" + activePage
            );

            recordToggle.setText("\u23F8");
            recordToggle.setToolTipText("Stop Recording");
            recordToggle.setBackground(Color.GRAY);

        } else {
            unregister();
            recordToggle.setText("\u2B24");
            recordToggle.setToolTipText("Start Recording");
            recordToggle.setBackground(Color.RED);
        }
    }

    public void unregister() {
        if (recorderService != null && activePage != null) {
            recorderService.removeListener(this);
            rightDrawer.getBrowserService().getBrowser().getRecordingEventRouter().removePageListener(activePage, recorderService);
            RecorderService.remove(activePage);

            activePage = null;
            activeContext = null;
        }
    }

    private void saveAsNewTestSuite() {
        if (recorderService == null) {
            System.out.println("⚠️ Kein aktiver RecorderService!");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Name der Testsuite eingeben:", "Neue Testsuite speichern", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            System.out.println("⚠️ Kein Name eingegeben.");
            return;
        }

        List<TestAction> actions = actionTable.getActions();
        List<TestCase> testCases = splitIntoTestCases(actions, name);

        TestSuite suite = new TestSuite(name, testCases);
        TestRegistry.getInstance().addSuite(suite);
        TestRegistry.getInstance().save();

        System.out.println("✅ Neue Testsuite gespeichert: " + name);

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(name));

        recorderService.clearRecordedEvents();
    }

    private void exportToSuite() {
        if (recorderService == null) return;

        String selectedSuiteName = (String) suiteDropdown.getSelectedItem();
        if (selectedSuiteName == null) return;

        TestSuite suite = TestRegistry.getInstance().getAll().stream()
                .filter(s -> s.getName().equals(selectedSuiteName))
                .findFirst()
                .orElse(null);

        if (suite == null) return;

        List<TestAction> actions = actionTable.getActions();
        List<TestCase> newCases = splitIntoTestCases(actions, selectedSuiteName + "_Part");

        suite.getTestCases().addAll(newCases);
        TestRegistry.getInstance().save();

        System.out.println("✅ Export in bestehende Testsuite: " + selectedSuiteName);

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(selectedSuiteName));

        recorderService.clearRecordedEvents();
    }

    private void importSuite() {
        String selectedSuiteName = (String) suiteDropdown.getSelectedItem();
        if (selectedSuiteName == null) return;

        TestSuite suite = TestRegistry.getInstance().getAll().stream()
                .filter(s -> s.getName().equals(selectedSuiteName))
                .findFirst()
                .orElse(null);

        if (suite == null) return;

        List<TestAction> actions = recorderService.getAllTestActionsForDrawer();
        for (TestCase tc : suite.getTestCases()) {
            actions.addAll(tc.getWhen());
        }

        recorderService.setRecordedActions(actions);
    }

    private void insertRow() {
        if (recorderService == null) return;

        TestAction newAction = new TestAction();
        newAction.setType(TestAction.ActionType.THEN);
        newAction.setAction("screenshot");

        int selectedRow = actionTable.getSelectedRow();
        List<TestAction> actions = recorderService.getAllTestActionsForDrawer();

        if (selectedRow < 0 || selectedRow >= actions.size()) {
            actions.add(newAction);
        } else {
            actions.add(selectedRow, newAction);
        }

        recorderService.setRecordedActions(actions);
    }

    private void deleteSelectedRows() {
        if (recorderService == null) return;

        List<TestAction> actions = recorderService.getAllTestActionsForDrawer();
        actions.removeIf(TestAction::isSelected);
        recorderService.setRecordedActions(actions);
    }

    private void moveSelectedRows(int direction) {
        if (recorderService == null) return;

        List<TestAction> actions = recorderService.getAllTestActionsForDrawer();
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

        if (changed) {
            recorderService.setRecordedActions(actions);
        }
    }

    private List<TestCase> splitIntoTestCases(List<TestAction> actions, String baseName) {
        List<TestCase> testCases = new ArrayList<>();
        List<TestAction> current = new ArrayList<>();
        int counter = 1;

        for (TestAction action : actions) {
            current.add(action);

            if (action.getType() == TestAction.ActionType.GIVEN ||
                    action.getType() == TestAction.ActionType.THEN) {
                if (!current.isEmpty()) {
                    testCases.add(new TestCase(baseName + "_" + counter++, new ArrayList<>(current)));
                    current.clear();
                }
            }
        }

        if (!current.isEmpty()) {
            testCases.add(new TestCase(baseName + "_" + counter, new ArrayList<>(current)));
        }

        return testCases;
    }

    @Override
    public void onRecorderUpdated(List<TestAction> actions) {
        SwingUtilities.invokeLater(() -> actionTable.setActions(actions));
    }
}
