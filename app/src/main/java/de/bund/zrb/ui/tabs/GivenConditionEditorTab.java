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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edit one GivenCondition in a human-friendly way.
 *
 * Intent:
 * - Let the tester describe a Given/precondition step using a DSL-like expression ({{...}} etc.).
 * - Let the tester optionally link a reusable Precondition (UUID), and also use it as "Vorlage".
 * - Show live AST preview. Show syntax validity feedback.
 *
 * Responsibilities:
 * - Manage user selection (username).
 * - Manage optional preconditionRef.id (UUID) and treat that dropdown as "Vorlagenauswahl".
 * - Manage expressionRaw (free form expression text) with rainbow highlighting, {{}} buttons, tooltip.
 * - Persist data back into GivenCondition.value.
 *
 * No longer use GivenRegistry. We inline knowledge about the only supported type "preconditionRef".
 *
 * condition.getType() is still stored in the model (e.g. "preconditionRef"), but we do not allow changing it here.
 */
public class GivenConditionEditorTab extends JPanel {

    private static final String TYPE_PRECONDITION_REF = "preconditionRef";

    // ---------- Model ----------
    private final GivenCondition condition;

    // ---------- Header widgets ----------
    private final JComboBox<String> userBox;

    // ---------- Dynamic area ----------
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    // We keep the special combo for preconditionRef.id so we can hook template behavior
    private JComboBox<PreItem> preconditionComboBox;

    // ---------- Expression + AST ----------
    private ExpressionInputPanel expressionPanel;
    private JTree astPreviewTree;
    private JScrollPane astPreviewScroll;

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Header: only "User"
        ////////////////////////////////////////////////////////////////////////////

        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        header.add(new JLabel("User:"));

        String[] users = UserRegistry.getInstance().getAll()
                .stream()
                .map(User::getUsername)
                .toArray(String[]::new);
        userBox = new JComboBox<String>(users);

        // Prefill username from stored condition.value
        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        ////////////////////////////////////////////////////////////////////////////
        // Layout container
        ////////////////////////////////////////////////////////////////////////////

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        // Footer with "Speichern"
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
        // Build dynamic area:
        // - Render fields depending on condition.getType()
        // - Render expression editor
        // - Render AST preview
        ////////////////////////////////////////////////////////////////////////////

        rebuildDynamicForm(condition.getType());

        // Wire template behavior now that preconditionComboBox and expressionPanel exist
        wirePreconditionTemplateBehavior();
    }

    /**
     * Build the dynamic area below the header.
     *
     * Steps:
     * 1. Render the "type-specific" inputs. Since we dropped GivenRegistry,
     *    we handle the known type(s) inline.
     *
     *    Currently we only support:
     *      TYPE_PRECONDITION_REF ("preconditionRef"):
     *        - Show a combo ("Precondition / Vorlage")
     *        - First item is "", meaning "keine Vorlage / keine Referenz"
     *        - Other items come from PreconditionRegistry
     *        - We still persist just the UUID in condition.value under key "id"
     *        - Selecting an item will also pre-fill expressionRaw as a template
     *
     *    If you ever add more types later, extend this method with more branches.
     *
     * 2. Render ExpressionInputPanel (rainbow braces, {{/}} buttons, help, status).
     * 3. Render AST preview tree below it.
     * 4. Attach live parsing listener.
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
        // 1. Type-specific block(s)
        ////////////////////////////////////////////////////////////////////////////

        if (TYPE_PRECONDITION_REF.equals(type)) {
            // We know this type uses one parameter "id" which is the referenced precondition UUID.
            // We now extend semantics: The same combo is also our "Vorlagen"-Picker.

            Object rawValue = paramMap.get("id"); // previously saved UUID
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

            // 1) Add an explicit "leer" option as first item
            PreItem emptyItem = new PreItem(null, null);
            preBox.addItem(emptyItem);

            // 2) Add all known preconditions from registry
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

                // Preselect if savedUuid matches this id
                if (savedUuid.equals(id)) {
                    selected = item;
                }
            }

            preBox.setSelectedItem(selected);

            dynamicFieldsPanel.add(preBox, gbc);

            // Remember for save() and for template behavior
            preconditionComboBox = preBox;
            inputs.put("id", preBox);

            row++;

        } else {
            // Fallback: no special fields for unknown types.
            // This keeps editor robust even if type != preconditionRef.
        }

        ////////////////////////////////////////////////////////////////////////////
        // 2. ExpressionInputPanel (buntes Feld)
        //
        // This is the DSL expression. We store it as "expressionRaw".
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
        } else {
            // Keep panel's internal default demo text. It teaches syntax.
        }

        dynamicFieldsPanel.add(expressionPanel, gbc);
        inputs.put("expressionRaw", expressionPanel);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // 3. AST preview tree (read-only)
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
        astPreviewScroll.setPreferredSize(new Dimension(300, 160));

        dynamicFieldsPanel.add(astPreviewScroll, gbc);

        row++;

        ////////////////////////////////////////////////////////////////////////////
        // 4. Live parsing:
        //    - Listen to expressionPanel editor changes
        //    - Update AST and status on each keystroke
        ////////////////////////////////////////////////////////////////////////////

        Document doc = getExpressionDocument();
        if (doc != null) {
            doc.addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void removeUpdate(DocumentEvent e) { updateAstAndStatus(); }
                public void changedUpdate(DocumentEvent e) { updateAstAndStatus(); }
            });
        }

        // First parse and render
        updateAstAndStatus();

        dynamicFieldsPanel.revalidate();
        dynamicFieldsPanel.repaint();
    }

    /**
     * Connect the preconditionComboBox (if present) with the expressionPanel.
     *
     * Behavior:
     * - If user selects the empty item:
     *     -> Clear expressionPanel text
     *     -> Set status neutral
     *     -> Refresh AST
     *
     * - If user selects a Precondition:
     *     -> Copy that Precondition's name into expressionPanel as the new expressionRaw
     *     -> Re-parse to update AST and status
     *
     * This replaces the old "Vorlagen-Dropdown", so we no longer need GivenRegistry.
     */
    private void wirePreconditionTemplateBehavior() {
        if (preconditionComboBox == null) {
            return;
        }
        preconditionComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                if (expressionPanel == null) {
                    return;
                }

                Object selObj = preconditionComboBox.getSelectedItem();
                if (!(selObj instanceof PreItem)) {
                    // defensive fallback
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    updateAstAndStatus();
                    return;
                }

                PreItem item = (PreItem) selObj;

                // Empty item (id == null) means "keine Vorlage"
                if (item.id == null) {
                    expressionPanel.setExpressionText("");
                    expressionPanel.setStatusNeutral("Kein Ausdruck");
                    updateAstAndStatus();
                    return;
                }

                // Use the Precondition name as initial expressionRaw text
                String templateName = (item.name != null) ? item.name : "";
                expressionPanel.setExpressionText(templateName);
                updateAstAndStatus();
            }
        });
    }

    /**
     * Parse expressionRaw live, show AST and validation status.
     *
     * - On valid parse: mark green.
     * - On empty     : neutral.
     * - On error     : red, and AST node shows ERROR.
     */
    private void updateAstAndStatus() {
        if (expressionPanel == null || astPreviewTree == null) {
            return;
        }

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

    /**
     * Get Document of the underlying RSyntaxTextArea in the ExpressionInputPanel
     * so we can attach a DocumentListener.
     *
     * IMPORTANT:
     * ExpressionInputPanel must provide:
     *
     *   public RSyntaxTextArea getEditor() {
     *       return editor;
     *   }
     */
    private Document getExpressionDocument() {
        if (expressionPanel == null) return null;
        return expressionPanel.getEditor().getDocument();
    }

    /**
     * Build fallback node for empty expressions.
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
     * Build error node if parsing fails.
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
     * Convert ExpressionTreeNode structure into a TreeModel for the preview JTree.
     */
    private TreeModel buildSwingModel(ExpressionTreeNode rootUiNode) {
        ExpressionTreeModelBuilder builder = new ExpressionTreeModelBuilder();
        DefaultMutableTreeNode swingRoot = builder.buildSwingTree(rootUiNode);
        return new DefaultTreeModel(swingRoot);
    }

    /**
     * Expand all rows in the preview tree for full visibility.
     */
    private void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    /**
     * Render AST labels from ExpressionTreeModelBuilder.NodePayload.
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
     * Persist current edits back into condition.value and save globally.
     *
     * We DO NOT touch condition.type here (that is owned by whoever created the GivenCondition).
     *
     * We save:
     *  - username                    -> from userBox
     *  - id (preconditionRef.id)     -> UUID or "" (from preconditionComboBox)
     *  - expressionRaw               -> text from expressionPanel
     */
    private void save() {

        Map<String, String> result = new LinkedHashMap<String, String>();

        // username
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // collect dynamic fields
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String key = entry.getKey();
            JComponent comp = entry.getValue();

            if (comp instanceof ExpressionInputPanel) {
                ExpressionInputPanel p = (ExpressionInputPanel) comp;
                result.put(key, p.getExpressionText());

            } else if (comp instanceof JComboBox) {
                JComboBox box = (JComboBox) comp;
                Object sel = box.getSelectedItem();
                if (sel instanceof PreItem) {
                    PreItem pi = (PreItem) sel;
                    // Persist ONLY the UUID or "" if no selection
                    result.put(key, pi.id == null ? "" : pi.id);
                } else {
                    // Defensive fallback
                    Object editorVal = (box.getEditor() != null)
                            ? box.getEditor().getItem()
                            : sel;
                    result.put(key, editorVal == null ? "" : String.valueOf(editorVal));
                }

            } else if (comp instanceof JTextField) {
                result.put(key, ((JTextField) comp).getText());

            } else if (comp instanceof RSyntaxTextArea) {
                result.put(key, ((RSyntaxTextArea) comp).getText());
            }
        }

        // push back into condition
        condition.setValue(serializeValueMap(result));

        // persist global test model
        TestRegistry.getInstance().save();

        JOptionPane.showMessageDialog(
                this,
                "Änderungen gespeichert.\n" +
                        "expressionRaw = " + result.get("expressionRaw")
        );
    }

    // -------------------- helper: value-map parsing/serialization --------------------

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
     * Represent one selectable Precondition in the preconditionRef.id combo.
     *
     * Behavior:
     * - id == null, name == null   -> special empty entry (no reference, no template)
     * - otherwise: name {uuid}
     *
     * Persist:
     * - Only store the UUID (id). For the empty entry store "".
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
