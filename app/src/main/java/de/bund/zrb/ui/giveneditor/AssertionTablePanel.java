package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assertions table UI with six columns:
 * [Enabled ✓] | Name | Expression | ValidatorType | ValidatorValue | Description
 *
 * Behavior:
 * - Pinned row (index 0 when pinnedKey != null):
 *   - Enabled: editable (allow disable default/pinned assertion)
 *   - Name:    locked (gray/italic)
 *   - Expr:    locked (gray/italic)
 *   - Desc:    editable (manager-friendly text)
 * - Expression column uses ExpressionCellEditor (same UX as MapTablePanel).
 * - Description is a plain editable text cell (default editor).
 */
public class AssertionTablePanel extends JPanel {

    // Kurze Hilfen (Tooltip für Typ-Spalte)
    private static final Map<String,String> TYPE_SHORT_HELP = new LinkedHashMap<String,String>() {{
        put("", "Keine Validierung – Ausdruck nur informativ, immer PASS");
        put("regex", "Teiltreffer (Pattern.find). Beispiel: foo|bar");
        put("fullregex", "Volltreffer (Pattern.matches). Beispiel: ^OK-[0-9]+$");
        put("contains", "String enthält Teilsequenz (case-sensitive)");
        put("equals", "Exakte Gleichheit (case-sensitive)");
        put("starts", "String beginnt mit Wert");
        put("ends", "String endet mit Wert");
        put("range", "Numerischer Wertebereich inkl. Grenzen: min:max (Double)");
        put("len", "Längenprüfung: n | >=n | <=n");
    }};

    // Ausführliche Hilfen (Tooltip für Wert-Spalte & ComboBox selbst)
    private static final Map<String,String> TYPE_DETAIL_HELP = new LinkedHashMap<String,String>() {{
        put("", "Keine Validierung aktiv. Der ausgewertete Ausdruck beeinflusst das Ergebnis nicht.");
        put("regex", "Regex-Teiltreffer (find):\nBeispiele:\n  foo\n  (ERROR|WARN)\n  ID-[0-9]{4}\nNamed Groups möglich: (?<id>[0-9]+)");
        put("fullregex", "Regex muss komplette Zeichenkette matchen (matches).\nBeispiele:\n  ^OK$\n  ^INV-[0-9]{6}$\n  ^[A-Z]{2}[0-9]{2}$");
        put("contains", "String.contains(expected). Groß-/Kleinschreibung bleibt erhalten.");
        put("equals", "String.equals(expected). Exakt gleiche Zeichenfolge erforderlich.");
        put("starts", "String.startsWith(expected). Beispiel: 'ERR' passt zu 'ERROR 42'.");
        put("ends", "String.endsWith(expected). Beispiel: '.xml' passt zu 'data/file.xml'.");
        put("range", "Numerischer inklusiver Bereich: min:max\nBeispiele:\n  0:1\n  -5.5:10.75\nWert wird als double geparst.");
        put("len", "Längenprüfung: \n  n   -> Länge == n\n  >=n -> Länge >= n\n  <=n -> Länge <= n\nBeispiel: >=3");
    }};

    public AssertionTablePanel(final Map<String, String> backingExpressions,
                               final Map<String, Boolean> backingEnabled,
                               final Map<String, String> backingDescriptions,
                               final Map<String, String> backingValidatorTypes,
                               final Map<String, String> backingValidatorValues,
                               final String scopeName,
                               final String pinnedKey,
                               final String pinnedValue) {
        super(new BorderLayout());

        final boolean includePinnedRow = (pinnedKey != null);

        // Ensure pinned exists
        boolean needImmediateSave = false;
        if (includePinnedRow && !backingExpressions.containsKey(pinnedKey)) {
            backingExpressions.put(pinnedKey, pinnedValue != null ? pinnedValue : "");
            if (backingEnabled != null) backingEnabled.put(pinnedKey, Boolean.TRUE);
            if (backingDescriptions != null && !backingDescriptions.containsKey(pinnedKey)) {
                backingDescriptions.put(pinnedKey, "");
            }
            if (backingValidatorTypes != null && !backingValidatorTypes.containsKey(pinnedKey)) {
                backingValidatorTypes.put(pinnedKey, "");
            }
            if (backingValidatorValues != null && !backingValidatorValues.containsKey(pinnedKey)) {
                backingValidatorValues.put(pinnedKey, "");
            }
            needImmediateSave = true;
        }

        // Fallback if null provided
        final Map<String,String> descMap = (backingDescriptions != null)
                ? backingDescriptions
                : new LinkedHashMap<String,String>();
        final Map<String,String> vtMap = (backingValidatorTypes != null)
                ? backingValidatorTypes
                : new LinkedHashMap<String,String>();
        final Map<String,String> vvMap = (backingValidatorValues != null)
                ? backingValidatorValues
                : new LinkedHashMap<String,String>();

        final AssertionTableModel model =
                new AssertionTableModel(backingExpressions, backingEnabled, descMap, vtMap, vvMap, includePinnedRow, pinnedKey);

        final JTable table = new JTable(model) {

            private final ExpressionCellEditor exprEditor =
                    new ExpressionCellEditor(
                            MapTablePanelFactories.varSupplierForAssertions(),
                            MapTablePanelFactories.fnSupplier(),
                            MapTablePanelFactories.rxSupplier()
                    );

            private final JComboBox<String> validatorTypeCombo = new JComboBox<>(
                    new String[]{"", "regex", "fullregex", "contains", "equals", "starts", "ends", "range", "len"}
            );
            private final DefaultCellEditor validatorTypeEditor = new DefaultCellEditor(validatorTypeCombo);

            {
                // Dynamischer Tooltip für ComboBox je nach Auswahl
                validatorTypeCombo.addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        String sel = (String) e.getItem();
                        validatorTypeCombo.setToolTipText(TYPE_DETAIL_HELP.getOrDefault(sel, ""));
                    }
                });
                // Initial setzen
                validatorTypeCombo.setToolTipText(TYPE_DETAIL_HELP.get(""));
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 0) return super.getCellEditor(row, column); // Enabled
                if (column == 1 && includePinnedRow && row == 0) return null; // Name pinned
                if (column == 2) { // Expression
                    if (includePinnedRow && row == 0) return null;
                    return exprEditor;
                }
                if (column == 3) return validatorTypeEditor; // Type
                if (column == 4) return super.getCellEditor(row, column); // Value
                if (column == 5) return super.getCellEditor(row, column); // Desc
                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (includePinnedRow && row == 0 && (column == 1 || column == 2)) {
                    return new GrayItalicLockedRenderer();
                }
                if (column == 2) { // Expression monospaced
                    return new ExpressionRenderers.ExpressionRenderer();
                }
                if (column == 4) { // ValidatorValue monospaced
                    return new ExpressionRenderers.ExpressionRenderer();
                }
                return super.getCellRenderer(row, column);
            }

            // Kontextsensitiver Tooltip je nach Spalte & Typ
            @Override
            public String getToolTipText(MouseEvent event) {
                Point p = event.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);
                if (row < 0 || col < 0) return null;
                // Typ der aktuellen Zeile ermitteln
                String type = null;
                try {
                    Object v = getValueAt(row, 3); // Type column
                    type = (v == null) ? "" : v.toString();
                } catch (Exception ignore) { type = ""; }

                if (col == 3) { // ValidatorType Spalte
                    return TYPE_SHORT_HELP.getOrDefault(type == null ? "" : type, "");
                } else if (col == 4) { // ValidatorValue
                    return TYPE_DETAIL_HELP.getOrDefault(type == null ? "" : type, "");
                }
                return super.getToolTipText(event);
            }
        };

        // Preview Panel unter Tabelle für Live-Status
        JLabel livePreview = new JLabel("Validator Vorschau: (kein Eintrag selektiert)");
        livePreview.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        livePreview.setFont(livePreview.getFont().deriveFont(Font.ITALIC, livePreview.getFont().getSize2D()));

        // Selection Listener zur Aktualisierung
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            updateLivePreview(table, livePreview);
        });

        // Editor Commit Hook: nach Edit Update
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " After-Assertion hinzufügen");
        addBtn.addActionListener(e -> model.addEmptyRow());

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                if (!model.canRemoveRow(row)) {
                    JOptionPane.showMessageDialog(
                            AssertionTablePanel.this,
                            "Gepinnte Assertion kann nicht gelöscht werden.",
                            "Hinweis",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                model.removeRow(row);
            }
        });

        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            table.editCellAt(row, 2);
            Component editorComp = table.getEditorComponent();
            if (editorComp != null) editorComp.requestFocusInWindow();
        });

        JButton suggestRegexBtn = new JButton("⚙ Regex Vorschlag");
        suggestRegexBtn.setToolTipText("Erzeugt aus dem aktuellen Expression-Wert einen Regex-Vorschlag und setzt Typ=regex.");
        suggestRegexBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            // Expression holen
            Object exprVal = table.getValueAt(row, 2);
            if (exprVal == null) return;
            String expr = String.valueOf(exprVal);
            if (expr.trim().isEmpty()) return;
            String pattern = buildRegexSuggestion(expr.trim());
            table.setValueAt("regex", row, 3); // Typ
            table.setValueAt(pattern, row, 4);  // Value
            updateLivePreview(table, livePreview);
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.add(suggestRegexBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(livePreview, BorderLayout.SOUTH);

        if (needImmediateSave) {
            try { TestRegistry.getInstance().save(); } catch (Throwable ignore) { }
        }
    }

    /** Gray/italic locked look for pinned cells (name/expr). */
    static final class GrayItalicLockedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
            return c;
        }
    }

    private void updateLivePreview(JTable table, JLabel livePreview) {
        int row = table.getSelectedRow();
        if (row < 0) { livePreview.setText("Validator Vorschau: (kein Eintrag selektiert)"); return; }
        Object typeObj = table.getValueAt(row, 3);
        Object valueObj = table.getValueAt(row, 4);
        String type = typeObj == null ? "" : typeObj.toString().trim();
        String val  = valueObj == null ? "" : valueObj.toString();
        if (type.isEmpty()) {
            livePreview.setText("Validator Vorschau: Kein Typ gesetzt – Assertion ist immer PASS.");
            return;
        }
        if (type.equals("regex") || type.equals("fullregex")) {
            try {
                java.util.regex.Pattern.compile(val);
                livePreview.setText("Validator Vorschau: Pattern gültig ✔ (" + (type.equals("fullregex")?"vollständiger":"Teil") + " Match)");
            } catch (Exception ex) {
                livePreview.setText("Validator Vorschau: Pattern ungültig ❌ " + ex.getMessage());
            }
            return;
        }
        if (type.equals("contains")) {
            livePreview.setText("Validator Vorschau: enthält(\"" + val + "\") – PASS wenn Wert diese Sequenz enthält");
        } else if (type.equals("equals")) {
            livePreview.setText("Validator Vorschau: equals(\"" + val + "\") – PASS bei exakter Gleichheit");
        } else if (type.equals("starts")) {
            livePreview.setText("Validator Vorschau: startsWith(\"" + val + "\")");
        } else if (type.equals("ends")) {
            livePreview.setText("Validator Vorschau: endsWith(\"" + val + "\")");
        } else if (type.equals("range")) {
            livePreview.setText("Validator Vorschau: range " + val + " – Format min:max, inklusiv");
        } else if (type.equals("len")) {
            livePreview.setText("Validator Vorschau: Länge prüfen " + val + " (n | >=n | <=n)");
        } else if (type.startsWith("!")) {
            livePreview.setText("Validator Vorschau: Negationstyp '" + type + "' – Ergebnis wird invertiert");
        } else {
            livePreview.setText("Validator Vorschau: unbekannter Typ '" + type + "' – wird immer FAIL ergeben");
        }
    }

    private String buildRegexSuggestion(String sample) {
        // Einfache Heuristik: Zahlenblöcke -> \d+, UUID -> [0-9a-fA-F-]{36}, Datum yyyy-MM-dd -> \d{4}-\d{2}-\d{2}
        String s = sample;
        // UUID Ersetzung
        s = s.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "[0-9a-fA-F-]{36}");
        // Datum yyyy-MM-dd
        s = s.replaceAll("\\b(20[0-9]{2})-([0-1][0-9])-([0-3][0-9])\\b", "\\d{4}-\\d{2}-\\d{2}");
        // Zahlenblöcke
        s = s.replaceAll("[0-9]+", "\\\\d+");
        // Replace whitespace with \s+
        s = s.replaceAll("\\s+", "\\\\s+");
        // Escape regex special chars except our replacements
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (".[]{}()^$?+|".indexOf(c) >= 0) {
                out.append('\\').append(c);
            } else {
                out.append(c);
            }
        }
        String pattern = out.toString();
        // Kleine Optimierung: mehrere \s+ hintereinander -> \s+
        pattern = pattern.replaceAll("(\\\\s\\\\+){2,}", "\\\\s+");
        // Füge Anchors hinzu für fullregex typische Nutzung
        return "^" + pattern + "$";
    }
}
