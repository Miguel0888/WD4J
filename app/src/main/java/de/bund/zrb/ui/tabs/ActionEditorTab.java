package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.expressions.ExpressionSelectionListener;
import de.bund.zrb.ui.expressions.ExpressionTreeComboBox;
import de.bund.zrb.ui.expressions.ExpressionTreeModelBuilder;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.util.LocatorType;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Edit a single TestAction.
 *
 * Intent:
 * - Bind this action (typically a When-step like "fill field X") to a specific dynamic value.
 * - That value is not typed in manually anymore. Instead, it comes from any Given in the same test.
 *
 * UX:
 * - The "Value:" control is an ExpressionTreeComboBox.
 * - It shows all expressions and subexpressions coming from all GivenConditions of this test.
 * - The user opens the dropdown, sees a tree:
 *      Givens
 *        Given #1: Es existiert eine {{Belegnummer}}.
 *          Literal: "Es existiert eine "
 *          Variable: Belegnummer
 *          Literal: "."
 *        Given #2: Der Benutzer hat einen OTP-Code {{OTP({{username}})}}.
 *          Function: OTP
 *            Variable: username
 * - Clicking any node selects it.
 *
 * Runtime idea:
 * - We store which node was chosen (chosenExpressionLabel + chosenExpression AST node).
 * - Later during playback we evaluate that chosen node lazily (Supplier etc.).
 *
 * Clean Code / Architektur:
 * - ActionEditorTab does NOT evaluate anything.
 * - ActionEditorTab only lets the tester *bind* an action to a specific AST node.
 * - Evaluation happens later in the runtime layer, not in the UI.
 */
public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    /** Remember what the user actually picked from the tree */
    private ResolvableExpression chosenExpression;
    private String chosenExpressionLabel;

    /** We also need the Givens of this scenario/test/suite */
    private final java.util.List<GivenCondition> givens;

    public ActionEditorTab(final TestAction action,
                           final java.util.List<GivenCondition> givensForThisAction) {
        super("Edit Action", action);
        this.givens = (givensForThisAction != null)
                ? givensForThisAction
                : java.util.Collections.<GivenCondition>emptyList();

        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Build dropdown data for "Action" and "Locator Type"
        ////////////////////////////////////////////////////////////////////////////

        // Known/best-effort actions (extend as needed)
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "fill", "type", "press",
                "select", "check", "radio",
                "navigate", "wait", "screenshot"
        ));

        // All available locator type keys (css, xpath, id, ...)
        Set<String> locatorTypes = new TreeSet<String>();
        for (LocatorType t : LocatorType.values()) {
            locatorTypes.add(t.getKey());
        }

        ////////////////////////////////////////////////////////////////////////////
        // UI fields
        ////////////////////////////////////////////////////////////////////////////

        // Action
        formPanel.add(new JLabel("Action:"));
        final JComboBox<String> actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        // Value
        formPanel.add(new JLabel("Value:"));

        // Our special "tree dropdown"
        final ExpressionTreeComboBox valueBox = new ExpressionTreeComboBox();

        // Build AST tree root from ALL givens of this test
        ExpressionTreeNode uiRoot = buildTreeFromAllGivens(this.givens);

        // Attach tree to combo
        valueBox.setTreeRoot(uiRoot);

        // Pre-fill UI field with whatever was already chosen before (action.getValue())
        chosenExpressionLabel = (action.getValue() != null) ? action.getValue() : "";
        if (chosenExpressionLabel != null && chosenExpressionLabel.trim().length() > 0) {
            trySetInitialSelection(valueBox, chosenExpressionLabel);
        }

        // React to user picking a node from the dropdown tree
        valueBox.addSelectionListener(new ExpressionSelectionListener() {
            public void onExpressionSelected(String label, ResolvableExpression expr) {
                // Keep both label and AST node so we can persist and later evaluate
                chosenExpressionLabel = label;
                chosenExpression = expr;
            }
        });

        formPanel.add(valueBox);

        // Locator Type
        formPanel.add(new JLabel("Locator Type:"));
        final JComboBox<String> locatorBox = new JComboBox<String>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);

        // Preselect current enum key or infer from selector
        final String initialTypeKey = resolveInitialTypeKey(action);
        locatorBox.setSelectedItem(initialTypeKey);
        formPanel.add(locatorBox);

        // Selector
        formPanel.add(new JLabel("Selector:"));
        final JComboBox<String> selectorBox = new JComboBox<String>();
        selectorBox.setEditable(true);

        populateSelectorBoxForType(action, selectorBox, initialTypeKey);
        selectorBox.setSelectedItem(action.getSelectedSelector());
        formPanel.add(selectorBox);

        // User (which account performs the action at playback)
        formPanel.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(UserRegistry.User::getUsername)
                .toArray(String[]::new);
        final JComboBox<String> userBox = new JComboBox<String>(users);
        userBox.setSelectedItem(action.getUser());
        formPanel.add(userBox);

        // Timeout
        formPanel.add(new JLabel("Timeout (ms):"));
        final JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        add(formPanel, BorderLayout.NORTH);

        ////////////////////////////////////////////////////////////////////////////
        // Interaktionen
        ////////////////////////////////////////////////////////////////////////////

        // Update selector dropdown when locator type changes
        locatorBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String typeKey = stringValue(locatorBox.getSelectedItem());
                populateSelectorBoxForType(action, selectorBox, typeKey);

                // If current selected selector no longer matches, fallback to first candidate
                String currentSel = stringValue(selectorBox.getEditor().getItem());
                if (!isSelectorValidForType(typeKey, currentSel)) {
                    if (selectorBox.getItemCount() > 0) {
                        selectorBox.setSelectedIndex(0);
                    } else {
                        selectorBox.setSelectedItem("");
                    }
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////
        // Speichern-Button
        //
        // Intent:
        // - Persist user choices directly into the TestAction
        // - chosenExpressionLabel becomes action.value (so runtime knows which node we meant)
        ////////////////////////////////////////////////////////////////////////////

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                // Actiontyp setzen
                action.setAction(stringValue(actionBox.getSelectedItem()));

                // WICHTIG:
                // Speichere ab jetzt das echte Template, nicht die UI-Beschreibung.
                // Beispiel: "{{username}}" oder "{{OTP({{username}})}}"
                if (chosenExpression != null) {
                    String template = de.bund.zrb.runtime.ExpressionTemplateRenderer.render(chosenExpression);
                    action.setValue(template);
                } else {
                    // Wenn der User nichts aus dem Tree gewählt hat, behalten wir das alte oder setzen leer
                    action.setValue(action.getValue() != null ? action.getValue() : "");
                }

                // Rest wie gehabt
                String typeKey = stringValue(locatorBox.getSelectedItem());
                LocatorType enumType = LocatorType.fromKey(typeKey);
                action.setLocatorType(enumType);

                String selector = stringValue(selectorBox.getSelectedItem());
                if (!isSelectorValidForType(typeKey, selector)) {
                    JOptionPane.showMessageDialog(
                            ActionEditorTab.this,
                            "Selector passt nicht zum Locator Type (" + typeKey + ").",
                            "Validierung",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                action.setSelectedSelector(selector);

                action.setUser(stringValue(userBox.getSelectedItem()));

                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) {
                    // keep previous
                }

                TestRegistry.getInstance().save();

                JOptionPane.showMessageDialog(
                        ActionEditorTab.this,
                        "Änderungen gespeichert.\nTemplate ist jetzt:\n" + action.getValue()
                );
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // AST-Baum aus allen Givens bauen
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Build a single root ExpressionTreeNode that groups all ASTs
     * derived from the GivenConditions of this test.
     *
     * Steps:
     * - For each GivenCondition:
     *      * extract expressionRaw from its value map
     *      * parse expressionRaw into a ResolvableExpression via ExpressionParser
     *      * wrap it with ExpressionTreeNode.fromExpression(...)
     *      * prefix with a friendly "Given #i: ..." label, so the user sees context
     *
     * - Then create a synthetic "Givens" root node that has all those as children.
     *
     * Error handling:
     * - If parsing fails for a Given, create an ERROR leaf so it's still visible in the tree.
     * - If there are no Givens or no expressions, fall back to a "No data" node.
     */
    private ExpressionTreeNode buildTreeFromAllGivens(java.util.List<GivenCondition> givens) {

        java.util.List<ExpressionTreeNode> children = new ArrayList<ExpressionTreeNode>();

        for (int i = 0; i < givens.size(); i++) {
            GivenCondition gc = givens.get(i);

            // Hole expressionRaw=... aus gc.getValue()
            Map<String, Object> valueMap = parseValueMap(gc.getValue());
            String raw = asString(valueMap.get("expressionRaw"));

            if (raw == null || raw.trim().length() == 0) {
                // Kein Ausdruck -> zeige Hinweis
                ExpressionTreeNode child = buildNoDataNode("Given #" + (i + 1) + ": (kein expressionRaw)");
                children.add(child);
                continue;
            }

            try {
                // Parse den Ausdruck aus dem Given
                ExpressionParser parser = new ExpressionParser();
                ResolvableExpression expr = parser.parseTemplate(raw);

                // Baue UI-Knoten für den Parsed AST
                ExpressionTreeNode givenRoot = ExpressionTreeNode.fromExpression(expr);

                // Hänge einen "Wrapper"-Knoten davor, damit der User erkennt,
                // aus welchem Given das stammt.
                String preview = summarizeExpression(raw);
                ExpressionTreeNode wrapped = wrapUnderLabel(
                        "Given #" + (i + 1) + ": " + preview,
                        givenRoot
                );
                children.add(wrapped);

            } catch (Exception ex) {
                // Parsefehler -> Knoten mit ERROR-Hinweis
                ExpressionTreeNode errNode = buildErrorNode(
                        "Given #" + (i + 1) + ": PARSE ERROR",
                        raw,
                        ex.getMessage()
                );
                children.add(errNode);
            }
        }

        if (children.isEmpty()) {
            // Fallback wenn gar keine Givens oder alle leer
            return buildNoDataNode("Givens: No data");
        }

        // Erzeuge künstlichen Root "Givens"
        return new ExpressionTreeNode(
                null, // no direct ResolvableExpression for the synthetic root
                "Givens",
                children
        );
    }

    /**
     * Wrap a single ExpressionTreeNode under a new parent node that has
     * a friendly label.
     *
     * Example:
     * parentLabel = "Given #1: Es existiert eine {{Belegnummer}}."
     * child = (Composite -> Literal + Variable + Literal)
     *
     * Result:
     *   Given #1: Es existiert eine {{Belegnummer}}.
     *     Composite
     *       Literal: "Es existiert eine "
     *       Variable: Belegnummer
     *       Literal: "."
     */
    private ExpressionTreeNode wrapUnderLabel(String parentLabel,
                                              ExpressionTreeNode childNode) {
        java.util.List<ExpressionTreeNode> singleChild = new ArrayList<ExpressionTreeNode>();
        singleChild.add(childNode);

        return new ExpressionTreeNode(
                null,
                parentLabel,
                singleChild
        );
    }

    /**
     * Build a tiny "No data" node or info node.
     */
    private ExpressionTreeNode buildNoDataNode(String label) {
        LiteralExpression lit = new LiteralExpression("No data");
        return new ExpressionTreeNode(
                lit,
                label,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        );
    }

    /**
     * Build a node describing a parse error for a Given.
     */
    private ExpressionTreeNode buildErrorNode(String headline,
                                              String raw,
                                              String msg) {
        LiteralExpression lit = new LiteralExpression("Parse Error: " + msg);
        java.util.List<ExpressionTreeNode> kids = new ArrayList<ExpressionTreeNode>();

        kids.add(new ExpressionTreeNode(
                null,
                "Raw: " + raw,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        ));
        kids.add(new ExpressionTreeNode(
                null,
                "Error: " + msg,
                java.util.Collections.<ExpressionTreeNode>emptyList()
        ));

        return new ExpressionTreeNode(
                lit,
                headline,
                kids
        );
    }

    /**
     * Create a short preview for the Given headline.
     * Limit length so the dropdown stays readable.
     */
    private String summarizeExpression(String raw) {
        String s = raw.trim().replace("\n", " ");
        int max = 60;
        if (s.length() > max) {
            s = s.substring(0, max) + "...";
        }
        return s;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Helper to preset the visible text of ExpressionTreeComboBox
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Simulate a pre-selection in the combo's display field, so when you re-open
     * an action you still see what was previously chosen.
     *
     * This does not "rebind" chosenExpression (because wir nicht sicher wissen,
     * welcher AST-Knoten das genau war), aber es zeigt den String wieder an.
     */
    private void trySetInitialSelection(ExpressionTreeComboBox combo, String label) {
        JTextField field = getComboDisplayField(combo);
        if (field != null) {
            field.setText(label);
            field.putClientProperty("chosenExpr", null);
            field.putClientProperty("chosenLabel", label);
        }
    }

    /**
     * Find the display JTextField inside ExpressionTreeComboBox.
     * ExpressionTreeComboBox ist ein JPanel(BorderLayout) mit:
     *  - CENTER = JTextField (read-only display)
     *  - EAST   = JButton (opens popup)
     */
    private JTextField getComboDisplayField(ExpressionTreeComboBox combo) {
        Component[] children = combo.getComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof JTextField) {
                return (JTextField) children[i];
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Locator / selector helpers (unchanged logic)
    ////////////////////////////////////////////////////////////////////////////////

    private String resolveInitialTypeKey(TestAction action) {
        // Prefer explicit enum
        if (action.getLocatorType() != null) {
            return action.getLocatorType().getKey();
        }

        // Infer from selector
        String sel = action.getSelectedSelector();
        if (sel != null) {
            String s = sel.trim();
            if (looksLikeXpath(s)) return LocatorType.XPATH.getKey();
            if (s.startsWith("#")) return LocatorType.ID.getKey();
            // Default css
            return LocatorType.CSS.getKey();
        }

        // Fallback heuristics
        if (hasLocator(action, "role")) return LocatorType.ROLE.getKey();
        if (hasLocator(action, "text")) return LocatorType.TEXT.getKey();
        if (hasLocator(action, "xpath")) return LocatorType.XPATH.getKey();
        if (hasLocator(action, "css")) return LocatorType.CSS.getKey();
        if (hasLocator(action, "id")) return LocatorType.ID.getKey();
        if (hasLocator(action, "label")) return LocatorType.LABEL.getKey();
        if (hasLocator(action, "placeholder")) return LocatorType.PLACEHOLDER.getKey();
        if (hasLocator(action, "altText")) return LocatorType.ALTTEXT.getKey();

        return LocatorType.CSS.getKey();
    }

    private boolean hasLocator(TestAction action, String key) {
        String v = action.getLocators().get(key);
        return v != null && v.trim().length() > 0;
    }

    private void populateSelectorBoxForType(TestAction action,
                                            JComboBox<String> selectorBox,
                                            String typeKey) {

        selectorBox.removeAllItems();

        // Primary candidate: locator stored for this type key
        String candidate = action.getLocators().get(typeKey);
        if (candidate != null && candidate.trim().length() > 0) {
            addIfAbsent(selectorBox, candidate);
        }

        // Also allow current selection if it fits
        String currentSelected = action.getSelectedSelector();
        if (currentSelected != null && currentSelected.trim().length() > 0) {
            if (isSelectorValidForType(typeKey, currentSelected)) {
                addIfAbsent(selectorBox, currentSelected);
            }
        }

        // Safety net: always offer empty editable entry
        if (selectorBox.getItemCount() == 0) {
            selectorBox.addItem("");
        }

        // Preselect first item if nothing else is selected
        if (selectorBox.getSelectedItem() == null && selectorBox.getItemCount() > 0) {
            selectorBox.setSelectedIndex(0);
        }
    }

    private void addIfAbsent(JComboBox<String> box, String value) {
        int count = box.getItemCount();
        for (int i = 0; i < count; i++) {
            Object it = box.getItemAt(i);
            if (value.equals(it)) return;
        }
        box.addItem(value);
    }

    private String stringValue(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    /**
     * Keep validation permissive. Only stop the user if it's obviously broken.
     */
    private boolean isSelectorValidForType(String typeKey, String selector) {
        if (selector == null) return false;
        String s = selector.trim();
        if (s.length() == 0) return false;

        LocatorType type = LocatorType.fromKey(typeKey);
        if (type == null) return true; // be permissive

        switch (type) {
            case XPATH:
                return looksLikeXpath(s);
            case CSS:
                return !looksLikeXpath(s);
            case ID:
                // Accept "#id" or bare id without spaces and not xpath-like
                return s.startsWith("#")
                        || (!s.contains(" ")
                        && !s.startsWith("/")
                        && !s.startsWith("("));
            case TEXT:
            case LABEL:
            case PLACEHOLDER:
            case ALTTEXT:
            case ROLE:
                // Accept anything non-empty; deeper validation happens at playback.
                return true;
            default:
                return true;
        }
    }

    private boolean looksLikeXpath(String s) {
        String t = s.trim();
        return t.startsWith("//")
                || t.startsWith(".//")
                || t.startsWith("/")
                || t.startsWith("(");
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Tiny parsing helpers for GivenCondition.value (key=value&key=value...)
    ////////////////////////////////////////////////////////////////////////////////

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

    private String asString(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
