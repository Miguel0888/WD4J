package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.expressions.GivenLookupService;
import de.bund.zrb.ui.expressions.ScopeReferenceComboBox;
import de.bund.zrb.util.LocatorType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Edit a single TestAction.
 *
 * Arbeitspaket 1 Version:
 * - "Value:" zeigt jetzt die ScopeReferenceComboBox.
 * - Die Combo listet alle Variablen/BeforeAll/BeforeEach/Templates,
 *   die im Scope (Case->Suite->Root) sichtbar sind.
 * - Beim Speichern wird einfach der gewählte Name (inkl. Präfix) in action.setValue() geschrieben.
 *
 * Noch NICHT implementiert:
 * - dynamische Expression-Auswertung
 * - Supplier/Funktion bauen
 */
public class ActionEditorTab extends AbstractEditorTab<TestAction> {

    private final TestAction action;

    // für's Speichern
    private String chosenNameFromScope;

    public ActionEditorTab(final TestAction action) {
        super("Edit Action", action);
        this.action = action;

        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ////////////////////////////////////////////////////////////////////////////
        // Build dropdown data for "Action" and "Locator Type"
        ////////////////////////////////////////////////////////////////////////////

        // Bekannte Action-Typen (kannst du gerne erweitern)
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "fill", "type", "press",
                "select", "check", "radio",
                "navigate", "wait", "screenshot"
        ));

        // Alle LocatorType-Keys
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

        // Value (NEU: ScopeReferenceComboBox)
        formPanel.add(new JLabel("Value:"));

        final ScopeReferenceComboBox valueCombo = new ScopeReferenceComboBox();

        // baue ScopeData für diese Action
        GivenLookupService lookup = new GivenLookupService(TestRegistry.getInstance());
        GivenLookupService.ScopeData scopeData = lookup.buildScopeDataForAction(action);
        valueCombo.setScopeData(scopeData);

        // Falls die Action schon einen Wert hat (z.B. "username" oder "*otpCode"),
        // diesen sichtbar machen:
        if (action.getValue() != null && action.getValue().trim().length() > 0) {
            valueCombo.presetSelection(action.getValue().trim());
            chosenNameFromScope = action.getValue().trim();
        }

        // Listener: User hat was Neues gewählt
        valueCombo.addSelectionListener(new ScopeReferenceComboBox.SelectionListener() {
            @Override
            public void onSelected(String nameWithPrefix) {
                chosenNameFromScope = nameWithPrefix;
            }
        });

        formPanel.add(valueCombo);

        // Locator Type
        formPanel.add(new JLabel("Locator Type:"));
        final JComboBox<String> locatorBox = new JComboBox<String>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);

        // Preselect current enum key or infer
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

        // LocatorType-Wechsel aktualisiert mögliche Selector-Einträge
        locatorBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String typeKey = stringValue(locatorBox.getSelectedItem());
                populateSelectorBoxForType(action, selectorBox, typeKey);

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
        // AP1: wir speichern NICHT die aufgelöste Expression,
        // sondern nur den gewählten Namen/Template mit Präfix.
        ////////////////////////////////////////////////////////////////////////////

        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                // Actiontyp ins Modell zurückschreiben
                action.setAction(stringValue(actionBox.getSelectedItem()));

                // Value: einfach der gewählte Name (inkl. Präfix falls * oder ①)
                // Wenn der User nichts neu gewählt hat, behalten wir was schon drin stand.
                if (chosenNameFromScope != null && chosenNameFromScope.trim().length() > 0) {
                    action.setValue(chosenNameFromScope.trim());
                } else {
                    // falls leer, dann ggf. leer schreiben
                    if (action.getValue() == null) {
                        action.setValue("");
                    }
                }

                // LocatorType etc.
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
                    // lass bisherigen Timeout stehen
                }

                TestRegistry.getInstance().save();

                JOptionPane.showMessageDialog(
                        ActionEditorTab.this,
                        "Änderungen gespeichert.\nValue ist jetzt:\n" + action.getValue()
                );
            }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
    }

    // ========================================================================
    // Hilfsfunktionen aus deiner alten Version bleiben größtenteils gleich
    // ========================================================================

    private String resolveInitialTypeKey(TestAction action) {
        if (action.getLocatorType() != null) {
            return action.getLocatorType().getKey();
        }

        String sel = action.getSelectedSelector();
        if (sel != null) {
            String s = sel.trim();
            if (looksLikeXpath(s)) return LocatorType.XPATH.getKey();
            if (s.startsWith("#")) return LocatorType.ID.getKey();
            return LocatorType.CSS.getKey();
        }

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

    private boolean hasLocator(TestAction a, String key) {
        String v = a.getLocators().get(key);
        return v != null && v.trim().length() > 0;
    }

    private void populateSelectorBoxForType(TestAction action,
                                            JComboBox<String> selectorBox,
                                            String typeKey) {

        selectorBox.removeAllItems();

        String candidate = action.getLocators().get(typeKey);
        if (candidate != null && candidate.trim().length() > 0) {
            addIfAbsent(selectorBox, candidate);
        }

        String currentSel = action.getSelectedSelector();
        if (currentSel != null && currentSel.trim().length() > 0) {
            if (isSelectorValidForType(typeKey, currentSel)) {
                addIfAbsent(selectorBox, currentSel);
            }
        }

        if (selectorBox.getItemCount() == 0) {
            selectorBox.addItem("");
        }

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

    private boolean isSelectorValidForType(String typeKey, String selector) {
        if (selector == null) return false;
        String s = selector.trim();
        if (s.length() == 0) return false;

        LocatorType type = LocatorType.fromKey(typeKey);
        if (type == null) return true;

        switch (type) {
            case XPATH:
                return looksLikeXpath(s);
            case CSS:
                return !looksLikeXpath(s);
            case ID:
                return s.startsWith("#")
                        || (!s.contains(" ")
                        && !s.startsWith("/")
                        && !s.startsWith("("));
            case TEXT:
            case LABEL:
            case PLACEHOLDER:
            case ALTTEXT:
            case ROLE:
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
