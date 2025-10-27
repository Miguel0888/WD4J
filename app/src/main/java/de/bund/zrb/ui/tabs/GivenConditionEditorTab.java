package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.expressions.ExpressionInputPanel;
import de.bund.zrb.ui.expressions.ExpressionTreeModelBuilder;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.ui.parts.Code;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.LiteralExpression;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit one GivenCondition in a human-friendly way.
 *
 * Intent:
 * - Let test authors maintain Given facts like
 *   "Es existiert eine {{Belegnummer}}." or "{{OTP({{username}})}}"
 * - Show live AST preview so they understand what will be reusable later
 * - Persist everything back into the GivenCondition for this scenario
 *
 * Responsibilities:
 * - Choose Given type (from GivenRegistry)
 * - Choose User (username is stored)
 * - Render dynamic fields defined in GivenTypeDefinition
 * - Provide ExpressionInputPanel (syntax help, rainbow braces, {{ / }} buttons)
 * - Parse expressionRaw live and show AST in a read-only JTree
 *
 * Persistence format in condition.value:
 *   username=<...>&someField=<...>&expressionRaw=<user text>...
 *
 * NO node binding/selection is done here. That happens later (e.g. in ActionEditorTab).
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    // Model
    private final GivenCondition condition;

    // Header widgets (always visible)
    private final JComboBox<String> typeBox;
    private final JComboBox<String> userBox;

    // Dynamic UI area below header
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());

    // Keep references to dynamic input components so save() can read them
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    // Expression editing + preview
    private ExpressionInputPanel expressionPanel; // custom panel with RSyntaxTextArea, help icon, {{/}} buttons
    private JTree astPreviewTree;                 // read-only AST tree
    private JScrollPane astPreviewScroll;         // scroll wrapper for tree

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Header: Typ + User
        ////////////////////////////////////////////////////////////////////////////

        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        // Typ
        header.add(new JLabel("Typ:"));
        String[] types = GivenRegistry.getInstance().getAll()
                .stream()
                .map(GivenTypeDefinition::getType)
                .toArray(String[]::new);
        typeBox = new JComboBox<String>(types);
        typeBox.setEditable(true);
        typeBox.setSelectedItem(condition.getType());
        header.add(typeBox);

        // User
        header.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll()
                .stream()
                .map(User::getUsername)
                .toArray(String[]::new);
        userBox = new JComboBox<String>(users);

        // Prefill username from condition.value
        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        // Put header + dynamicArea into the north/center of the main panel
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        // Rebuild dynamic fields when type changes
        typeBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rebuildDynamicForm(String.valueOf(typeBox.getSelectedItem()));
            }
        });

        ////////////////////////////////////////////////////////////////////////////
        // Footer with "Speichern"
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
        // Initial layout build
        ////////////////////////////////////////////////////////////////////////////

        rebuildDynamicForm(condition.getType());
    }

    /**
     * Rebuild the lower part (dynamicFieldsPanel):
     *  - Render fields defined for the chosen Given type
     *  - Render ExpressionInputPanel + AST preview
     *  - Register live listeners so AST + status update immediately on typing
     */
    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        ////////////////////////////////////////////////////////////////////////////
        // 1. Render standard fields from GivenTypeDefinition
        ////////////////////////////////////////////////////////////////////////////
        if (def != null) {
            for (GivenField field : def.getFields().values()) {

                Object value = paramMap.get(field.name);
                if (value == null && field.defaultValue != null) {
                    value = field.defaultValue;
                }

                // Case: multiline code editor
                if (field.type == Code.class) {
                    gbc.gridx = 0; gbc.gridy = row++;
                    gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                    RSyntaxTextArea codeEditor = new RSyntaxTextArea(10, 40);
                    codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                    codeEditor.setCodeFoldingEnabled(true);
                    codeEditor.setText(value != null ? value.toString() : "");
                    RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);

                    gbc.gridy = row++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
                    dynamicFieldsPanel.add(scrollPane, gbc);

                    inputs.put(field.name, codeEditor);
                    continue;
                }

                // Case: preconditionRef.id -> dropdown of known preconditions
                if (TYPE_PRECONDITION_REF.equals(def.getType()) && "id".equals(field.name)) {
                    gbc.gridx = 0; gbc.gridy = row;
                    gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
                    gbc.fill = GridBagConstraints.NONE;
                    dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                    gbc.gridx = 1; gbc.weightx = 1.0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;

                    JComboBox<PreItem> preBox = new JComboBox<PreItem>();
                    List<Precondition> pres = PreconditionRegistry.getInstance().getAll();
                    PreItem selected = null;

                    for (int i = 0; i < pres.size(); i++) {
                        Precondition p = pres.get(i);
                        String id = p.getId();
                        String name = (p.getName() != null && p.getName().trim().length() > 0)
                                ? p.getName().trim()
                                : "(unnamed)";
                        PreItem item = new PreItem(id, name);
                        preBox.addItem(item);

                        if (value != null && value.equals(id)) {
                            selected = item;
                        }
                    }
                    if (selected != null) {
                        preBox.setSelectedItem(selected);
                    }

                    dynamicFieldsPanel.add(preBox, gbc);
                    inputs.put(field.name, preBox);

                    row++;
                    continue;
                }

                // Case: normal single-line text field
                gbc.gridx = 0; gbc.gridy = row;
                gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
                gbc.fill = GridBagConstraints.NONE;
                dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

                gbc.gridx = 1; gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                JTextField tf = new JTextField(value != null ? value.toString() : "");
                dynamicFieldsPanel.add(tf, gbc);
                inputs.put(field.name, tf);

                row++;
            }
        }

        ////////////////////////////////////////////////////////////////////////////
        // 2. ExpressionInputPanel (our new fancy editor with rainbow braces etc.)
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        expressionPanel = new ExpressionInputPanel();

        // Restore previously saved expressionRaw if available
        String initialRaw = asString(paramMap.get("expressionRaw"));
        if (initialRaw != null && initialRaw.trim().length() > 0) {
            expressionPanel.setExpressionText(initialRaw.trim());
        } else {
            // else leave panel's default demo text (it guides the user)
        }

        dynamicFieldsPanel.add(expressionPanel, gbc);

        // Store so save() can persist expressionRaw
        inputs.put("expressionRaw", expressionPanel);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // 3. AST preview tree (read-only)
        ////////////////////////////////////////////////////////////////////////////

        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        astPreviewTree = new JTree(new DefaultMutableTreeNode("No data"));
        astPreviewTree.setRootVisible(true);
        astPreviewTree.setShowsRootHandles(true);
        astPreviewTree.setCellRenderer(createPreviewRenderer());
        astPreviewTree.setFocusable(false);

        astPreviewScroll = new JScrollPane(astPreviewTree);
        astPreviewScroll.setPreferredSize(new Dimension(300, 160));

        dynamicFieldsPanel.add(astPreviewScroll, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // 4. Live update wiring:
        //    - Listen to text changes in expressionPanel editor
        //    - On each change: parse, update AST, update status color/message
        ////////////////////////////////////////////////////////////////////////////

        Document doc = getExpressionDocument();
        if (doc != null) {
            doc.addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void removeUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void changedUpdate(DocumentEvent e) { updateAstAndStatus(); }
            });
        }

        // Run once initially so preview + status are correct
        updateAstAndStatus();

        dynamicFieldsPanel.revalidate();
        dynamicFieldsPanel.repaint();
    }

    /**
     * Build AST from current expression text, update preview tree,
     * and set validation status in the ExpressionInputPanel.
     *
     * Policy:
     * - Parse using ExpressionParser.
     * - If ok: show full AST, status "✔ Ausdruck ist gültig".
     * - If fail: show error node, status "❌ Fehler: ...".
     */
    private void updateAstAndStatus() {
        if (expressionPanel == null || astPreviewTree == null) {
            return;
        }

        String raw = expressionPanel.getExpressionText();

        ExpressionTreeNode uiRoot;
        try {
            if (raw == null || raw.trim().length() == 0) {
                // No content -> neutral preview
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

    /**
     * Get the Swing Document from the internal RSyntaxTextArea so we can attach listeners.
     *
     * IMPORTANT:
     * - Add this method to ExpressionInputPanel:
     *
     *   public RSyntaxTextArea getEditor() {
     *       return editor;
     *   }
     *
     * Then this method here will work.
     */
    private Document getExpressionDocument() {
        if (expressionPanel == null) return null;
        return expressionPanel.getEditor().getDocument();
    }

    /**
     * Build a "No data" AST node for empty expressions.
     */
    private ExpressionTreeNode buildFallbackNoDataTree() {
        LiteralExpression lit = new LiteralExpression("No data");
        return new ExpressionTreeNode(
                lit,
                "Literal: \"No data\"",
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    /**
     * Build an "ERROR" AST node for invalid expressions.
     */
    private ExpressionTreeNode buildErrorTree(String raw, String msg) {
        LiteralExpression lit = new LiteralExpression("Parse Error: " + msg);
        return new ExpressionTreeNode(
                lit,
                "ERROR parsing: " + raw,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    /**
     * Convert ExpressionTreeNode (domain-ish UI struct)
     * into a Swing TreeModel for JTree rendering.
     */
    private TreeModel buildSwingModel(ExpressionTreeNode rootUiNode) {
        ExpressionTreeModelBuilder builder = new ExpressionTreeModelBuilder();
        DefaultMutableTreeNode swingRoot = builder.buildSwingTree(rootUiNode);
        return new DefaultTreeModel(swingRoot);
    }

    /**
     * Expand all tree rows so user always sees full AST immediately.
     */
    private void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    /**
     * Use a renderer that shows the label for each AST node.
     * (ExpressionTreeModelBuilder.NodePayload holds the display label.)
     */
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
     * Save changes back into the GivenCondition and persist via TestRegistry.
     *
     * Steps:
     * - type -> condition.setType(...)
     * - username from userBox
     * - all dynamic fields (including expressionRaw from expressionPanel)
     * - serialize into condition.value "key=value&key=value"
     */
    private void save() {
        // Persist chosen type
        condition.setType(String.valueOf(typeBox.getSelectedItem()));

        Map<String, String> result = new LinkedHashMap<String, String>();

        // username
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // Dynamic fields incl. expressionRaw
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            if (input instanceof ExpressionInputPanel) {
                ExpressionInputPanel p = (ExpressionInputPanel) input;
                result.put(name, p.getExpressionText());
            } else if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            } else if (input instanceof JComboBox) {
                JComboBox box = (JComboBox) input;

                // PreItem case (preconditionRef.id)
                Object sel = box.getSelectedItem();
                if (sel instanceof PreItem) {
                    PreItem pi = (PreItem) sel;
                    result.put(name, pi.id); // store only UUID
                } else {
                    Object editorVal = box.getEditor() != null
                            ? box.getEditor().getItem()
                            : sel;
                    result.put(name, (editorVal == null) ? "" : String.valueOf(editorVal));
                }
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

    // -------------------------------------------------------------------------
    // Helpers for serializing / parsing condition.value
    // -------------------------------------------------------------------------

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

    /**
     * Internal helper for preconditionRef.id combo box.
     * Display "Name {UUID}", but persist only the UUID.
     */
    private static final class PreItem {
        final String id;
        final String name;
        PreItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String toString() {
            return name + " {" + id + "}";
        }
    }

    private String asString(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
