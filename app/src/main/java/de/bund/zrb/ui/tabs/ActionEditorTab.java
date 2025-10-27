package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.ParameterRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.expressions.ExpressionSelectionListener;
import de.bund.zrb.ui.expressions.ExpressionTreeComboBox;
import de.bund.zrb.ui.expressions.ExpressionTreeNode;
import de.bund.zrb.util.LocatorType;
import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Edit a single TestAction.
 * Keep UI decoupled from playback logic:
 * - Let user pick an action type (click/fill/...).
 * - Let user pick or edit selector and value template.
 * - Sync changes back into TestAction.
 *
 * Value handling (NEW):
 * - Instead of a plain JComboBox<String>, present an expression-aware dropdown.
 * - The dropdown shows an expression AST as a tree (indented).
 * - User clicks any node (Variable, Function, Composite, ...) to select it.
 * - The chosen node can later be evaluated at runtime.
 *
 * Test mode:
 * - Use dummy AST data: OTP(username) and Uhrzeit.
 * - Store the chosen node label back into action.setValue(...) when "Speichern" is pressed.
 *
 * Next step (not done here):
 * - Instead of saving only the label, store the chosen ResolvableExpression in ScenarioState
 *   under a logical key, so the When-step can resolve it lazily.
 */
public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    // Keep reference to what user picked in the expression dropdown
    private ResolvableExpression chosenExpression;
    private String chosenExpressionLabel;

    public ActionEditorTab(final TestAction action) {
        super("Edit Action", action);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Build dropdown data
        ////////////////////////////////////////////////////////////////////////////

        // Known/best-effort actions. Extend as recorder learns more.
        // (Add navigate, wait, fill, type, press etc. so the user sees common actions.)
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

        // Old value model (string-based). Keep method for future fallback,
        // but we will not use it anymore:
        // DefaultComboBoxModel<String> valueModel = buildValueModel(action);


        ////////////////////////////////////////////////////////////////////////////
        // UI fields
        ////////////////////////////////////////////////////////////////////////////

        // Action
        formPanel.add(new JLabel("Action:"));
        final JComboBox<String> actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        // Value (AST-driven dropdown instead of plain text model)
        formPanel.add(new JLabel("Value:"));

        // Create the special combo-like component that shows a tree in a popup
        final ExpressionTreeComboBox valueBox = new ExpressionTreeComboBox();

        // Build dummy AST and connect it to the dropdown
        // (In real code this would come from ScenarioState.getRootExpression() etc.)
        ExpressionTreeNode uiRoot = buildDummyExpressionTreeNode();
        valueBox.setTreeRoot(uiRoot);

        // Pre-fill display with whatever the action had before,
        // so the user sees existing state when editing.
        chosenExpressionLabel = action.getValue() != null ? action.getValue() : "";
        // NOTE: chosenExpression stays null until user actively picks from tree,
        // which is fine for a first test run.

        if (chosenExpressionLabel != null && chosenExpressionLabel.trim().length() > 0) {
            // Show previous label in the UI field to preserve user's context.
            // We simulate an already chosen node label by directly setting text in the field.
            // (Hack: reuse combo's internal property contract)
            // -> This keeps the preview consistent with the rest of the component's API.
            trySetInitialSelection(valueBox, chosenExpressionLabel);
        }

        // Listen for user selection from the popup tree
        valueBox.addSelectionListener(new ExpressionSelectionListener() {
            public void onExpressionSelected(String chosenLabel,
                                             ResolvableExpression chosenExpr) {
                // Store for saving
                chosenExpression = chosenExpr;
                chosenExpressionLabel = chosenLabel;
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

        // User
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
        ////////////////////////////////////////////////////////////////////////////

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                // Action
                action.setAction(stringValue(actionBox.getSelectedItem()));

                // Value template:
                // Previously: action.setValue(stringValue(valueBox.getSelectedItem()))
                // Now: take the label of the chosen AST node.
                // Intent: Express which AST node the user bound to this TestAction.
                action.setValue(chosenExpressionLabel != null ? chosenExpressionLabel : "");

                // Locator type
                String typeKey = stringValue(locatorBox.getSelectedItem());
                LocatorType enumType = LocatorType.fromKey(typeKey);
                action.setLocatorType(enumType);

                // Selector
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

                // User
                action.setUser(stringValue(userBox.getSelectedItem()));

                // Timeout
                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) {
                    // Keep previous value if parsing fails
                }

                // Persist test data
                TestRegistry.getInstance().save();

                JOptionPane.showMessageDialog(ActionEditorTab.this, "Änderungen gespeichert.\n" +
                        "Gewählter Ausdruck: " + chosenExpressionLabel);
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Hilfsmethoden
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a dummy AST and wrap it as ExpressionTreeNode for preview/testing.
     *
     * Intent:
     * - Provide stable test data independent of ScenarioState.
     * - Show how nested nodes appear.
     *
     * This mimics something like:
     *   Composite(
     *     Function OTP( Variable username ),
     *     Variable Uhrzeit
     *   )
     *
     * Visuell im Tree siehst du dann:
     *   - Composite
     *     - Function: OTP
     *       - Variable: username
     *     - Variable: Uhrzeit
     */
    private ExpressionTreeNode buildDummyExpressionTreeNode() {
        // Build Variable username
        VariableExpression usernameVar = new VariableExpression("username");

        // Build Function OTP(username)
        java.util.List<ResolvableExpression> otpArgs = new ArrayList<ResolvableExpression>();
        otpArgs.add(usernameVar);
        FunctionExpression otpFunc = new FunctionExpression("OTP", otpArgs);

        // Build Variable Uhrzeit
        VariableExpression timeVar = new VariableExpression("Uhrzeit");

        // Build Composite [ OTP(username), Uhrzeit ]
        java.util.List<ResolvableExpression> parts = new ArrayList<ResolvableExpression>();
        parts.add(otpFunc);
        parts.add(timeVar);
        CompositeExpression root = new CompositeExpression(parts);

        // Wrap root expression into a UI tree node
        return ExpressionTreeNode.fromExpression(root);
    }

    /**
     * Pre-populate the ExpressionTreeComboBox's display text with a previous value.
     *
     * Intent:
     * - Simulate that the user had already picked something earlier, even
     *   if they haven't clicked in this session yet.
     *
     * This method does not set a chosenExpression ResolvableExpression on purpose,
     * because we cannot safely guess which AST node matches the previous string.
     * We only show the label to avoid confusing empty state.
     */
    private void trySetInitialSelection(ExpressionTreeComboBox combo, String label) {
        // Hack the "public API" we defined in ExpressionTreeComboBox:
        // We write the text and client properties the same way fireSelection() would.
        // This keeps everything consistent with getSelectedLabel().
        JTextField f = getComboDisplayField(combo);
        if (f != null) {
            f.setText(label);
            f.putClientProperty("chosenExpr", null);
            f.putClientProperty("chosenLabel", label);
        }
    }

    /**
     * Extract the internal, non-editable display field from ExpressionTreeComboBox
     * in order to preset it. This is a bit of an integration hack for test/demo mode.
     *
     * IMPORTANT:
     * - Keep this method only for demo integration.
     * - In production, add a proper setter method to ExpressionTreeComboBox instead.
     */
    private JTextField getComboDisplayField(ExpressionTreeComboBox combo) {
        // ExpressionTreeComboBox is a JPanel(BorderLayout) with CENTER = JTextField, EAST = JButton.
        // We iterate its children to find the JTextField. This avoids reflection.
        Component[] children = combo.getComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof JTextField) {
                return (JTextField) children[i];
            }
        }
        return null;
    }

    /**
     * Build the dropdown model for the "old" string-based Value field.
     * Left in place for reference / fallback, but not used anymore.
     */
    private DefaultComboBoxModel<String> buildValueModel(TestAction action) {
        java.util.List<String> items = new ArrayList<String>();

        // 1. Empty entry -> user can type a literal fixed value
        items.add("");

        // 2. OTP placeholder
        items.add("{{OTP}}");

        // 3. All known parameters from registry, as placeholders
        java.util.List<String> params = ParameterRegistry.getInstance().getAllParameterNames();
        for (String p : params) {
            if (p != null && p.trim().length() > 0) {
                String placeholder = "{{" + p.trim() + "}}";
                addIfAbsent(items, placeholder);
            }
        }

        // 4. Current action value (template or literal), keep it selectable
        String curVal = action.getValue();
        if (curVal != null && curVal.trim().length() > 0) {
            addIfAbsent(items, curVal);
        }

        DefaultComboBoxModel<String> model =
                new DefaultComboBoxModel<String>(items.toArray(new String[0]));
        return model;
    }

    private void addIfAbsent(java.util.List<String> list, String value) {
        for (String s : list) {
            if (s.equals(value)) {
                return;
            }
        }
        list.add(value);
    }

    private void addIfAbsent(JComboBox<String> box, String value) {
        int count = box.getItemCount();
        for (int i = 0; i < count; i++) {
            Object it = box.getItemAt(i);
            if (value.equals(it)) return;
        }
        box.addItem(value);
    }

    private String resolveInitialTypeKey(TestAction action) {
        // Prefer explicit enum
        if (action.getLocatorType() != null) {
            return action.getLocatorType().getKey();
        }

        // Infer from selector if possible
        String sel = action.getSelectedSelector();
        if (sel != null) {
            String s = sel.trim();
            if (looksLikeXpath(s)) return LocatorType.XPATH.getKey();
            if (s.startsWith("#")) return LocatorType.ID.getKey();
            // Default to css
            return LocatorType.CSS.getKey();
        }

        // Fallback heuristics from recorded locators
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

        // Also allow the currently selected selector if it fits
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
                // Allow anything non-empty; deeper validation happens at playback.
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
}
