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
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Swing tab for one user; delegate recording to service. */
public final class RecorderTab extends JPanel implements RecorderTabUi {

    private final RightDrawer rightDrawer;
    private final UserRegistry.User selectedUser;

    private final ActionTable actionTable = new ActionTable();
    private final JToggleButton recordToggle = new JToggleButton("\u2B24"); // red dot
    private final JComboBox<String> suiteDropdown = new JComboBox<String>();

    // --- Bottom drawer (meta monitor) ---
    private final JSplitPane centerSplit;
    /**
     * Container for all meta event components. Each event is represented as a Swing
     * component (e.g. {@link JLabel}). Unlike the previous plain text log, this
     * panel uses a vertical {@link BoxLayout} so that each entry occupies its own
     * row. Components added here should have a client property named
     * "eventName" of type {@link String} for filtering.
     */
    private final JPanel metaContainer = new JPanel();
    private final JPanel metaPanel = new JPanel(new BorderLayout(6, 6));
    /**
     * Button to toggle visibility of the entire meta drawer. This controls
     * whether the splitter shows or hides the bottom component. Independent of
     * event logging.
     */
    private final JToggleButton metaToggle = new JToggleButton("Meta-Events");
    private int lastDividerLocation = -1;

    /**
     * Previously a toggle button controlled starting and stopping of the event logging
     * service. This field has been removed in favour of dedicated commands
     * (see {@link de.bund.zrb.ui.commands.StartEventServiceCommand} and
     * {@link de.bund.zrb.ui.commands.StopEventServiceCommand}). The
     * event service is started and stopped by publishing
     * {@link de.bund.zrb.event.EventServiceControlRequestedEvent} via
     * {@link de.bund.zrb.event.ApplicationEventBus}.
     */
    // private final JToggleButton eventsToggle = new JToggleButton("Start Events");

    /**
     * Event service for this tab. A separate instance is created for each
     * RecorderTab and bound to its {@link RecordingSession}. It is started and
     * stopped via commands (see StartEventServiceCommand/StopEventServiceCommand)
     * and will record all raw events in the session as well as produce UI
     * components for enabled event types.
     */
    private EventService eventService;

    // Header-Leiste für Meta (wir hängen hier die Checkboxen rein)
    private final JPanel metaHeader = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
    // Panel für die Event-Checkboxen (damit man sie einfach erneuern könnte)
    private JPanel eventCheckboxPanel;

    // UserContext filter for this tab
    private String myUserContextId;

    private RecordingSession session;

    /**
     * Starts the event service by attaching it to the current recording target (context or page).
     * If neither a context nor a page is active, this method does nothing. The service
     * will begin consuming raw events and dispatching them to the UI.
     */
    @Override
    public void startEventService() {
        if (eventService == null) return;
        // Determine the current active target from the session. The recording session
        // exposes the active context and page once recording has started. Attach to
        // whichever is available. If recording hasn't started yet, there may be no
        // active context or page.
        com.microsoft.playwright.Page page = session.getActivePage();
        com.microsoft.playwright.BrowserContext context = session.getActiveContext();
        try {
            if (context != null) {
                eventService.start(context);
            } else if (page != null) {
                eventService.start(page);
            }
        } catch (Throwable ignore) {
            // swallow to avoid UI disruption
        }
    }

    /**
     * Stops the event service and clears any queued events. This will detach all raw
     * event listeners and stop dispatching to the UI. The component log remains
     * visible until cleared manually.
     */
    @Override
    public void stopEventService() {
        if (eventService == null) return;
        try {
            eventService.stop();
        } catch (Throwable ignore) {
            // ignore
        }
    }

    /**
     * Applies the current event filter settings by updating the visibility of all
     * components in the metaContainer. Each component must have a client property
     * "eventName" containing the BiDi event name. Components whose event type
     * is disabled in the session will be hidden, others will be shown.
     */
    private void applyEventFilter() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (java.awt.Component c : metaContainer.getComponents()) {
                    Object nameObj = null;
                    try {
                        if (c instanceof javax.swing.JComponent) {
                            nameObj = ((javax.swing.JComponent) c).getClientProperty("eventName");
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
            }
        });
    }

    public RecorderTab(RightDrawer rightDrawer, UserRegistry.User user) {
        super(new BorderLayout(8, 8));
        this.rightDrawer = rightDrawer;
        this.selectedUser = user;

        // Resolve userContextId for this tab (may be null initially, then register later)
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

        // Meta toggle button in toolbar
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

        // Center split: actions (top) + meta drawer (bottom)
        JScrollPane actionsScroll = new JScrollPane(actionTable);

        // Configure meta container for component-based logging
        metaContainer.setLayout(new BoxLayout(metaContainer, BoxLayout.Y_AXIS));

        // Meta header with title, clear button, events toggle and (later) checkboxes
        JLabel metaTitle = new JLabel("Events");
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusable(false);
        clearBtn.setToolTipText("Meta-Log leeren");
        clearBtn.addActionListener(e -> {
            // Remove all components from the container instead of clearing text
            metaContainer.removeAll();
            metaContainer.revalidate();
            metaContainer.repaint();
        });
        // Remove the events toggle from the meta header. Event logging
        // is now controlled exclusively via menu commands. Only the
        // title and clear button are added here. A horizontal strut
        // provides spacing before the dynamically added checkbox panel.
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

        // Register with coordinator (inject BrowserService via RightDrawer)
        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());

        // Create the event service bound to this tab and session. It will be started
        // via dedicated start/stop commands; initially it remains inactive until
        // the user triggers event logging.
        this.eventService = new EventService(this, session);

        // Checkboxen erst NACH dem Registrieren und auf dem EDT erzeugen
        SwingUtilities.invokeLater(this::buildEventCheckboxes);

        // Initialize drawer opened
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                setMetaDrawerVisible(true);
                if (lastDividerLocation <= 0) {
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

    @Override
    public void appendMeta(final String line) {
        // Convert plain text into a JLabel and delegate to component-based method
        if (line == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JLabel label = new JLabel(line);
                // Default event name is unknown for plain strings
                label.putClientProperty("eventName", null);
                appendMeta(label);
            }
        });
    }

    /**
     * Appends a Swing component to the metaContainer. Components should have a
     * client property "eventName" containing the raw event name if available;
     * otherwise the component will always be visible regardless of filter flags.
     * The component is added to the container and scrolled into view. Visibility
     * is determined by {@link #applyEventFilter()}.
     *
     * @param component the component to append; ignored if null
     */
    @Override
    public void appendMeta(javax.swing.JComponent component) {
        if (component == null) return;
        // Add the component to the container and mark its visibility based on current filter
        boolean visible = true;
        Object nameObj = component.getClientProperty("eventName");
        if (nameObj instanceof String) {
            visible = session.isEventEnabled((String) nameObj);
        }
        component.setVisible(visible);
        metaContainer.add(component);
        // Scroll to bottom to reveal the newest entry
        metaContainer.revalidate();
        metaContainer.repaint();
        // Attempt to ensure the new component is visible in the scroll pane
        try {
            Rectangle bounds = component.getBounds();
            metaContainer.scrollRectToVisible(bounds);
        } catch (Throwable ignore) { /* no-op */ }
    }

    /**
     * Appends an event component with an associated raw BiDi event name. The
     * component is annotated with the event name so that filtering via
     * checkboxes can hide or show it. If the name is null, the component
     * will always be visible.
     *
     * @param bidiEventName the BiDi event name, may be null
     * @param component     the component to append, may be null
     */
    @Override
    public void appendEvent(String bidiEventName, javax.swing.JComponent component) {
        if (component == null) return;
        component.putClientProperty("eventName", bidiEventName);
        appendMeta(component);
    }

    // ---------- RecorderListener (actions) ----------

    @Override
    public void onRecordingStateChanged(boolean recording) {
        setRecordingUiState(recording);
        SwingUtilities.invokeLater(this::buildEventCheckboxes);
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
            RecorderCoordinator.getInstance().unregisterTab(this);
            session = null;
        }
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

    /** Erzeugt/erneuert die Event-Checkboxen rechts vom "Clear"-Button und verkabelt sie mit der Session. */
    private void buildEventCheckboxes() {
        if (session == null) return;

        if (eventCheckboxPanel != null) {
            metaHeader.remove(eventCheckboxPanel);
            eventCheckboxPanel = null;
        }

        EnumMap<WDEventNames, Boolean> flags = session.getEventFlags();
        if (flags == null || flags.isEmpty()) {
            flags = de.bund.zrb.service.WDEventFlagPresets.recorderDefaults();
        }

        // WICHTIG: WrapLayout, damit die Checkboxes umbrechen
        eventCheckboxPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));

        for (WDEventNames ev : WDEventNames.values()) {
            if (!flags.containsKey(ev)) continue;
            boolean selected = Boolean.TRUE.equals(flags.get(ev));
            JCheckBox cb = new JCheckBox(prettyLabel(ev), selected);
            cb.setFocusable(false);
            String tt = ev.getDescription();
            cb.setToolTipText((tt == null || tt.trim().isEmpty()) ? ev.getName() : tt);
            cb.addActionListener(ae -> {
                // Update flag in session
                session.setEventFlag(ev, cb.isSelected());
                // Immediately update UI component visibility based on new filter
                applyEventFilter();
                // Propagate updated flags to EventService so it can adjust subscriptions if necessary
                if (eventService != null) {
                    eventService.updateFlags(session.getEventFlags());
                }
            });
            eventCheckboxPanel.add(cb);
        }

        metaHeader.add(eventCheckboxPanel);
        metaHeader.revalidate();
        metaHeader.repaint();
        // Ensure the current filter is applied after constructing checkboxes
        applyEventFilter();
    }

    /** Kleine Anzeige-Labels für die Events. */
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

    /**
     * Append a single formatted meta line. This legacy method now delegates to
     * {@link #appendMeta(String)} to support component-based logging. The caret
     * management previously used on a JTextArea is no longer needed because
     * scrolling is managed by {@link #appendMeta(javax.swing.JComponent)}.
     *
     * @param line the line of text to append
     */
    private void appendMetaLine(String line) {
        appendMeta(line);
    }

    /** Resolve the UserContext-ID for a username via mapping. */
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
