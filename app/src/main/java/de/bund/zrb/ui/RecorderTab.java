package de.bund.zrb.ui;

import com.microsoft.playwright.BrowserContext;
import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.*;
import de.bund.zrb.ui.debug.RecorderEventController;

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
    private final JComboBox<String> suiteDropdown = new JComboBox<>();

    /** Der alte EventService wurde ersetzt – dieser Feldname bleibt zur Kompatibilität.
     *  Es handelt sich um den leichten Shim, der Events aus dem Dispatcher abonniert,
     *  in die Session schreibt und UI-Labels erzeugt. */
    private RecorderEventController recorderEventController;

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

        // --- Topbar: links Bedienelemente, rechts Hilfe ---
        JPanel topBar = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));

        left.add(saveButton);
        left.add(addButton);
        left.add(deleteButton);
        left.add(upButton);
        left.add(downButton);

        left.add(Box.createHorizontalStrut(24));
        left.add(suiteDropdown);
        left.add(importButton);
        left.add(exportButton);

        topBar.add(left, BorderLayout.CENTER);

        // Right-aligned help button (blue ℹ)
        topBar.add(buildHelpCornerButton(), BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Center: nur noch die Actions-Tabelle (Meta/Events-Drawer entfernt)
        JScrollPane actionsScroll = new JScrollPane(actionTable);
        add(actionsScroll, BorderLayout.CENTER);

        // Tab registrieren (liefert die RecordingSession)
        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());

        // Leichter Event-Bridge-Service (Shim), der die Dispatcher-Events konsumiert
        this.recorderEventController = new RecorderEventController(this, session);
        if (myUserContextId != null && !myUserContextId.isEmpty()) {
            this.recorderEventController.setUserContextFilter(myUserContextId);
        }
    }

    // Build a small blue info button for the top-right corner
    private JButton buildHelpCornerButton() {
        JButton b = new JButton("ℹ");
        b.setFocusable(false);
        b.setToolTipText("Hilfe zum Recorder anzeigen");
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x1E88E5)); // Blue
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1565C0)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        b.addActionListener(e -> {
            String html = buildRecorderHelpHtml();
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(wrapAsHtmlPane(html)),
                    "Hilfe – Recorder",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        return b;
    }

    // Build simple HTML help for the recorder
    private String buildRecorderHelpHtml() {
        StringBuilder sb = new StringBuilder(800);
        sb.append("<html><body style='font-family:sans-serif; padding:8px;'>");
        sb.append("<h3 style='margin-top:0'>Recorder – Kurzüberblick</h3>");
        sb.append("<ul>");
        sb.append("<li><b>+</b> fügt eine neue Zeile ein.</li>");
        sb.append("<li><b>🗑</b> löscht markierte Zeilen. Sind keine markiert, wird angeboten, alle zu löschen.</li>");
        sb.append("<li><b>▲/▼</b> verschiebt markierte Zeilen.</li>");
        sb.append("<li><b>⤵</b> importiert die ausgewählte Suite in den Recorder.</li>");
        sb.append("<li><b>⤴</b> exportiert die Recorder-Inhalte in die gewählte Suite.</li>");
        sb.append("<li><b>Neue Testsuite speichern</b> legt aus den Recorder-Aktionen eine Suite an.</li>");
        sb.append("</ul>");
        sb.append("<p style='color:#555'>Tipp: Markierungen erfolgen über die Auswahl-Flags der Actions; die Tabelle spiegelt das im Modell.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    // Create a non-editable HTML pane for dialogs
    private JEditorPane wrapAsHtmlPane(String html) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        pane.setCaretPosition(0);
        return pane;
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
        SwingUtilities.invokeLater(() ->
                rightDrawer.setTabRecording(selectedUser, recording)
        );
    }

    @Override
    public void appendMeta(final String line) {
        // Meta-Ansicht wurde in ein separates Fenster ausgelagert – hier kein Inline-Log mehr.
        // no-op
    }

    @Override
    public void appendMeta(final JComponent component) {
        // Meta-Ansicht wurde in ein separates Fenster ausgelagert – hier kein Inline-Log mehr.
        // no-op
    }

    @Override
    public void appendEvent(String bidiEventName, JComponent component) {
        // Meta-Ansicht wurde in ein separates Fenster ausgelagert – hier kein Inline-Log mehr.
        // no-op
    }

    // ---------- RecorderListener (Actions) ----------

    @Override
    public void onRecordingStateChanged(boolean recording) {
        setRecordingUiState(recording);
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
        if (actions == null) actions = new ArrayList<TestAction>();

        String name = JOptionPane.showInputDialog(this, "Name der Testsuite eingeben:",
                "Neue Testsuite speichern", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        List<TestCase> testCases = splitIntoTestCases(actions, name);
        TestSuite suite = new TestSuite(name, testCases);

        // Users hochpromoten (Action -> Case -> Suite)
        de.bund.zrb.service.UserPromotionUtil.promoteSuiteUsers(suite);

        RecorderService.wireSuite(suite);

        TestRegistry.getInstance().addSuite(suite);
        TestRegistry.getInstance().save();

        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(name));
        session.clearRecordedEvents();
    }

    // RecorderTab.java – Methode komplett ersetzen
    private void exportToSuite() {
        String selectedSuiteName = (String) suiteDropdown.getSelectedItem();
        if (selectedSuiteName == null) return;

        // Suite ohne Streams holen
        TestSuite suite = null;
        java.util.List<TestSuite> all = TestRegistry.getInstance().getAll();
        for (int i = 0; i < all.size(); i++) {
            TestSuite s = all.get(i);
            if (s != null && selectedSuiteName.equals(s.getName())) {
                suite = s;
                break;
            }
        }
        if (suite == null) return;

        // Aktionen aus dem Recorder holen und in Cases splitten
        java.util.List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null) actions = new java.util.ArrayList<TestAction>();
        java.util.List<TestCase> newCases = splitIntoTestCases(actions, selectedSuiteName + "_Part");

        // 1) User von Actions auf Case-Ebene heben
        for (int i = 0; i < newCases.size(); i++) {
            de.bund.zrb.service.UserPromotionUtil.promoteCaseUser(newCases.get(i));
        }

        // 2) Neue Cases zur Suite hinzufügen
        suite.getTestCases().addAll(newCases);

        // 3) Prüfen, ob jetzt die gesamte Suite denselben Case-User hat -> ggf. Suite-User setzen
        de.bund.zrb.service.UserPromotionUtil.promoteSuiteUsers(suite);

        // 4) IDs und Parent-IDs SOFORT verdrahten (suite.id, case.id, action.id + parentId)
        //    ⇒ damit erscheinen parentId der Actions direkt im JSON nach dem Save
        de.bund.zrb.service.RecorderService.wireSuite(suite);

        // 5) Persistieren und UI aufräumen
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
        // Set default action to "click"
        newAction.setAction("click");

        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= actions.size()) {
            actions.add(newAction);
        } else {
            actions.add(selectedRow, newAction);
        }
        session.setRecordedActions(actions);
    }

    // Delete selected rows; if none selected, ask to delete all
    private void deleteSelectedRows() {
        java.util.List<TestAction> actions = session.getAllTestActionsForDrawer();
        if (actions == null || actions.isEmpty()) {
            return;
        }

        // Check selection state
        boolean anySelected = false;
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).isSelected()) {
                anySelected = true;
                break;
            }
        }

        if (!anySelected) {
            // Ask if all rows should be deleted
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Es ist keine Zeile markiert.\nSollen wirklich alle Zeilen gelöscht werden?",
                    "Alle Zeilen löschen?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return; // Do nothing on "No"
            }
            // Clear all actions
            actions.clear();
            session.setRecordedActions(actions);
            return;
        }

        // Remove only selected actions
        java.util.Iterator<TestAction> it = actions.iterator();
        while (it.hasNext()) {
            if (it.next().isSelected()) {
                it.remove();
            }
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
