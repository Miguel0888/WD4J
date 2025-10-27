package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.expressions.ExpressionInputPanel;
import de.bund.zrb.ui.expressions.ExpressionTreeModelBuilder;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Editor für ein einzelnes GivenCondition.
 *
 * Wichtig für dich:
 * - Konstruktor bleibt (GivenCondition condition) -> NICHT geändert.
 * - username & expressionRaw werden wie gehabt gespeichert.
 * - Wir zeigen dir live:
 *   - bunten Ausdrucks-Editor (mit Rainbow-Braces, Toolbar, etc.)
 *   - AST-Vorschau (klein)
 *   - und vor allem eine GROßE Variablen-Tabelle mit "Name / Wert / Scope".
 *
 * Ziel:
 * - Du siehst sofort deine Variablen inkl. username.
 * - Die Tabelle bekommt den Platz (weighty=1.0).
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    // ----- Model -----
    private final GivenCondition condition;

    // ----- Header oben -----
    private final JComboBox<String> userBox;
    private JComboBox<PreItem> preconditionComboBox; // für Vorlage/Precondition-UUID

    // ----- Mittelteil (GridBag) -----
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    // ----- Ausdruck / AST / Variablen -----
    private ExpressionInputPanel expressionPanel;
    private JTree astPreviewTree;
    private JScrollPane astPreviewScroll;

    private JTable variablesTable;
    private DefaultTableModel variablesModel;
    private JScrollPane variablesScroll;

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // HEADER: User-Auswahl (wir lassen das so wie vorher, nichts kaputtmachen)
        ////////////////////////////////////////////////////////////////////////////

        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        header.add(new JLabel("User:"));

        String[] users = UserRegistry.getInstance().getAll()
                .stream()
                .map(User::getUsername)
                .toArray(String[]::new);
        userBox = new JComboBox<>(users);

        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        ////////////////////////////////////////////////////////////////////////////
        // FORM-KONTAINER
        ////////////////////////////////////////////////////////////////////////////

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        ////////////////////////////////////////////////////////////////////////////
        // FOOTER: Speichern
        ////////////////////////////////////////////////////////////////////////////

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
        // Mittelteil aufbauen (Precondition-Dropdown, Expression, AST, Variablen)
        ////////////////////////////////////////////////////////////////////////////

        rebuildDynamicForm(condition.getType());

        // Dropdown-Logik (Vorlage -> expressionRaw übernehmen)
        wirePreconditionTemplateBehavior();
    }

    /**
     * Baut das UI im Mittelteil neu auf.
     *
     * Layout (GridBag rows):
     *
     * row0: [Label "Precondition / Vorlage:" | ComboBox]  (nur falls type==preconditionRef)
     * row1: ExpressionInputPanel (Rainbow, Toolbar, etc.) (weighty=0)
     * row2: AST Preview (klein)                            (weighty=0)
     * row3: Variablen-Tabelle (groß!)                      (weighty=1.0, fill=BOTH)
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
        // row0: Precondition-/Vorlagen-Auswahl (nur für preconditionRef)
        ////////////////////////////////////////////////////////////////////////////
        if (TYPE_PRECONDITION_REF.equals(type)) {
            Object rawValue = paramMap.get("id"); // gespeicherte UUID
            String savedUuid = rawValue != null ? String.valueOf(rawValue) : "";

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel("Precondition / Vorlage:"), gbc);

            gbc.gridx = 1;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JComboBox<PreItem> preBox = new JComboBox<>();
            preBox.setEditable(false);

            // leerer Eintrag
            PreItem emptyItem = new PreItem(null, null);
            preBox.addItem(emptyItem);

            // alle vorhandenen Preconditions
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
        // row1: Ausdrucks-Editor (ExpressionInputPanel)
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0; // Editor kriegt fixe Höhe
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
        // row2: AST Preview (klein, Info nur)
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0; // soll nicht alles fressen
        gbc.fill = GridBagConstraints.BOTH;

        JPanel astPanel = new JPanel(new BorderLayout(4, 4));
        JLabel astLabel = new JLabel("Ausdrucks-Struktur (Preview):");
        astPanel.add(astLabel, BorderLayout.NORTH);

        astPreviewTree = new JTree(new DefaultMutableTreeNode("No data"));
        astPreviewTree.setRootVisible(true);
        astPreviewTree.setShowsRootHandles(true);
        astPreviewTree.setCellRenderer(createPreviewRenderer());
        astPreviewTree.setFocusable(false);

        astPreviewScroll = new JScrollPane(astPreviewTree);
        astPreviewScroll.setPreferredSize(new Dimension(400, 120)); // klein halten
        astPanel.add(astPreviewScroll, BorderLayout.CENTER);

        dynamicFieldsPanel.add(astPanel, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // row3: Variablen-/Werte-Tabelle (DAS IST DEIN WICHTIGER TEIL)
        //
        // -> kriegt weighty = 1.0 und fill=BOTH
        // -> eigener ScrollPane mit ordentlicher PreferredSize
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // <-- friss den Rest des Fensters!
        gbc.fill = GridBagConstraints.BOTH;

        JPanel varsPanel = new JPanel(new BorderLayout(4, 4));
        JLabel varsLabel = new JLabel("Variablen / Werte (Scope-Kette):");
        varsPanel.add(varsLabel, BorderLayout.NORTH);

        variablesModel = new DefaultTableModel(
                new Object[]{"Name", "Wert", "Scope"}, 0
        ) {
            public boolean isCellEditable(int row, int col) {
                // erstmal read-only anzeigen
                return false;
            }
        };
        variablesTable = new JTable(variablesModel);
        variablesTable.setFillsViewportHeight(true);
        variablesTable.setRowHeight(22);

        variablesScroll = new JScrollPane(variablesTable);
        variablesScroll.setPreferredSize(new Dimension(400, 200));
        varsPanel.add(variablesScroll, BorderLayout.CENTER);

        dynamicFieldsPanel.add(varsPanel, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // Live-Parsing & erstes Rendering:
        // - AST bauen
        // - Variablenliste befüllen
        ////////////////////////////////////////////////////////////////////////////

        Document doc = getExpressionDocument();
        if (doc != null) {
            doc.addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { refreshAstAndVars(); }
                public void removeUpdate(DocumentEvent e)  { refreshAstAndVars(); }
                public void changedUpdate(DocumentEvent e){ refreshAstAndVars(); }
            });
        }

        refreshAstAndVars(); // initial

        dynamicFieldsPanel.revalidate();
        dynamicFieldsPanel.repaint();
    }

    /**
     * Precondition/Vorlage steuert expressionRaw:
     * - leer => Ausdruck leeren + neutraler Status
     * - sonst => Ausdruck = Name der Precondition
     */
    private void wirePreconditionTemplateBehavior() {
        if (preconditionComboBox == null) return;

        preconditionComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) return;
                if (expressionPanel == null) return;

                Object selObj = preconditionComboBox.getSelectedItem();
                if (!(selObj instanceof PreItem)) {
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    refreshAstAndVars();
                    return;
                }

                PreItem item = (PreItem) selObj;
                if (item.id == null) {
                    // leer
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    refreshAstAndVars();
                    return;
                }

                // Vorlage = Name übernehmen
                String templateName = (item.name != null) ? item.name : "";
                expressionPanel.setExpressionText(templateName);
                refreshAstAndVars();
            }
        });
    }

    /**
     * Das ist jetzt der zentrale "live update":
     * - parse expressionRaw
     * - baue AST-Tree
     * - baue Variablen-Tabelle (mit username aus diesem Given)
     */
    private void refreshAstAndVars() {
        if (expressionPanel == null) return;

        String raw = expressionPanel.getExpressionText();

        // 1) AST + Status
        ExpressionTreeNode uiRoot;
        ResolvableExpression parsedExpr = null;
        try {
            if (raw == null || raw.trim().isEmpty()) {
                uiRoot = buildFallbackNoDataTree();
                expressionPanel.setStatusNeutral("Kein Ausdruck");
            } else {
                ExpressionParser parser = new ExpressionParser();
                parsedExpr = parser.parseTemplate(raw);
                uiRoot = ExpressionTreeNode.fromExpression(parsedExpr);
                expressionPanel.setStatusOk("Ausdruck ist gültig");
            }
        } catch (Exception ex) {
            uiRoot = buildErrorTree(raw, ex.getMessage());
            expressionPanel.setStatusError("Fehler: " + ex.getMessage());
        }

        // update AST view
        TreeModel model = buildSwingModel(uiRoot);
        astPreviewTree.setModel(model);
        expandAll(astPreviewTree);

        // 2) Variablen-Tabelle füllen
        rebuildVariablesTable(parsedExpr);
    }

    /**
     * Variablen-Tabelle neu aufbauen:
     *
     * Spalten:
     *  - Name
     *  - Wert
     *  - Scope
     *
     * Aktuell:
     *  - username aus diesem Given -> Scope "local"
     *  - alle {{xxx}} aus dem Ausdruck -> Scope "unknown" (Wert leer),
     *    außer "username", den können wir auflösen.
     */
    private void rebuildVariablesTable(ResolvableExpression rootExpr) {
        if (variablesModel == null) return;

        variablesModel.setRowCount(0);

        // 1) username aus diesem Given (wir behal­ten das aktuelle userBox-Selection / param)
        String localUser = null;
        Object uSel = userBox.getSelectedItem();
        if (uSel != null && uSel.toString().trim().length() > 0) {
            localUser = uSel.toString().trim();
        } else {
            // fallback: aus condition.value -> username=
            Map<String, Object> map = parseValueMap(condition.getValue());
            Object u = map.get("username");
            if (u != null && u.toString().trim().length() > 0) {
                localUser = u.toString().trim();
            }
        }

        if (localUser != null && !localUser.isEmpty()) {
            variablesModel.addRow(new Object[]{
                    "username",
                    localUser,
                    "local"
            });
        }

        // 2) Variablen aus dem Ausdruck sammeln
        Set<String> varsInExpr = new LinkedHashSet<>();
        collectVariablesRecursive(rootExpr, varsInExpr);

        for (String varName : varsInExpr) {
            if ("username".equals(varName)) {
                // haben wir oben schon mit Wert eingetragen
                continue;
            }
            variablesModel.addRow(new Object[]{
                    varName,
                    "",           // Wert kennen wir hier noch nicht
                    "unknown"     // später: case/suite/root etc.
            });
        }

        // Tabelle neu zeichnen
        variablesTable.revalidate();
        variablesTable.repaint();
    }

    /**
     * Alle verwendeten Variablen aus dem AST sammeln (rekursiv).
     * Wir traversieren CompositeExpression, FunctionExpression, VariableExpression usw.
     */
    private void collectVariablesRecursive(ResolvableExpression expr, Set<String> out) {
        if (expr == null) return;

        if (expr instanceof VariableExpression) {
            VariableExpression v = (VariableExpression) expr;
            out.add(v.getName());
            return;
        }
        if (expr instanceof LiteralExpression) {
            return;
        }
        if (expr instanceof FunctionExpression) {
            FunctionExpression f = (FunctionExpression) expr;
            List<ResolvableExpression> args = f.getArgs();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    collectVariablesRecursive(args.get(i), out);
                }
            }
            return;
        }
        if (expr instanceof CompositeExpression) {
            CompositeExpression c = (CompositeExpression) expr;
            List<ResolvableExpression> parts = c.getParts();
            if (parts != null) {
                for (int i = 0; i < parts.size(); i++) {
                    collectVariablesRecursive(parts.get(i), out);
                }
            }
        }
    }

    // ---- AST helpers ----

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

    private Document getExpressionDocument() {
        if (expressionPanel == null) return null;
        return expressionPanel.getEditor().getDocument();
    }

    // ---- Speichern wie gehabt ----

    private void save() {
        Map<String, String> result = new LinkedHashMap<>();

        // username (scope-localer Nutzer)
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String key = entry.getKey();
            JComponent comp = entry.getValue();

            if (comp instanceof ExpressionInputPanel) {
                ExpressionInputPanel p = (ExpressionInputPanel) comp;
                result.put(key, p.getExpressionText());

            } else if (comp instanceof JComboBox) {
                JComboBox<?> box = (JComboBox<?>) comp;
                Object sel = box.getSelectedItem();
                if (sel instanceof PreItem) {
                    PreItem pi = (PreItem) sel;
                    result.put(key, (pi.id == null) ? "" : pi.id);
                } else {
                    Object editorVal = (box.getEditor() != null)
                            ? box.getEditor().getItem()
                            : sel;
                    result.put(key, editorVal == null ? "" : String.valueOf(editorVal));
                }
            } else if (comp instanceof JTextField) {
                result.put(key, ((JTextField) comp).getText());
            }
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();

        JOptionPane.showMessageDialog(
                this,
                "Änderungen gespeichert.\n" +
                        "expressionRaw = " + result.get("expressionRaw")
        );
    }

    // ---- key=value&... Parser/Helfer ----

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (String pair : pairs) {
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
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    // ---- Precondition-Auswahl ----

    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String toString() {
            if (id == null && name == null) return "";
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
