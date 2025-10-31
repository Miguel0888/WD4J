package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.TestCase;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den Case-Scope.
 *
 * Tabs:
 *  - Before     (Variablen, die vor diesem TestCase laufen)
 *  - Templates  (Funktionszeiger/lazy für diesen TestCase)
 *
 * Noch ohne Persist-Knopf. Speichern ans Repo machen wir im nächsten Schritt.
 */
public class CaseScopeEditorTab extends JPanel {

    private final TestCase testCase;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public CaseScopeEditorTab(TestCase testCase) {
        super(new BorderLayout());
        this.testCase = testCase;

        // --- Header oben: Case-Name ---
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Case-Scope: " + safe(testCase.getName()), SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea();
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);

        // Hinweis:
        // TestCase hat bei dir aktuell kein description-Feld.
        // Wir lassen das TextArea leer (künftige Erweiterung möglich).
        desc.setText("");

        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        add(header, BorderLayout.NORTH);

        // --- unsere zwei Tabs ---
        innerTabs.addTab("Before",    buildTablePanel(testCase.getBeforeCase(), "Before"));
        innerTabs.addTab("Templates", buildTablePanel(testCase.getTemplates(),  "Templates"));

        add(innerTabs, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel(List<GivenCondition> data, String scopeName) {
        JPanel panel = new JPanel(new BorderLayout());

        GivenTableModel model = new GivenTableModel(data);
        JTable table = new JTable(model);

        // Toolbar (+ / –)
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(e -> {
            // neue Default-Zeile einfügen
            data.add(new GivenCondition(
                    "preconditionRef",
                    "name=<neu>&expressionRaw="
            ));
            model.fireTableDataChanged();
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                model.fireTableDataChanged();
            }
        });

        bar.add(addBtn);
        bar.add(delBtn);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    private static String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "" : s.trim();
    }

    /**
     * TableModel für Name/Expression.
     * Gleiche Logik wie in SuiteScopeEditorTab.GivenTableModel,
     * aber hier nochmal eigenständig eingebettet, damit diese Klasse standalone kompilierbar ist.
     */
    private static class GivenTableModel extends AbstractTableModel {

        private final List<GivenCondition> rows;

        public GivenTableModel(List<GivenCondition> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2; // Name, Expression
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Name";
            if (column == 1) return "Expression";
            return super.getColumnName(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GivenCondition gc = rows.get(rowIndex);
            java.util.Map<String,String> map = parseValueMap(gc.getValue());

            if (columnIndex == 0) {
                // Name
                return map.getOrDefault("name", "");
            }
            if (columnIndex == 1) {
                // Expression
                return map.getOrDefault("expressionRaw", "");
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            GivenCondition gc = rows.get(rowIndex);
            java.util.Map<String,String> map = parseValueMap(gc.getValue());

            String val = (aValue == null) ? "" : String.valueOf(aValue);

            if (columnIndex == 0) {
                map.put("name", val);
            } else if (columnIndex == 1) {
                map.put("expressionRaw", val);
            }

            gc.setValue(serializeValueMap(map));

            fireTableCellUpdated(rowIndex, columnIndex);
        }

        // copy von deiner parse/serialize Logik aus GivenConditionEditorTab
        private java.util.Map<String,String> parseValueMap(String raw) {
            java.util.Map<String,String> result = new java.util.LinkedHashMap<>();
            if (raw != null && raw.contains("=")) {
                String[] pairs = raw.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        result.put(kv[0], kv[1]);
                    }
                }
            }
            return result;
        }

        private String serializeValueMap(java.util.Map<String,String> map) {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String,String> e : map.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            return sb.toString();
        }
    }
}
