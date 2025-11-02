package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.RootNode;
import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * Editor für den globalen Root-Scope.
 *
 * Tabs:
 *  - BeforeAll
 *  - BeforeEach
 *  - Templates
 *
 * Rechts oben ein Speichern-Button (schreibt tests.json über TestRegistry.save()).
 * Jede Tabelle hat + und – zum Hinzufügen/Entfernen und kann inline editiert werden.
 *
 * Semantik:
 *  - BeforeAll (root.getBeforeAll()):
 *        Variablen, die EINMAL ganz am Anfang evaluiert werden.
 *        -> landen nicht im Dropdown der WHEN-Values
 *
 *  - BeforeEach (root.getBeforeEach()):
 *        Variablen, die vor jedem TestCase evaluiert werden.
 *        -> tauchen im Dropdown der WHEN-Values als normale Namen auf
 *
 *  - Templates (root.getTemplates()):
 *        Funktionszeiger (lazy ausgewertet in WHEN).
 *        -> tauchen im Dropdown mit führendem * auf
 */
public class RootScopeEditorTab extends JPanel {

    private final RootNode root;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public RootScopeEditorTab(RootNode root) {
        super(new BorderLayout());
        this.root = root;

        // ===== Header (Titel, Info, Save Button) =====
        JPanel header = new JPanel(new BorderLayout());

        JPanel textBlock = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Globaler Scope (Root)", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        JTextArea desc = new JTextArea();
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setText(
                "BeforeAll: Variablen, die genau einmal vor allen Tests ausgewertet werden.\n" +
                        "BeforeEach: Variablen, die vor jedem TestCase neu ausgewertet werden.\n" +
                        "Templates: Funktionszeiger (werden lazy in WHEN mit *name aufgelöst)."
        );

        textBlock.add(title, BorderLayout.NORTH);
        textBlock.add(desc, BorderLayout.CENTER);
        textBlock.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("💾 Speichern");
        saveBtn.setToolTipText("Änderungen global speichern (tests.json aktualisieren)");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    RootScopeEditorTab.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        savePanel.add(saveBtn);
        savePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        header.add(textBlock, BorderLayout.CENTER);
        header.add(savePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ===== Inhalt-Tabs (BeforeAll, BeforeEach, Templates) =====
        innerTabs.addTab("BeforeAll",   buildTablePanel(root.getBeforeAll(),   "BeforeAll"));
        innerTabs.addTab("BeforeEach",  buildTablePanel(root.getBeforeEach(),  "BeforeEach"));
        innerTabs.addTab("Templates",   buildTablePanel(root.getTemplates(),   "Templates"));

        add(innerTabs, BorderLayout.CENTER);
    }

    private JPanel buildTablePanel(List<GivenCondition> data, String scopeName) {
        JPanel panel = new JPanel(new BorderLayout());

        GivenTableModel model = new GivenTableModel(data);
        JTable table = new JTable(model);

        // Toolbar über der Tabelle: [+] [–] [💾]
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(e -> {
            // Neuer Default-Eintrag
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

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    RootScopeEditorTab.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    /**
     * TableModel für Name/Expression.
     * Identisch zur SuiteScopeEditorTab inner class, nur hier nochmal eingefügt
     * damit RootScopeEditorTab allein kompilierbar ist.
     *
     * Wir encodieren/decodieren GivenCondition.value immer noch als
     * "key=value&key2=value2", wie du's vorher hattest.
     *
     * Wichtig: Wir erwarten in der Map mindestens:
     *  - name
     *  - expressionRaw
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
            return 2; // Name | Expression
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
                return map.getOrDefault("name", "");
            }
            if (columnIndex == 1) {
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
