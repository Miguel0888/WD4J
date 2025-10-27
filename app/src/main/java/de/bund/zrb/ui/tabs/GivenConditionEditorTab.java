package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.expressions.ExpressionInputPanel;
import de.bund.zrb.ui.expressions.ExpressionTreeModelBuilder;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.LiteralExpression;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit one GivenCondition in a human-friendly way.
 *
 * Intent:
 * - Bearbeite ein einzelnes Given.
 * - Erfasse:
 *    - username / User-Auswahl
 *    - optionale Referenz auf eine Precondition (preconditionRef.id)
 *    - Ausdruck (expressionRaw) inkl. Live-AST-Vorschau, Syntax-Status
 *    - Scope-Variablen (Key/Value) für diesen Given (Case-/Suite-/Root-Scope)
 *
 * Architektur:
 * - Konstruktor nimmt NUR das GivenCondition-Objekt.
 * - Wir leiten den Scope (CASE / SUITE / ROOT) intern über TestRegistry her,
 *   indem wir schauen, wo dieses Given im Modell vorkommt.
 *
 * Persistenz:
 * - Alle Daten werden in condition.setValue(...) als key=value&key2=value2 gespeichert.
 *   Reservierte Keys:
 *     username
 *     id              (UUID der Precondition, falls type == preconditionRef)
 *     expressionRaw   (der eingegebene Text mit {{...}})
 *   Alle anderen Keys aus der Variablentabelle werden ebenfalls geschrieben.
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    /**
     * Represent logical runtime scope for variables of this Given.
     * SUITE  -> Suite-weite Variablen
     * CASE   -> nur in diesem TestCase gültig
     * ROOT   -> global/fallback, wenn weder Suite- noch Case-Zuordnung gefunden wurde
     */
    public static enum ScopeLevel {
        SUITE,
        CASE,
        ROOT
    }

    // ----------------- Modell -----------------
    private final GivenCondition condition;
    private final ScopeLevel scopeLevel;

    // ----------------- Header: User -----------------
    private final JComboBox<String> userBox;

    // ----------------- Dynamischer Bereich -----------------
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    // preconditionRef.id Auswahl (gleichzeitig "Vorlage")
    private JComboBox<PreItem> preconditionComboBox;

    // Expression + AST
    private ExpressionInputPanel expressionPanel;
    private JTree astPreviewTree;
    private JScrollPane astPreviewScroll;

    // Scope-Variablen Tabelle
    private VariableTablePanel variableTablePanel;

    /**
     * Konstruktor wie von dir verlangt: nur das GivenCondition.
     * Der Scope wird intern bestimmt.
     */
    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        this.scopeLevel = detectScopeLevel(condition);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Header: User (=> "username")
        ////////////////////////////////////////////////////////////////////////////

        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));
        header.add(new JLabel("User (username):"));

        String[] users = UserRegistry.getInstance().getAll()
                .stream()
                .map(User::getUsername)
                .toArray(String[]::new);

        userBox = new JComboBox<String>(users);

        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        ////////////////////////////////////////////////////////////////////////////
        // Form / Footer
        ////////////////////////////////////////////////////////////////////////////

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        ////////////////////////////////////////////////////////////////////////////
        // Hauptbereich aufbauen
        ////////////////////////////////////////////////////////////////////////////

        rebuildDynamicForm(condition.getType());

        // Nach dem Aufbau (preconditionComboBox existiert jetzt) das Vorlagen-Verhalten verdrahten
        wirePreconditionTemplateBehavior();
    }

    /**
     * Baue die komplette rechte Seite für dieses Given:
     * (1) Precondition-/Vorlagen-Auswahl (falls type == preconditionRef)
     * (2) ExpressionInputPanel (Regenbogen-Klammern, {{ }}, Help-Icon, Buttons)
     * (3) AST-Vorschau (JTree)
     * (4) Scope-Variablen Tabelle inkl. Scope-Label (Suite / Case / Root)
     */
    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();
        preconditionComboBox = null;

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        ////////////////////////////////////////////////////////////////////////////
        // (1) Precondition / Vorlage-Auswahl (nur bei preconditionRef)
        ////////////////////////////////////////////////////////////////////////////

        if (TYPE_PRECONDITION_REF.equals(type)) {
            Object rawValue = paramMap.get("id");
            String savedUuid = rawValue != null ? String.valueOf(rawValue) : "";

            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel("Precondition / Vorlage:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JComboBox<PreItem> preBox = new JComboBox<PreItem>();
            preBox.setEditable(false);

            // explizit erster Eintrag = leer
            PreItem emptyItem = new PreItem(null, null);
            preBox.addItem(emptyItem);

            List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
            PreItem selected = emptyItem;
            for (int i = 0; i < pres.size(); i++) {
                Precondition p = pres.get(i);
                String id = p.getId();
                String name = (p.getName() != null && p.getName().trim().length() > 0)
                        ? p.getName().trim()
                        : "(unnamed)";
                PreItem item = new PreItem(id, name);
                preBox.addItem(item);
                if (savedUuid.equals(id)) {
                    selected = item;
                }
            }
            preBox.setSelectedItem(selected);

            dynamicFieldsPanel.add(preBox, gbc);

            preconditionComboBox = preBox;
            inputs.put("id", preBox);

            row++;
        }

        ////////////////////////////////////////////////////////////////////////////
        // (2) ExpressionInputPanel mit Regenbogen-Highlight und {{}}-Buttons
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        expressionPanel = new ExpressionInputPanel();

        String initialRaw = asString(paramMap.get("expressionRaw"));
        if (initialRaw != null && initialRaw.trim().length() > 0) {
            expressionPanel.setExpressionText(initialRaw.trim());
        }
        dynamicFieldsPanel.add(expressionPanel, gbc);
        inputs.put("expressionRaw", expressionPanel);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // (3) AST-Vorschau (read-only JTree der aktuellen expressionRaw)
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        astPreviewTree = new JTree(new DefaultMutableTreeNode("No data"));
        astPreviewTree.setRootVisible(true);
        astPreviewTree.setShowsRootHandles(true);
        astPreviewTree.setCellRenderer(createPreviewRenderer());
        astPreviewTree.setFocusable(false);

        astPreviewScroll = new JScrollPane(astPreviewTree);
        astPreviewScroll.setPreferredSize(new Dimension(300, 140));
        dynamicFieldsPanel.add(astPreviewScroll, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // (4) Variablen-Tabelle (Scope-Variablen)
        //
        // Diese Variablen + username fließen zur Laufzeit in den entsprechenden
        // Scope-Kontext (Case-/Suite-/Root-Variablen).
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;

        variableTablePanel = new VariableTablePanel();

        // passendes Scope-Label setzen
        String scopeText;
        switch (scopeLevel) {
            case SUITE:
                scopeText = "Scope-Variablen (Suite-Scope):";
                break;
            case CASE:
                scopeText = "Scope-Variablen (Case-Scope):";
                break;
            default:
                scopeText = "Scope-Variablen (Global-Scope):";
                break;
        }
        variableTablePanel.setScopeLabel(scopeText);

        // Tabelle initial mit allen Key/Value befüllen außer reservierten Keys
        for (Map.Entry<String, Object> e : paramMap.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if ("id".equals(k)) continue;
            if ("expressionRaw".equals(k)) continue;
            // username darf (und soll) hier auftauchen
            if (v != null) {
                variableTablePanel.addRow(k, String.valueOf(v));
            }
        }

        // username aus userBox synchronisieren
        syncUserBoxIntoTable(variableTablePanel);

        dynamicFieldsPanel.add(variableTablePanel, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // 5. Live-AST-Update für expressionRaw
        ////////////////////////////////////////////////////////////////////////////

        Document doc = getExpressionDocument();
        if (doc != null) {
            doc.addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void removeUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void changedUpdate(DocumentEvent e) { updateAstAndStatus(); }
            });
        }

        // initial parse ausführen
        updateAstAndStatus();

        dynamicFieldsPanel.revalidate();
        dynamicFieldsPanel.repaint();
    }

    /**
     * Scope bestimmen (CASE / SUITE / ROOT), ohne dass der Aufrufer das übergeben muss.
     *
     * Strategie:
     * - Hol alle Suites aus TestRegistry.
     * - Wenn dieses Given direkt in suite.getGiven() vorkommt -> SUITE
     * - Sonst: jede suite durchgehen, jede ihrer TestCases prüfen:
     *      wenn testCase.getGiven() dieses Given enthält -> CASE
     * - Falls wir es nirgends finden -> ROOT
     *
     * Annahme:
     * - TestRegistry hat getAllSuites().
     * - TestSuite hat getGiven() und getTestCases() (oder getCases()).
     *   Wenn dein Modell leicht anders heißt, bitte hier anpassen.
     */
    private ScopeLevel detectScopeLevel(GivenCondition target) {
        try {
            List<TestSuite> suites = TestRegistry.getInstance().getAll();

            // 1) Suite-Level?
            for (int i = 0; i < suites.size(); i++) {
                TestSuite s = suites.get(i);

                // Suite-Givens?
                List<GivenCondition> suiteGivens = s.getGiven();
                if (suiteGivens != null) {
                    for (int g = 0; g < suiteGivens.size(); g++) {
                        if (suiteGivens.get(g) == target) {
                            return ScopeLevel.SUITE;
                        }
                    }
                }

                // 2) Case-Level?
                List<TestCase> cases = s.getTestCases(); // falls deine API anders heißt, hier anpassen
                if (cases != null) {
                    for (int c = 0; c < cases.size(); c++) {
                        TestCase tc = cases.get(c);
                        List<GivenCondition> caseGivens = tc.getGiven();
                        if (caseGivens != null) {
                            for (int g = 0; g < caseGivens.size(); g++) {
                                if (caseGivens.get(g) == target) {
                                    return ScopeLevel.CASE;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
            // Wenn irgendwas schief geht (z.B. API anders), fallback auf ROOT
        }
        return ScopeLevel.ROOT;
    }

    /**
     * username zwischen ComboBox und Tabelle synchronisieren:
     * - Falls Tabelle noch keinen "username" Key hat, nimm userBox.
     * - Falls Tabelle schon "username" hat, übernehme diesen Wert in userBox.
     */
    private void syncUserBoxIntoTable(VariableTablePanel tablePanel) {
        String selUser = (String) userBox.getSelectedItem();
        if (selUser != null && selUser.trim().length() > 0) {
            if (!tablePanel.hasKey("username")) {
                tablePanel.addRow("username", selUser.trim());
            } else {
                String tableVal = tablePanel.getValueForKey("username");
                if (tableVal != null && tableVal.trim().length() > 0) {
                    userBox.setSelectedItem(tableVal.trim());
                } else {
                    tablePanel.setValueForKey("username", selUser.trim());
                }
            }
        }
    }

    /**
     * Precondition-Auswahl wirkt wie "Vorlage":
     * - Leerer Eintrag -> Ausdruck leeren, Status neutral
     * - Eintrag mit UUID -> Ausdruck mit Precondition-Name vorbelegen
     */
    private void wirePreconditionTemplateBehavior() {
        if (preconditionComboBox == null) {
            return;
        }
        preconditionComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                if (expressionPanel == null) return;

                Object selObj = preconditionComboBox.getSelectedItem();
                if (!(selObj instanceof PreItem)) {
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    updateAstAndStatus();
                    return;
                }

                PreItem item = (PreItem) selObj;
                if (item.id == null) {
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    updateAstAndStatus();
                    return;
                }

                String templateName = (item.name != null) ? item.name : "";
                expressionPanel.setExpressionText(templateName);
                updateAstAndStatus();
            }
        });
    }

    /**
     * Parse expressionRaw und aktualisiere AST-Baum + Statusanzeige im Editor.
     */
    private void updateAstAndStatus() {
        if (expressionPanel == null || astPreviewTree == null) return;

        String raw = expressionPanel.getExpressionText();

        ExpressionTreeNode uiRoot;
        try {
            if (raw == null || raw.trim().length() == 0) {
                uiRoot = buildFallbackNoDataTree();
                expressionPanel.setStatusNeutral("Kein Ausdruck");
            } else {
                ExpressionParser parser = new ExpressionParser();
                ResolvableExpression expr = parser.parseTemplate(raw);

                uiRoot = ExpressionTreeNode.fromExpression(expr);
                expressionPanel.setStatusOk("Ausdruck ist gültig");
            }
        } catch (Exception ex) {
            uiRoot = buildErrorTree(raw, ex.getMessage());
            expressionPanel.setStatusError("Fehler: " + ex.getMessage());
        }

        TreeModel model = buildSwingModel(uiRoot);
        astPreviewTree.setModel(model);
        expandAll(astPreviewTree);
    }

    private Document getExpressionDocument() {
        if (expressionPanel == null) return null;
        return expressionPanel.getEditor().getDocument();
    }

    private ExpressionTreeNode buildFallbackNoDataTree() {
        LiteralExpression lit = new LiteralExpression("No data");
        return new ExpressionTreeNode(
                lit,
                "Literal: \"No data\"",
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    private ExpressionTreeNode buildErrorTree(String raw, String msg) {
        LiteralExpression lit = new LiteralExpression("Parse Error: " + msg);
        return new ExpressionTreeNode(
                lit,
                "ERROR parsing: " + raw,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    private TreeModel buildSwingModel(ExpressionTreeNode rootUiNode) {
        ExpressionTreeModelBuilder builder = new ExpressionTreeModelBuilder();
        DefaultMutableTreeNode swingRoot = builder.buildSwingTree(rootUiNode);
        return new DefaultTreeModel(swingRoot);
    }

    private void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private DefaultTreeCellRenderer createPreviewRenderer() {
        return new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean sel,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus) {

                Component c = super.getTreeCellRendererComponent(
                        tree, value, sel, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) value;
                    Object userObj = dmtn.getUserObject();
                    if (userObj instanceof ExpressionTreeModelBuilder.NodePayload) {
                        ExpressionTreeModelBuilder.NodePayload payload =
                                (ExpressionTreeModelBuilder.NodePayload) userObj;
                        setText(payload.getLabel());
                    }
                }
                return c;
            }
        };
    }

    /**
     * Speichere alle aktuellen Eingaben zurück ins GivenCondition.
     *
     * - Hole alle Rows aus der Variablentabelle (key=value)
     * - Forciere username aus userBox => überschreibt ggf. Tabelleneintrag
     * - Hole preconditionRef.id aus preconditionComboBox (falls vorhanden)
     * - Hole expressionRaw aus expressionPanel
     * - Schreibe alles als key=value&key2=value2...
     * - Rufe TestRegistry.save()
     */
    private void save() {
        Map<String, String> result = new LinkedHashMap<String, String>();

        // 1) alle Tabellenvariablen
        java.util.List<VarRow> rows = variableTablePanel.getRows();
        for (int i = 0; i < rows.size(); i++) {
            VarRow r = rows.get(i);
            if (r.key != null && r.key.trim().length() > 0) {
                result.put(r.key.trim(), (r.value != null) ? r.value : "");
            }
        }

        // 2) username aus userBox zwingend setzen
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // 3) preconditionRef.id (UUID) speichern, falls vorhanden
        if (preconditionComboBox != null) {
            Object sel = preconditionComboBox.getSelectedItem();
            if (sel instanceof PreItem) {
                PreItem pi = (PreItem) sel;
                result.put("id", (pi.id == null ? "" : pi.id));
            } else if (sel != null) {
                result.put("id", String.valueOf(sel));
            } else {
                result.put("id", "");
            }
        }

        // 4) expressionRaw speichern
        if (expressionPanel != null) {
            result.put("expressionRaw", expressionPanel.getExpressionText());
        }

        // 5) In condition.value schreiben
        condition.setValue(serializeValueMap(result));

        // 6) global speichern
        TestRegistry.getInstance().save();

        JOptionPane.showMessageDialog(
                this,
                "Änderungen gespeichert.\n" +
                        "Scope: " + scopeLevel + "\n" +
                        "expressionRaw = " + result.get("expressionRaw")
        );
    }

    // -------------------- Hilfen: Map <-> String --------------------

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (int i = 0; i < pairs.length; i++) {
                String pair = pairs[i];
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    result.put(kv[0], kv[1]);
                }
            }
        }
        return result;
    }

    private String serializeValueMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    // -------------------- Innere Hilfstypen --------------------

    /**
     * Eine Tabellenzeile in der Scope-Variablen-Tabelle.
     */
    private static class VarRow {
        String key;
        String value;
        VarRow(String k, String v) {
            this.key = k;
            this.value = v;
        }
    }

    /**
     * TableModel für die Variablen-Tabelle.
     * Eine Zeile = (Schlüssel, Wert). Beide Spalten editierbar.
     */
    private static class VarTableModel extends AbstractTableModel {
        private final java.util.List<VarRow> rows = new java.util.ArrayList<VarRow>();

        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int col) {
            return (col == 0) ? "Name" : "Wert";
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        public Object getValueAt(int row, int col) {
            VarRow r = rows.get(row);
            return (col == 0) ? r.key : r.value;
        }

        public void setValueAt(Object aValue, int row, int col) {
            VarRow r = rows.get(row);
            if (col == 0) {
                r.key = (aValue == null) ? "" : aValue.toString();
            } else {
                r.value = (aValue == null) ? "" : aValue.toString();
            }
            fireTableRowsUpdated(row, row);
        }

        public void addRow(String key, String value) {
            rows.add(new VarRow(key, value));
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        public void removeRow(int idx) {
            if (idx >= 0 && idx < rows.size()) {
                rows.remove(idx);
                fireTableRowsDeleted(idx, idx);
            }
        }

        public java.util.List<VarRow> getRows() {
            return new java.util.ArrayList<VarRow>(rows);
        }

        public boolean hasKey(String k) {
            for (int i = 0; i < rows.size(); i++) {
                VarRow r = rows.get(i);
                if (k.equals(r.key)) return true;
            }
            return false;
        }

        public String getValueForKey(String k) {
            for (int i = 0; i < rows.size(); i++) {
                VarRow r = rows.get(i);
                if (k.equals(r.key)) return r.value;
            }
            return null;
        }

        public void setValueForKey(String k, String v) {
            for (int i = 0; i < rows.size(); i++) {
                VarRow r = rows.get(i);
                if (k.equals(r.key)) {
                    r.value = v;
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
        }
    }

    /**
     * Panel um die Variablen-Tabelle herum:
     * - Label mit Scope ("Case-Scope", "Suite-Scope", ...)
     * - JTable
     * - + / - Buttons
     */
    private static class VariableTablePanel extends JPanel {
        private final VarTableModel model;
        private final JTable table;

        VariableTablePanel() {
            super(new BorderLayout(5,5));
            this.model = new VarTableModel();
            this.table = new JTable(model);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(300, 120));

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            JButton addBtn = new JButton("+");
            JButton rmBtn  = new JButton("–");
            addBtn.setToolTipText("Neue Variable hinzufügen");
            rmBtn.setToolTipText("Ausgewählte Variable entfernen");

            addBtn.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    model.addRow("", "");
                }
            });
            rmBtn.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int sel = table.getSelectedRow();
                    if (sel >= 0) {
                        model.removeRow(sel);
                    }
                }
            });

            buttons.add(addBtn);
            buttons.add(rmBtn);

            JLabel scopeLabel = new JLabel("Scope-Variablen:");
            add(scopeLabel, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
        }

        public void setScopeLabel(String txt) {
            if (getComponentCount() > 0 && getComponent(0) instanceof JLabel) {
                ((JLabel) getComponent(0)).setText(txt);
            }
        }

        public void addRow(String key, String value) {
            model.addRow(key, value);
        }

        public boolean hasKey(String k) {
            return model.hasKey(k);
        }

        public String getValueForKey(String k) {
            return model.getValueForKey(k);
        }

        public void setValueForKey(String k, String v) {
            model.setValueForKey(k, v);
        }

        public java.util.List<VarRow> getRows() {
            return model.getRows();
        }
    }

    /**
     * Einträge für die Precondition-Auswahl.
     * id == null => leerer Eintrag ("keine Vorlage / keine Referenz").
     */
    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String toString() {
            if (id == null && name == null) {
                return "";
            }
            String n = (name != null && name.trim().length() > 0)
                    ? name.trim()
                    : "(unnamed)";
            return n + " {" + id + "}";
        }
    }

    private String asString(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
