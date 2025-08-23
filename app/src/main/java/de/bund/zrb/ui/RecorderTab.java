package de.bund.zrb.ui;

import com.microsoft.playwright.BrowserContext;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.*;
import de.bund.zrb.websocket.WDEventNames;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/** Swing tab for one user; delegate recording to service. */
public final class RecorderTab extends JPanel implements RecorderTabUi {

    private final RightDrawer rightDrawer;
    private final UserRegistry.User selectedUser;

    private final ActionTable actionTable = new ActionTable();
    private final JToggleButton recordToggle = new JToggleButton("\u2B24"); // red dot
    private final JComboBox<String> suiteDropdown = new JComboBox<>();

    // --- Bottom drawer (meta monitor) ---
    private final JSplitPane centerSplit;
    /**
     * Container für alle Meta-Event-Komponenten (jede Zeile eigenes Swing-Component).
     * Jedes Component sollte "eventName" (String) als ClientProperty tragen,
     * damit die Checkbox-Filter wirken können.
     */
    private final JPanel metaContainer = new JPanel();
    private final JPanel metaPanel = new JPanel(new BorderLayout(6, 6));

    /** Schaltet die Sichtbarkeit des Meta-Drawers (Splitter unten). */
    private final JToggleButton metaToggle = new JToggleButton("Meta-Events");
    private int lastDividerLocation = -1;

    /** Der alte EventService wurde ersetzt – dieser Feldname bleibt zur Kompatibilität.
     *  Es handelt sich um den leichten Shim, der Events aus dem Dispatcher abonniert,
     *  in die Session schreibt und UI-Labels erzeugt. */
    private RecorderEventController recorderEventController;

    // Header für Meta (rechts vom "Clear" kommen die Checkboxen rein)
    private final JPanel metaHeader = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
    private JPanel eventCheckboxPanel;

    // UserContext-Filter für diesen Tab (optional, kann null sein)
    private String myUserContextId;

    private RecordingSession session;

    public RecorderTab(RightDrawer rightDrawer, UserRegistry.User user) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.selectedUser = user;

        // UserContext-ID (falls schon vorhanden)
        this.myUserContextId = resolveUserContextId(user.getUsername());

        // Suites füllen
        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            suiteDropdown.addItem(suite.getName());
        }

        // Record-Button
        recordToggle.setBackground(Color.RED);
        recordToggle.setFocusPainted(false);
        recordToggle.setToolTipText("Start Recording");
        recordToggle.addActionListener(e ->
                RecorderCoordinator.getInstance().toggleForUser(selectedUser.getUsername())
        );

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

        // Center: Actions (oben) + Meta-Drawer (unten)
        JScrollPane actionsScroll = new JScrollPane(actionTable);

        metaContainer.setLayout(new BoxLayout(metaContainer, BoxLayout.Y_AXIS));

        JLabel metaTitle = new JLabel("Events");
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusable(false);
        clearBtn.setToolTipText("Meta-Log leeren");
        clearBtn.addActionListener(e -> {
            metaContainer.removeAll();
            metaContainer.revalidate();
            metaContainer.repaint();
        });

        metaHeader.add(metaTitle);
        metaHeader.add(clearBtn);
        metaHeader.add(Box.createHorizontalStrut(10));

        metaPanel.add(metaHeader, BorderLayout.NORTH);
        metaPanel.add(new JScrollPane(metaContainer), BorderLayout.CENTER);

        centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, actionsScroll, metaPanel);
        centerSplit.setResizeWeight(0.8);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setDividerSize(10);
        add(centerSplit, BorderLayout.CENTER);

        // Tab registrieren (liefert die RecordingSession)
        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());

        // Leichter Event-Bridge-Service (Shim), der die Dispatcher-Events konsumiert
        this.recorderEventController = new RecorderEventController(this, session);
        if (myUserContextId != null && !myUserContextId.isEmpty()) {
            this.recorderEventController.setUserContextFilter(myUserContextId);
        }

        // Checkboxen nach Registrierung erstellen
        SwingUtilities.invokeLater(this::buildEventCheckboxes);

        // Drawer initial sichtbar
        SwingUtilities.invokeLater(() -> {
            setMetaDrawerVisible(true);
            if (lastDividerLocation <= 0) {
                centerSplit.setDividerLocation(0.75);
            }
        });
    }

    // ---------- RecorderTabUi ----------

    @Override
    public String getUsername() {
        return selectedUser.getUsername();
    }

    @Override
    public boolean isVisibleActive() {
        return isShowing() && isVisible();
    }

    @Override
    public void setActions(final List<TestAction> actions) {
        SwingUtilities.invokeLater(() -> actionTable.setActions(actions));
    }

    @Override
    public void setRecordingUiState(final boolean recording) {
        SwingUtilities.invokeLater(() -> {
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
        });
    }

    @Override
    public void appendMeta(final String line) {
        if (line == null) return;
        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel(line);
            label.putClientProperty("eventName", null);
            appendMeta(label);
        });
    }

    @Override
    public void appendMeta(final JComponent component) {
        if (component == null) return;

        boolean visible = true;
        Object nameObj = component.getClientProperty("eventName");
        if (nameObj instanceof String) {
            visible = session.isEventEnabled((String) nameObj);
        }
        component.setVisible(visible);
        metaContainer.add(component);

        metaContainer.revalidate();
        metaContainer.repaint();
        try {
            Rectangle bounds = component.getBounds();
            metaContainer.scrollRectToVisible(bounds);
        } catch (Throwable ignore) { /* no-op */ }
    }

    @Override
    public void appendEvent(String bidiEventName, JComponent component) {
        if (component == null) return;
        component.putClientProperty("eventName", bidiEventName);
        appendMeta(component);
    }

    // ---------- RecorderListener (Actions) ----------

    @Override
    public void onRecordingStateChanged(boolean recording) {
        setRecordingUiState(recording);
        SwingUtilities.invokeLater(this::buildEventCheckboxes);
    }

    @Override
    public void onRecorderUpdated(final List<TestAction> actions) {
        setActions(actions);
    }

    // ---------- Event-Lifecycle vom Tab aus ----------

    /** Startet den leichten Event-Bridge-Service (Shim). */
    @Override
    public void startEventService() {
        if (recorderEventController == null) return;

        com.microsoft.playwright.Page page = session.getActivePage();
        com.microsoft.playwright.BrowserContext context = session.getActiveContext();
        try {
            if (context != null) {
                recorderEventController.start(context);
            } else if (page != null) {
                recorderEventController.start(page);
            }
        } catch (Throwable t) {
            System.out.println("::: Event Worker failed to start :::");
            t.printStackTrace();
        }
    }

    /** Stoppt den Event-Bridge-Service. */
    @Override
    public void stopEventService() {
        if (recorderEventController == null) return;
        try {
            recorderEventController.stop();
        } catch (Throwable ignore) {
            // ignore
        }
    }

    // ---------- UI-Operationen (Delegation zur Session) ----------

    private void saveAsNewTestSuite() {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new ArrayList<>();

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
        if (actions == null) actions = new ArrayList<>();
        for (TestCase tc : suite.getTestCases()) actions.addAll(tc.getWhen());
        session.setRecordedActions(actions);
    }

    private void insertRow() {
        List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new ArrayList<>();

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
        List<TestCase> testCases = new ArrayList<>();
        List<TestAction> current = new ArrayList<>();
        int counter = 1;

        for (TestAction action : actions) {
            current.add(action);
            if (action.getType() == TestAction.ActionType.GIVEN
                    || action.getType() == TestAction.ActionType.THEN) {
                if (!current.isEmpty()) {
                    testCases.add(new TestCase(baseName + "_" + (counter++), new ArrayList<>(current)));
                    current.clear();
                }
            }
        }
        if (!current.isEmpty()) {
            testCases.add(new TestCase(baseName + "_" + counter, new ArrayList<>(current)));
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
            RecorderCoordinator.getInstance().unregisterTab(this);
            session = null;
        }
    }

    // ---------- Private Helpers ----------

    /** Meta-Drawer ein-/ausblenden, Divider-Position merken. */
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

    /** Erzeugt/erneuert die Event-Checkboxen rechts vom "Clear"-Button und verdrahtet sie. */
    private void buildEventCheckboxes() {
        if (session == null) return;

        if (eventCheckboxPanel != null) {
            metaHeader.remove(eventCheckboxPanel);
            eventCheckboxPanel = null;
        }

        EnumMap<WDEventNames, Boolean> flags = session.getEventFlags();
        if (flags == null || flags.isEmpty()) {
            flags = WDEventFlagPresets.recorderDefaults();
        }

        eventCheckboxPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));

        for (WDEventNames ev : WDEventNames.values()) {
            if (!flags.containsKey(ev)) continue;
            boolean selected = Boolean.TRUE.equals(flags.get(ev));
            JCheckBox cb = new JCheckBox(prettyLabel(ev), selected);
            cb.setFocusable(false);
            String tt = ev.getDescription();
            cb.setToolTipText((tt == null || tt.trim().isEmpty()) ? ev.getName() : tt);
            cb.addActionListener(ae -> {
                session.setEventFlag(ev, cb.isSelected());
                applyEventFilter();
                if (recorderEventController != null) {
                    recorderEventController.updateFlags(session.getEventFlags());
                }
            });
            eventCheckboxPanel.add(cb);
        }

        metaHeader.add(eventCheckboxPanel);
        metaHeader.revalidate();
        metaHeader.repaint();
        applyEventFilter();
    }

    /** Schöne Kurzlabels für Checkboxen. */
    private static String prettyLabel(WDEventNames ev) {
        switch (ev) {
            case BEFORE_REQUEST_SENT: return "request";
            case RESPONSE_STARTED:    return "response";
            case RESPONSE_COMPLETED:  return "done";
            case FETCH_ERROR:         return "error";
            case DOM_CONTENT_LOADED:  return "dom";
            case LOAD:                return "load";
            case ENTRY_ADDED:         return "console";
            case CONTEXT_CREATED:     return "ctx+";
            case CONTEXT_DESTROYED:   return "ctx-";
            case FRAGMENT_NAVIGATED:  return "hash";
            case NAVIGATION_STARTED:  return "nav";
            default:
                return ev.name().toLowerCase().replace('_', ' ');
        }
    }

    /** Re-appliziert die Sichtbarkeit in der Meta-Liste gemäß Checkbox-Flags. */
    private void applyEventFilter() {
        SwingUtilities.invokeLater(() -> {
            for (Component c : metaContainer.getComponents()) {
                Object nameObj = null;
                try {
                    if (c instanceof JComponent) {
                        nameObj = ((JComponent) c).getClientProperty("eventName");
                    }
                } catch (Throwable ignore) {}
                boolean visible = true;
                if (nameObj instanceof String) {
                    visible = session.isEventEnabled((String) nameObj);
                }
                c.setVisible(visible);
            }
            metaContainer.revalidate();
            metaContainer.repaint();
        });
    }

    /** UserContext-ID für den Username auflösen (falls schon verfügbar). */
    private static String resolveUserContextId(String username) {
        BrowserContext ctx = UserContextMappingService.getInstance().getContextForUser(username);
        if (ctx instanceof de.bund.zrb.UserContextImpl) {
            try {
                return ((de.bund.zrb.UserContextImpl) ctx).getUserContext().value();
            } catch (Throwable ignore) { }
        }
        return null;
    }
}
