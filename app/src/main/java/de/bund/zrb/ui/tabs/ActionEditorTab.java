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
import java.util.List;

/**
 * Edit a single TestAction.
 *
 * Neu:
 * - Statt ExpressionTreeComboBox (AST) nutzen wir ScopeReferenceComboBox.
 * - Die Combo zeigt Variablen- und Template-Namen aus dem Scope.
 *   * username            -> "{{username}}"
 *   * *otpCode            -> "{{otpCode()}}"
 *
 * Speichern schreibt:
 *   - action.setValue(<aktuelles Template aus dem Value-Feld>)
 *   - action.setLocatorType(...)
 *   - action.setSelectedSelector(...)
 *   - action.setUser(...)
 *   - action.setTimeout(...)
 *   - TestRegistry.save()
 *
 * Alles andere (LocatorType-Handling etc.) bleibt wie gehabt.
 */
public class ActionEditorTab extends JPanel {

    private final TestAction action;
    private final List<GivenCondition> givens;

    // UI Felder
    private final JComboBox<String> actionBox;
    private final JComboBox<String> locatorBox;
    private final JComboBox<String> selectorBox;
    private final JComboBox<String> userBox;
    private final JTextField timeoutField;

    // Neues Value-UI:
    // - scopeCombo: Auswahl aus Scope
    // - valueField: zeigt resultierendes Template ({{username}} / {{otpCode()}})
    private final ScopeReferenceComboBox scopeCombo;
    private final JTextField valueField;

    public ActionEditorTab(final TestAction action,
                           final List<GivenCondition> givensForThisAction) {
        super(new BorderLayout());
        this.action = action;
        this.givens = (givensForThisAction != null)
                ? givensForThisAction
                : Collections.<GivenCondition>emptyList();

        // ---------- Panel-Layout ----------
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(formPanel, BorderLayout.NORTH);

        // ---------- Bekannte Actions ----------
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "fill", "type", "press",
                "select", "check", "radio",
                "navigate", "wait", "screenshot"
        ));

        // ---------- LocatorTypes ----------
        Set<String> locatorTypes = new TreeSet<String>();
        for (LocatorType t : LocatorType.values()) {
            locatorTypes.add(t.getKey());
        }

        // ---------- Action ----------
        formPanel.add(new JLabel("Action:"));
        actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        // ---------- Value ----------
        formPanel.add(new JLabel("Value:"));

        // Container für Value: Textfeld + Combo
        JPanel valuePanel = new JPanel(new BorderLayout(4, 0));

        valueField = new JTextField();
        valueField.setEditable(false);
        valueField.setText(action.getValue() != null ? action.getValue() : "");
        valuePanel.add(valueField, BorderLayout.CENTER);

        scopeCombo = new ScopeReferenceComboBox();
        valuePanel.add(scopeCombo, BorderLayout.EAST);

        // ScopeData aus LookupService aufbauen (Variables/Templates)
        GivenLookupService.ScopeData scopeData =
                new GivenLookupService().collectScopeForAction(action);
        scopeCombo.setScopeData(scopeData);

        // Wenn der User in der Combo etwas auswählt,
        // setzen wir daraus das Template in valueField UND ins Action-Objekt.
        scopeCombo.addSelectionListener(new ScopeReferenceComboBox.SelectionListener() {
            @Override
            public void onSelected(String name) {
                if (name == null || name.trim().isEmpty()) {
                    return;
                }
                String template;
                if (name.startsWith("*")) {
                    // Template-Funktion -> "*otpCode" => "{{otpCode()}}"
                    String fn = name.substring(1).trim();
                    template = "{{" + fn + "()}}";
                } else {
                    // normale Variable -> "username" => "{{username}}"
                    template = "{{" + name.trim() + "}}";
                }
                valueField.setText(template);
                action.setValue(template);
            }
        });

        formPanel.add(valuePanel);

        // ---------- Locator Type ----------
        formPanel.add(new JLabel("Locator Type:"));
        locatorBox = new JComboBox<String>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);

        final String initialTypeKey = resolveInitialTypeKey(action);
        locatorBox.setSelectedItem(initialTypeKey);
        formPanel.add(locatorBox);

        // ---------- Selector ----------
        formPanel.add(new JLabel("Selector:"));
        selectorBox = new JComboBox<String>();
        selectorBox.setEditable(true);

        populateSelectorBoxForType(action, selectorBox, initialTypeKey);
        selectorBox.setSelectedItem(action.getSelectedSelector());
        formPanel.add(selectorBox);

        // ---------- User ----------
        formPanel.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(UserRegistry.User::getUsername)
                .toArray(String[]::new);
        userBox = new JComboBox<String>(users);
        userBox.setSelectedItem(action.getUser());
        formPanel.add(userBox);

        // ---------- Timeout ----------
        formPanel.add(new JLabel("Timeout (ms):"));
        timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        // ---------- Interaktion Locator <-> Selector ----------
        locatorBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String typeKey = stringValue(locatorBox.getSelectedItem());
                populateSelectorBoxForType(action, selectorBox, typeKey);

                // Wenn der aktuelle Selector nicht passt, setz auf ersten Kandidaten
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

        // ---------- Save-Button unten rechts ----------
        JButton saveButton = new JButton("Speichern");
        saveButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Action-Type
                action.setAction(stringValue(actionBox.getSelectedItem()));

                // Value
                // Wir nehmen einfach den aktuellen Inhalt des valueField.
                // Der wurde entweder initial aus action.getValue() gesetzt
                // oder durch scopeCombo.onSelected(...) aktualisiert.
                action.setValue(valueField.getText());

                // LocatorType
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
                    // wenn Mist eingegeben wurde, behalten wir den alten Wert
                }

                // Persistieren
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

    // ==============================
    // Helper wie in deiner Version
    // ==============================

    private String resolveInitialTypeKey(TestAction action) {
        if (action.getLocatorType() != null) {
            return action.getLocatorType().getKey();
        }

        String sel = action.getSelectedSelector();
        if (sel != null) {
            String s = sel.trim();
            if (looksLikeXpath(s)) return LocatorType.XPATH.getKey();
            if (s.startsWith("#")) return LocatorType.ID.getKey();
            return LocatorType.CSS.getKey(); // fallback
        }

        if (hasLocator(action, "role"))        return LocatorType.ROLE.getKey();
        if (hasLocator(action, "text"))        return LocatorType.TEXT.getKey();
        if (hasLocator(action, "xpath"))       return LocatorType.XPATH.getKey();
        if (hasLocator(action, "css"))         return LocatorType.CSS.getKey();
        if (hasLocator(action, "id"))          return LocatorType.ID.getKey();
        if (hasLocator(action, "label"))       return LocatorType.LABEL.getKey();
        if (hasLocator(action, "placeholder")) return LocatorType.PLACEHOLDER.getKey();
        if (hasLocator(action, "altText"))     return LocatorType.ALTTEXT.getKey();

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

    private boolean isSelectorValidForType(String typeKey, String selector) {
        if (selector == null) return false;
        String s = selector.trim();
        if (s.length() == 0) return false;

        LocatorType type = LocatorType.fromKey(typeKey);
        if (type == null) return true; // permissiv

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

    private String stringValue(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }
}
