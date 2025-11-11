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
    private final JComboBox<String> suiteSelector = new JComboBox<>();
    private final JComboBox<String> caseDropdown = new JComboBox<>();
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

        // Suite-Auswahl vorbereiten (mit "neu" als Default)
        refreshSuiteSelector();
        suiteSelector.addActionListener(e -> refreshCaseDropdown());
        suiteSelector.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { refreshSuiteSelector(); }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });

        caseDropdown.setPrototypeDisplayValue("(Case)");
        caseDropdown.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { refreshCaseDropdown(); }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });

        JButton saveButton = new JButton("Speichern");
        saveButton.setToolTipText("Speichert: Suite <neu> → neue Suite; sonst als einzelner Case in ausgewählter Suite");
        saveButton.addActionListener(e -> onSaveClicked());

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
        importButton.setToolTipText("Ausgewählten Case importieren und Recorder füllen");
        importButton.addActionListener(e -> importCase());

        // --- Topbar ---
        JPanel topBar = new JPanel(new BorderLayout());

        // WEST: Suite + Case + Import (Info-Button entfernt)
        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        west.add(suiteSelector);
        west.add(caseDropdown);
        west.add(importButton);

        // CENTER: Steuer-Buttons mittig
        JPanel middle = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        middle.add(addButton);
        middle.add(deleteButton);
        middle.add(upButton);
        middle.add(downButton);

        // EAST: Speichern ganz rechts
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        east.add(saveButton);

        topBar.add(west, BorderLayout.WEST);
        topBar.add(middle, BorderLayout.CENTER);
        topBar.add(east, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Center: Tabelle
        JScrollPane actionsScroll = new JScrollPane(actionTable);
        add(actionsScroll, BorderLayout.CENTER);

        this.session = RecorderCoordinator.getInstance()
                .registerTab(selectedUser.getUsername(), this, rightDrawer.getBrowserService());

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
        sb.append("<li><b>⤵</b> importiert den ausgewählten Case in den Recorder.</li>");
        sb.append("<li><b>Speichern</b>: Suite &lt;neu&gt; → neue Testsuite; sonst die Recorder-Aktionen als einzelnen Case in der Suite speichern.</li>");
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
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null) actions = new ArrayList<>();

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

    // Speichern-Button: je nach Suite-Auswahl neue Suite oder einzelner Case in vorhandener Suite
    private void onSaveClicked() {
        String suiteName = (String) suiteSelector.getSelectedItem();
        if (suiteName == null || suiteName.trim().isEmpty() || "<neu>".equalsIgnoreCase(suiteName.trim())) {
            saveAsNewTestSuite();
            refreshSuiteSelector();
            refreshCaseDropdown();
            return;
        }
        saveCaseToSuite(suiteName);
        refreshCaseDropdown();
    }

    private void saveCaseToSuite(String suiteName) {
        TestSuite suite = findSuiteByName(suiteName);
        if (suite == null) {
            JOptionPane.showMessageDialog(this, "Suite nicht gefunden: " + suiteName, "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null) actions = new ArrayList<>();

        String selectedCase = (String) caseDropdown.getSelectedItem();
        if (selectedCase != null && !selectedCase.trim().isEmpty() && !"<neu>".equalsIgnoreCase(selectedCase.trim())) {
            // Überschreiben-Logik für bestehenden Case
            TestCase existing = findCaseByName(suite, selectedCase.trim());
            if (existing == null) {
                JOptionPane.showMessageDialog(this, "Case nicht gefunden: " + selectedCase, "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int ovw = JOptionPane.showConfirmDialog(this,
                    "Case '" + selectedCase + "' überschreiben?",
                    "Überschreiben?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (ovw != JOptionPane.YES_OPTION) return;

            existing.getWhen().clear();
            existing.getWhen().addAll(actions);
            de.bund.zrb.service.UserPromotionUtil.promoteCaseUser(existing);
            RecorderService.wireSuite(suite);
            TestRegistry.getInstance().save();
            ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suiteName));
            return;
        }

        // Neuer Case: Namen abfragen
        String defaultName = suiteName + "_Case";
        String caseName = JOptionPane.showInputDialog(this, "Name des TestCase:", defaultName);
        if (caseName == null || caseName.trim().isEmpty()) return;
        caseName = caseName.trim();

        TestCase existing = findCaseByName(suite, caseName);
        if (existing != null) {
            int ovw = JOptionPane.showConfirmDialog(this,
                    "Ein Case namens '" + caseName + "' existiert bereits. Überschreiben?",
                    "Überschreiben?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (ovw != JOptionPane.YES_OPTION) return;
            existing.getWhen().clear();
            existing.getWhen().addAll(actions);
            de.bund.zrb.service.UserPromotionUtil.promoteCaseUser(existing);
        } else {
            TestCase newCase = new TestCase(caseName, new ArrayList<>(actions));
            de.bund.zrb.service.UserPromotionUtil.promoteCaseUser(newCase);
            suite.getTestCases().add(newCase);
        }

        RecorderService.wireSuite(suite);
        TestRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new TestSuiteSavedEvent(suiteName));
    }

    private void importCase() {
        String suiteName = (String) suiteSelector.getSelectedItem();
        if (suiteName == null || suiteName.trim().isEmpty() || "<neu>".equalsIgnoreCase(suiteName.trim())) {
            JOptionPane.showMessageDialog(this, "Bitte zuerst eine Suite auswählen.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String caseName = (String) caseDropdown.getSelectedItem();
        if (caseName == null || caseName.trim().isEmpty() || "<neu>".equalsIgnoreCase(caseName.trim())) {
            JOptionPane.showMessageDialog(this, "Bitte einen existierenden Case auswählen.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        TestSuite suite = findSuiteByName(suiteName);
        if (suite == null) return;
        TestCase tc = findCaseByName(suite, caseName);
        if (tc == null) return;

        // Merge aktuelle Tabellen-Actions + importierter Case.
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null) actions = new ArrayList<>();
        actions.addAll(tc.getWhen());
        session.setRecordedActions(actions);
        setActions(actions); // Tabelle aktualisieren
    }

    // ---------- Dropdown Refresh & Lookups ----------
    private void refreshSuiteSelector() {
        Object sel = suiteSelector.getSelectedItem();
        suiteSelector.removeAllItems();
        suiteSelector.addItem("<neu>");
        for (TestSuite s : TestRegistry.getInstance().getAll()) {
            if (s != null && s.getName() != null) suiteSelector.addItem(s.getName());
        }
        if (sel != null && existsInCombo(suiteSelector, sel.toString())) {
            suiteSelector.setSelectedItem(sel);
        } else {
            suiteSelector.setSelectedItem("<neu>");
        }
    }

    private void refreshCaseDropdown() {
        caseDropdown.removeAllItems();
        String suiteName = (String) suiteSelector.getSelectedItem();
        if (suiteName == null || suiteName.trim().isEmpty() || "<neu>".equalsIgnoreCase(suiteName.trim())) {
            return; // keine Suite gewählt
        }
        caseDropdown.addItem("<neu>");
        TestSuite s = findSuiteByName(suiteName);
        if (s == null) return;
        for (TestCase tc : s.getTestCases()) {
            if (tc != null && tc.getName() != null) caseDropdown.addItem(tc.getName());
        }
        caseDropdown.setSelectedItem("<neu>");
    }

    private boolean existsInCombo(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) return true;
        }
        return false;
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

    // ---------- Re-added helper methods (previously removed) ----------
    private void insertRow() {
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null) actions = new ArrayList<>();
        TestAction newAction = new TestAction();
        newAction.setType(TestAction.ActionType.WHEN);
        newAction.setAction("click");
        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= actions.size()) {
            actions.add(newAction);
        } else {
            actions.add(selectedRow, newAction);
        }
        session.setRecordedActions(actions);
    }

    private void deleteSelectedRows() {
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null || actions.isEmpty()) return;
        boolean anySelected = false;
        for (TestAction ta : actions) { if (ta.isSelected()) { anySelected = true; break; } }
        if (!anySelected) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Es ist keine Zeile markiert.\nSollen wirklich alle Zeilen gelöscht werden?",
                    "Alle Zeilen löschen?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) return;
            actions.clear();
            session.setRecordedActions(actions);
            return;
        }
        actions.removeIf(TestAction::isSelected);
        session.setRecordedActions(actions);
    }

    private void moveSelectedRows(int direction) {
        List<TestAction> actions = snapshotActionsFromTable();
        if (actions == null || actions.isEmpty()) return;
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
        if (actions == null) return testCases;
        // Alte Logik: trennt bei GIVEN/THEN – jetzt sind alles WHEN, also ein Case
        TestCase single = new TestCase(baseName + "_1", new ArrayList<>(actions));
        testCases.add(single);
        return testCases;
    }

    /**
     * Liefert eine frische Kopie der im UI sichtbaren Actions.
     * Stellt sicher, dass laufende Cell-Edits committed werden.
     */
    private List<TestAction> snapshotActionsFromTable() {
        // Laufende Editor-Session committen
        if (actionTable.isEditing()) {
            try { actionTable.getCellEditor().stopCellEditing(); } catch (Exception ignore) {}
        }
        List<TestAction> raw = actionTable.getActions();
        if (raw == null) return null;
        // Flache Kopie reicht (Objekte selbst werden weiterverwendet) – falls Deep Copy nötig wäre, hier ergänzen.
        return new ArrayList<>(raw);
    }

    private TestSuite findSuiteByName(String name) {
        if (name == null) return null;
        for (TestSuite s : TestRegistry.getInstance().getAll()) {
            if (s != null && name.equals(s.getName())) return s;
        }
        return null;
    }

    private TestCase findCaseByName(TestSuite suite, String name) {
        if (suite == null || name == null) return null;
        for (TestCase tc : suite.getTestCases()) {
            if (tc != null && name.equals(tc.getName())) return tc;
        }
        return null;
    }

    public void unregister() {
        if (session != null) {
            try { if (session.isRecording()) session.stop(); } catch (Exception ignore) {}
            RecorderCoordinator.getInstance().unregisterTab(this);
            session = null;
        }
    }
}
