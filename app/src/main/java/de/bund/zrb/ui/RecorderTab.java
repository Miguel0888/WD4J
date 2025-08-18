package de.bund.zrb.ui;

import com.microsoft.playwright.BrowserContext;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.*;
import de.bund.zrb.meta.MetaEvent;
import de.bund.zrb.meta.MetaEventFormatter;
import de.bund.zrb.meta.MetaEventListener;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Swing tab for one user; delegate recording to service. */
public final class RecorderTab extends JPanel implements RecorderTabUi {

    private final RightDrawer rightDrawer;
    private final UserRegistry.User selectedUser;

    private final ActionTable actionTable = new ActionTable();
    private final JToggleButton recordToggle = new JToggleButton("\u2B24"); // red dot
    private final JComboBox<String> suiteDropdown = new JComboBox<String>();

    // --- New: bottom drawer (meta monitor) ---
    private final JSplitPane centerSplit;
    private final JTextArea metaArea = new JTextArea();
    private final JPanel metaPanel = new JPanel(new BorderLayout(6, 6));
    private final JToggleButton metaToggle = new JToggleButton("Meta-Events");
    private int lastDividerLocation = -1;

    private final MetaEventService metaService = MetaEventServiceImpl.getInstance();

    // UserContext-Filter für Meta-Events dieses Tabs
    private String myUserContextId;

    private final MetaEventListener metaListener = new MetaEventListener() {
        public void onMetaEvent(final MetaEvent event) {
            Map<String,String> d = event.getDetails();
            if(myUserContextId == null) {
                myUserContextId = resolveUserContextId(selectedUser.getUsername());
            }
            // fremde Events ignorieren
            if (myUserContextId != null && d != null) {
                String uc = d.get("userContextId");
                if (uc != null && uc.equals(myUserContextId)) {
                    SwingUtilities.invokeLater(() -> appendMetaLine(MetaEventFormatter.format(event)));
                };
            }
        }
    };

    private RecordingSession session;

    public RecorderTab(RightDrawer rightDrawer, UserRegistry.User user) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.selectedUser = user;

        // UserContext-ID für diesen Tab ermitteln (falls noch kein Context existiert -> null, dann wird nicht gefiltert)
        this.myUserContextId = resolveUserContextId(user.getUsername());

        // Fill suites
        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            suiteDropdown.addItem(suite.getName());
        }

        // Wire record button
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

        // --- New: meta toggle button in toolbar ---
        metaToggle.setSelected(true);
        metaToggle.setToolTipText("Meta-Drawer ein-/ausblenden");
        metaToggle.addActionListener(e -> setMetaDrawerVisible(metaToggle.isSelected()));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(recordToggle);
        topPanel.add(saveButton);
        topPanel.add(addButton);
        topPanel.add(deleteButton);
        topPanel.add(upButton);
        topPanel.add(downButton);

        topPanel.add(Box.createHorizontalStrut(24));
        topPanel.add(suiteDropdown);
        topPanel.add(importButton);
        topPanel.add(exportButton);

        topPanel.add(Box.createHorizontalStrut(24));
        topPanel.add(metaToggle);

        add(topPanel, BorderLayout.NORTH);

        // --- Center split: actions (top) + meta drawer (bottom) ---
        JScrollPane actionsScroll = new JScrollPane(actionTable);

        // Configure meta area
        metaArea.setEditable(false);
        metaArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        metaArea.setLineWrap(false);
        DefaultCaret caret = (DefaultCaret) metaArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Meta header with clear button
        JPanel metaHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JLabel metaTitle = new JLabel("Meta-Events (experimentell)");
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusable(false);
        clearBtn.setToolTipText("Meta-Log leeren");
        clearBtn.addActionListener(e -> metaArea.setText(""));
        metaHeader.add(metaTitle);
        metaHeader.add(clearBtn);

        metaPanel.add(metaHeader, BorderLayout.NORTH);
        metaPanel.add(new JScrollPane(metaArea), BorderLayout.CENTER);

        centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, actionsScroll, metaPanel);
        centerSplit.setResizeWeight(0.8);           // Prefer space for actions
        centerSplit.setOneTouchExpandable(true);    // Show collapse/expand arrows
        centerSplit.setDividerSize(10);             // Easier to grab
        add(centerSplit, BorderLayout.CENTER);

        // Register with coordinator (inject BrowserService via RightDrawer)
        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());

        // Subscribe to meta events (experimental)
        metaService.addListener(metaListener);

        // Initialize drawer opened
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setMetaDrawerVisible(true);
                if (lastDividerLocation <= 0) {
                    // Set a reasonable initial height for the drawer
                    centerSplit.setDividerLocation(0.75);
                }
            }
        });
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

    @Override
    public void onRecordingStateChanged(boolean recording) {
        setRecordingUiState(recording);
        if (recording) {
            // Optionally clear meta log when starting fresh
            // metaArea.setText("");
        }
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
        // Remove meta listener to avoid leaks
        metaService.removeListener(metaListener);
    }

    // ---------- Private helpers ----------

    /** Show or hide the meta drawer by adjusting split visibility and divider. */
    private void setMetaDrawerVisible(boolean visible) {
        Component bottom = centerSplit.getBottomComponent();
        if (visible) {
            bottom.setVisible(true);
            if (lastDividerLocation > 0) {
                centerSplit.setDividerLocation(lastDividerLocation);
            } else {
                centerSplit.setDividerLocation(0.75);
            }
        } else {
            lastDividerLocation = centerSplit.getDividerLocation();
            bottom.setVisible(false);
            centerSplit.setDividerLocation(1.0);
        }
        centerSplit.revalidate();
        centerSplit.repaint();
    }

    /** Append a single formatted meta line and keep caret at bottom. */
    private void appendMetaLine(String line) {
        if (line == null) return;
        if (metaArea.getText().length() == 0) {
            metaArea.setText(line);
        } else {
            metaArea.append("\n" + line);
        }
    }

    /** Ermittelt die UserContext-ID für einen Benutzer über das Mapping. */
    private static String resolveUserContextId(String username) {
        BrowserContext ctx = UserContextMappingService.getInstance().getContextForUser(username);
        if (ctx instanceof de.bund.zrb.UserContextImpl) {
            try {
                return ((de.bund.zrb.UserContextImpl) ctx).getUserContext().value();
            } catch (Throwable ignore) { }
        }
        return null; // noch kein Context vorhanden -> keine Filterung
    }
}
