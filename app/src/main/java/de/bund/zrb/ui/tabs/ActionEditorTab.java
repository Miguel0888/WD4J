package de.bund.zrb.ui.tabs;

import de.bund.zrb.util.LocatorType;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keep original UI design; add filtering so that "Selector" shows only values matching the chosen "Locator Type".
 * Use enum LocatorType internally while the dropdown displays string keys (css/xpath/...).
 */
public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    public ActionEditorTab(TestAction action) {
        super("Edit Action", action);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Known actions: extend if needed
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "select", "check", "radio", "screenshot"
        ));

        // Use display keys for enum values to keep UI unchanged
        Set<String> locatorTypes = new TreeSet<String>();
        for (LocatorType t : LocatorType.values()) {
            locatorTypes.add(t.getKey());
        }

        // --- Fields ---

        formPanel.add(new JLabel("Action:"));
        JComboBox<String> actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        formPanel.add(new JLabel("Value:"));
        JComboBox<String> valueBox = new JComboBox<String>(new String[]{ "OTP" });
        valueBox.setEditable(true);
        valueBox.setSelectedItem(action.getValue());
        formPanel.add(valueBox);

        formPanel.add(new JLabel("Locator Type:"));
        JComboBox<String> locatorBox = new JComboBox<String>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);
        // Preselect current enum key or infer from selected selector if null
        String initialTypeKey = resolveInitialTypeKey(action);
        locatorBox.setSelectedItem(initialTypeKey);
        formPanel.add(locatorBox);

        formPanel.add(new JLabel("Selector:"));
        JComboBox<String> selectorBox = new JComboBox<String>();
        selectorBox.setEditable(true);
        // Fill selector options based on current locator type
        populateSelectorBoxForType(action, selectorBox, initialTypeKey);
        selectorBox.setSelectedItem(action.getSelectedSelector());
        formPanel.add(selectorBox);

        formPanel.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(UserRegistry.User::getUsername)
                .toArray(String[]::new);
        JComboBox<String> userBox = new JComboBox<String>(users);
        userBox.setSelectedItem(action.getUser());
        formPanel.add(userBox);

        formPanel.add(new JLabel("Timeout (ms):"));
        JTextField timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        add(formPanel, BorderLayout.NORTH);

        // --- Interactions ---

        // Update selector options when locator type changes
        locatorBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String typeKey = stringValue(locatorBox.getSelectedItem());
                populateSelectorBoxForType(action, selectorBox, typeKey);

                // If current selected selector does not fit the new type, preselect first candidate
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

        // --- Save button ---

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.setAction(stringValue(actionBox.getSelectedItem()));
                action.setValue(stringValue(valueBox.getSelectedItem()));

                String typeKey = stringValue(locatorBox.getSelectedItem());
                LocatorType enumType = LocatorType.fromKey(typeKey);
                action.setLocatorType(enumType); // use enum-based setter

                String selector = stringValue(selectorBox.getSelectedItem());
                if (!isSelectorValidForType(typeKey, selector)) {
                    JOptionPane.showMessageDialog(ActionEditorTab.this,
                            "Selector passt nicht zum Locator Type (" + typeKey + ").",
                            "Validierung",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                action.setSelectedSelector(selector);

                action.setUser(stringValue(userBox.getSelectedItem()));
                try {
                    action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
                } catch (NumberFormatException ignored) { /* Keep previous value */ }

                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(ActionEditorTab.this, "Änderungen gespeichert.");
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }

    // ---------- Helpers ----------

    private String resolveInitialTypeKey(TestAction action) {
        // Prefer explicit enum on action
        if (action.getLocatorType() != null) {
            return action.getLocatorType().getKey();
        }
        // Infer from selected selector shape if possible
        String sel = action.getSelectedSelector();
        if (sel != null) {
            String s = sel.trim();
            if (looksLikeXpath(s)) return LocatorType.XPATH.getKey();
            if (s.startsWith("#")) return LocatorType.ID.getKey();
            // Heuristic: default to css if not xpath
            return LocatorType.CSS.getKey();
        }
        // Fallback: pick the first available locator from map with preference
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

    private void populateSelectorBoxForType(TestAction action, JComboBox<String> selectorBox, String typeKey) {
        // Clear and refill only with matching selector(s)
        selectorBox.removeAllItems();

        // Primary candidate: the stored locator for this type key
        String candidate = action.getLocators().get(typeKey);
        if (candidate != null && candidate.trim().length() > 0) {
            addIfAbsent(selectorBox, candidate);
        }

        // Also add the current selectedSelector if it fits the chosen type and is not already included
        String currentSelected = action.getSelectedSelector();
        if (currentSelected != null && currentSelected.trim().length() > 0) {
            if (isSelectorValidForType(typeKey, currentSelected)) {
                addIfAbsent(selectorBox, currentSelected);
            }
        }

        // Safety net: If nothing matches, keep an empty editable entry so users can type
        if (selectorBox.getItemCount() == 0) {
            selectorBox.addItem("");
        }

        // Select first item if nothing is selected yet
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
        return o == null ? "" : String.valueOf(o);
    }

    // Very lightweight validation per type; keep permissive to avoid blocking users.
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
                // Accept "#id" or bare id without spaces and not xpath
                return s.startsWith("#") || (!s.contains(" ") && !s.startsWith("/") && !s.startsWith("("));
            case TEXT:
            case LABEL:
            case PLACEHOLDER:
            case ALTTEXT:
            case ROLE:
                // Accept any non-empty; format is enforced on playback/resolution step
                return true;
            default:
                return true;
        }
    }

    private boolean looksLikeXpath(String s) {
        String t = s.trim();
        return t.startsWith("//") || t.startsWith(".//") || t.startsWith("/") || t.startsWith("(");
    }
}
