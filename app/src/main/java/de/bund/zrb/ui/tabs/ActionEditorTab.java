package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestActionUpdatedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.expressions.GivenLookupService;
import de.bund.zrb.ui.expressions.ScopeReferenceComboBox;
import de.bund.zrb.util.LocatorType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Editor für eine einzelne TestAction.
 *
 * Value-Feld:
 *  - Freitext erlaubt.
 *  - Auswahl aus ScopeReferenceComboBox:
 *      - Variable  -> schreibe {{name}}
 *      - *Template -> schreibe den Template-Body (Value) 1:1, KEIN erzwungenes {{fn()}}
 */
public class ActionEditorTab extends AbstractEditorTab<TestAction> implements Saveable, Revertable {

    private final TestAction action;

    private JComboBox<String> actionBox;
    private JTextField valueField;
    private ScopeReferenceComboBox scopeCombo;
    private JComboBox<String> locatorBox;
    private JComboBox<String> selectorBox;
    private JComboBox<String> userBox;
    private JTextField timeoutField;
    private JTextField descField; // Header-Feld (neu gestaltete Beschreibung)
    private JSpinner spMinDuration; // NEU

    // Snapshot für Revert
    private String snapAction;
    private String snapValue;
    private String snapLocatorTypeKey;
    private String snapSelectedSelector;
    private String snapUser;
    private Integer snapTimeout;
    private String snapDescription;
    private Integer snapMinDuration;

    public ActionEditorTab(final TestAction action) {
        super("Action Editor", action);
        this.action = action;

        setLayout(new BorderLayout());

        // ----------------------------- Description Header -----------------------------
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));

        descField = new JTextField(action.getDescription() != null ? action.getDescription() : "");
        // Stil: größere, fette Schrift als "Überschrift" / optionaler Titel
        Font base = descField.getFont();
        if (base != null) {
            descField.setFont(base.deriveFont(Font.BOLD, Math.min(22f, base.getSize() + 8f))); // etwas größer
        }
        descField.setForeground(new Color(30, 30, 30));
        descField.setToolTipText("Optionale Beschreibung/Titel für diesen Schritt. Leer lassen für Standardbezeichnung.");
        // Leichte Hintergrund-Hervorhebung
        descField.setBackground(new Color(250, 250, 235));

        // Placeholder-Hinweis, falls leer (einfach via Prompt-Label darüber)
        JLabel headerLabel = new JLabel("Beschreibung (optional):");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.PLAIN, headerLabel.getFont().getSize2D()));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel headerInner = new JPanel(new BorderLayout());
        headerInner.add(headerLabel, BorderLayout.NORTH);
        headerInner.add(descField, BorderLayout.CENTER);
        headerPanel.add(headerInner, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // ----------------------------- Formular (Rest) -----------------------------
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        add(formPanel, BorderLayout.CENTER);

        // Actions
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "fill", "type", "press",
                "select", "check", "radio",
                "navigate", "wait", "screenshot"
        ));

        // LocatorTypes
        Set<String> locatorTypes = new TreeSet<String>();
        for (LocatorType t : LocatorType.values()) {
            locatorTypes.add(t.getKey());
        }

        // Action
        formPanel.add(new JLabel("Action:"));
        actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        actionBox.setSelectedItem(action.getAction());
        formPanel.add(actionBox);

        // Value
        formPanel.add(new JLabel("Value:"));
        JPanel valuePanel = new JPanel(new BorderLayout(4, 0));
        valueField = new JTextField();
        valueField.setEditable(true); // allow free text
        valuePanel.add(valueField, BorderLayout.CENTER);

        scopeCombo = new ScopeReferenceComboBox();
        valuePanel.add(scopeCombo, BorderLayout.EAST);

        formPanel.add(valuePanel);

        // Mindestdauer
        formPanel.add(new JLabel("Min. Dauer (ms):"));
        Integer md = action.getMinDurationMs();
        spMinDuration = new JSpinner(new SpinnerNumberModel(md != null ? md.intValue() : 0, 0, 3600000, 100));
        formPanel.add(spMinDuration);

        // ScopeData bereitstellen
        GivenLookupService.ScopeData scopeData =
                new GivenLookupService().collectScopeForAction(action);
        scopeCombo.setScopeData(scopeData);

        // Vorbelegung Value
        String initialTemplate = (action.getValue() != null) ? action.getValue().trim() : "";
        valueField.setText(initialTemplate);

        // Freitext -> Modell spiegeln
        attachValueMirror(valueField, action);

        // Vorwahl im Combo-Display (ohne Event)
        String preselectName = deriveScopeNameFromTemplate(initialTemplate);
        if (preselectName != null && preselectName.length() > 0) {
            scopeCombo.setInitialChoiceWithoutEvent(preselectName);
        }

        // Einziger Listener: Variable -> {{name}}, *Template -> Body einsetzen
        scopeCombo.addSelectionListener(new ScopeReferenceComboBox.SelectionListener() {
            public void onSelected(String nameWithPrefix) {
                if (nameWithPrefix == null || nameWithPrefix.trim().length() == 0) return;

                String toSet;

                if (nameWithPrefix.startsWith("*")) {
                    // Template gewählt -> Body (Expression-Value) aus Scope nehmen
                    String fn = nameWithPrefix.substring(1).trim();
                    GivenLookupService.ScopeData sd = scopeCombo.getCurrentScopeData();
                    String body = (sd != null && sd.templates != null) ? sd.templates.get(fn) : null;

                    // Fallback: falls nicht gefunden, schreibe zumindest {{fn()}}
                    toSet = (body != null && body.trim().length() > 0)
                            ? body.trim()
                            : "{{" + fn + "()}}";
                } else if (nameWithPrefix.startsWith("①")) {
                    // beforeAll-Variable wie normale Variable behandeln
                    String varName = nameWithPrefix.substring(1).trim();
                    toSet = "{{" + varName + "}}";
                } else {
                    // Normale Variable
                    toSet = "{{" + nameWithPrefix.trim() + "}}";
                }

                valueField.setText(toSet);
                action.setValue(toSet);
            }
        });

        // Locator Type
        formPanel.add(new JLabel("Locator Type:"));
        locatorBox = new JComboBox<String>(locatorTypes.toArray(new String[0]));
        locatorBox.setEditable(true);
        final String initialTypeKey = resolveInitialTypeKey(action);
        locatorBox.setSelectedItem(initialTypeKey);
        formPanel.add(locatorBox);

        // Selector
        formPanel.add(new JLabel("Selector:"));
        selectorBox = new JComboBox<String>();
        selectorBox.setEditable(true);
        populateSelectorBoxForType(action, selectorBox, initialTypeKey);
        selectorBox.setSelectedItem(action.getSelectedSelector());
        formPanel.add(selectorBox);

        // User
        formPanel.add(new JLabel("User:"));
        java.util.List<String> usernames = de.bund.zrb.service.UserRegistry.getInstance().getUsernames();
        String[] userItems = new String[usernames.size() + 1];
        userItems[0] = "";
        for (int i = 0; i < usernames.size(); i++) userItems[i + 1] = usernames.get(i);
        userBox = new JComboBox<String>(userItems);
        userBox.setEditable(false);
        String initialUser = action.getUserRaw() != null ? action.getUserRaw() : "";
        if (initialUser == null || initialUser.trim().length() == 0) userBox.setSelectedIndex(0);
        else userBox.setSelectedItem(initialUser);
        formPanel.add(userBox);

        // Timeout
        formPanel.add(new JLabel("Timeout (ms):"));
        timeoutField = new JTextField(String.valueOf(action.getTimeout()));
        formPanel.add(timeoutField);

        // Locator/Selector-Interaktion
        locatorBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String typeKey = stringValue(locatorBox.getSelectedItem());
                populateSelectorBoxForType(action, selectorBox, typeKey);

                String currentSel = stringValue(selectorBox.getEditor().getItem());
                if (!isSelectorValidForType(typeKey, currentSel)) {
                    if (selectorBox.getItemCount() > 0) selectorBox.setSelectedIndex(0);
                    else selectorBox.setSelectedItem("");
                }
            }
        });

        // Snapshot initialisieren
        snapshotFromModel();

        // Speichern/Verwerfen (unten rechts) — entfernt, übernimmt SaveRevertContainer
        // JButton saveButton = new JButton("Speichern");
        // saveButton.addActionListener(new AbstractAction() {
        //     public void actionPerformed(ActionEvent e) { saveChanges(); }
        // });
        // JButton revertButton = new JButton("Änderungen verwerfen");
        // revertButton.addActionListener(e -> revertChanges());
        // JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // southPanel.add(revertButton);
        // southPanel.add(saveButton);
        // add(southPanel, BorderLayout.SOUTH);

        // Live-Update der Beschreibung bei In-Place-Edit aus dem Baum
        ApplicationEventBus.getInstance().subscribe(TestActionUpdatedEvent.class, ev -> {
            Object p = ev.getPayload();
            if (p == action) {
                SwingUtilities.invokeLater(() -> {
                    String d = action.getDescription();
                    descField.setText(d != null ? d : "");
                });
            }
        });
    }

    @Override
    public void saveChanges() {
        // Action
        action.setAction(stringValue(actionBox.getSelectedItem()));

        // Value
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
        String selUser = stringValue(userBox.getSelectedItem());
        if (selUser == null || selUser.trim().length() == 0) {
            action.setUser(null);
        } else {
            action.setUser(selUser.trim());
        }

        // Timeout
        try {
            action.setTimeout(Integer.parseInt(timeoutField.getText().trim()));
        } catch (NumberFormatException ignored) { /* keep old */ }

        // Description aus Header
        String d = descField.getText();
        action.setDescription(d != null && d.trim().length() > 0 ? d.trim() : null);

        // Min Duration
        try {
            int val = ((Number) spMinDuration.getValue()).intValue();
            action.setMinDurationMs(val > 0 ? val : null);
        } catch (Exception ignore) { action.setMinDurationMs(null); }

        // Persist
        TestRegistry.getInstance().save();
        // Snapshot aktualisieren
        snapshotFromModel();
    }

    @Override
    public void revertChanges() {
        // Auf Snapshot zurücksetzen
        action.setAction(snapAction);
        action.setValue(snapValue);
        action.setLocatorType(LocatorType.fromKey(snapLocatorTypeKey));
        action.setSelectedSelector(snapSelectedSelector);
        action.setUser((snapUser == null || snapUser.trim().isEmpty()) ? null : snapUser.trim());
        action.setTimeout(snapTimeout != null ? snapTimeout : action.getTimeout());
        action.setDescription(snapDescription != null && snapDescription.trim().length() > 0 ? snapDescription.trim() : null);
        action.setMinDurationMs(snapMinDuration);

        // UI aktualisieren
        SwingUtilities.invokeLater(() -> {
            actionBox.setSelectedItem(action.getAction());
            valueField.setText(action.getValue() != null ? action.getValue() : "");
            String typeKey = resolveInitialTypeKey(action);
            locatorBox.setSelectedItem(typeKey);
            populateSelectorBoxForType(action, selectorBox, typeKey);
            selectorBox.setSelectedItem(action.getSelectedSelector());
            String ur = action.getUserRaw();
            if (ur == null || ur.trim().isEmpty()) userBox.setSelectedIndex(0); else userBox.setSelectedItem(ur);
            timeoutField.setText(String.valueOf(action.getTimeout()));
            descField.setText(action.getDescription() != null ? action.getDescription() : "");
            spMinDuration.setValue(action.getMinDurationMs() != null ? action.getMinDurationMs() : 0);
        });
    }

    private void snapshotFromModel() {
        snapAction = action.getAction();
        snapValue = action.getValue();
        snapLocatorTypeKey = resolveInitialTypeKey(action);
        snapSelectedSelector = action.getSelectedSelector();
        snapUser = action.getUserRaw();
        snapTimeout = action.getTimeout();
        snapDescription = action.getDescription();
        snapMinDuration = action.getMinDurationMs();
    }

    /**
     * "{{username}}"  -> "username"
     * "{{otpCode()}}" -> "*otpCode"
     * Sonst -> null
     */
    private String deriveScopeNameFromTemplate(String template) {
        if (template == null) return null;
        String t = template.trim();
        if (!t.startsWith("{{") || !t.endsWith("}}")) return null;

        String inner = t.substring(2, t.length() - 2).trim();
        if (inner.endsWith("()")) {
            String base = inner.substring(0, inner.length() - 2).trim();
            return base.length() > 0 ? "*" + base : null;
        } else {
            return inner.length() > 0 ? inner : null;
        }
    }

    // ---------- Helpers ----------

    private String resolveInitialTypeKey(TestAction action) {
        if (action.getLocatorType() != null) return action.getLocatorType().getKey();

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
        if (candidate != null && candidate.trim().length() > 0) addIfAbsent(selectorBox, candidate);

        String currentSel = action.getSelectedSelector();
        if (currentSel != null && currentSel.trim().length() > 0) {
            if (isSelectorValidForType(typeKey, currentSel)) addIfAbsent(selectorBox, currentSel);
        }

        if (selectorBox.getItemCount() == 0) selectorBox.addItem("");

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
        return t.startsWith("//") || t.startsWith(".//") || t.startsWith("/") || t.startsWith("(");
    }

    /** Mirror free text from UI into model immediately. */
    private static void attachValueMirror(final JTextField field, final TestAction action) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void mirror() {
                String txt = field.getText();
                if (txt == null) {
                    action.setValue(null);
                } else {
                    String t = txt.trim();
                    action.setValue(t.length() == 0 ? null : txt);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { mirror(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { mirror(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { mirror(); }
        });
    }
}
