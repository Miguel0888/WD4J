package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.service.RegexPatternRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import de.bund.zrb.ui.celleditors.DescribedItem;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class MapTablePanel extends JPanel {

    /**
     * @param backing         Map-Backing
     * @param scopeName       Label/Tipptexte
     * @param usersProvider   liefert die gleichen User-Optionen wie dein TestAction-Dropdown; null erlaubt
     */
    public MapTablePanel(final Map<String,String> backing,
                         final String scopeName,
                         final Supplier<List<String>> usersProvider) {
        super(new BorderLayout());

    // Decide includeUserRow from usersProvider (null => false)
    final boolean includeUserRow = (usersProvider != null);

        final MapTableModel model = new MapTableModel(backing, includeUserRow);
        final JTable table = new JTable(model) {

            private final TableCellEditor userEditor = buildUserEditor(usersProvider);
            private final ExpressionCellEditor exprEditor = buildExpressionEditor();

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Column 1 (Expression):
                if (column == 1) {
                    // Row 0, Column 1 -> User-Dropdown (falls aktiv)
                    if (includeUserRow && row == 0) {
                        return userEditor;
                    }
                    // Alle anderen in Column 1 -> ExpressionCellEditor (mit IntelliSense)
                    return exprEditor;
                }

                // Column 0 (Name) und alle anderen Spalten -> Standard-Editor der JTable (kein IntelliSense)
                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
            // Row 0, Col 0 -> ausgegraut/gesperrt (nur wenn includeUserRow == true)
                if (includeUserRow && row == 0 && column == 0) {
                    return new UserNameLockedRenderer();
                }
                // Value-Spalte monospaced
                if (column == 1) {
                    return new MultiLineMonoRenderer();
                }
                return super.getCellRenderer(row, column);
            }

            private TableCellEditor buildUserEditor(final Supplier<List<String>> provider) {
                return new UserComboBoxCellEditor(new UserComboBoxCellEditor.ChoicesProvider() {
                    public List<String> getUsers() {
                        return provider != null ? provider.get() : java.util.Collections.<String>emptyList();
                    }
                });
            }

            private ExpressionCellEditor buildExpressionEditor() {
                // ------- Suppliers wie gehabt -------
                Supplier<List<String>> varSupplier = new Supplier<List<String>>() {
                    @Override public List<String> get() {
                        Set<String> all = new LinkedHashSet<String>();
                        if (backing != null) all.addAll(backing.keySet());

                        de.bund.zrb.model.RootNode root = TestRegistry.getInstance().getRoot();
                        if (root != null) {
                            if (root.getBeforeAll()   != null) all.addAll(root.getBeforeAll().keySet());
                            if (root.getBeforeEach()  != null) all.addAll(root.getBeforeEach().keySet());
                            if (root.getTemplates()   != null) all.addAll(root.getTemplates().keySet());
                        }
                        List<de.bund.zrb.model.TestSuite> suites = TestRegistry.getInstance().getAll();
                        if (suites != null) {
                            for (int si = 0; si < suites.size(); si++) {
                                de.bund.zrb.model.TestSuite s = suites.get(si);
                                if (s.getBeforeAll()   != null) all.addAll(s.getBeforeAll().keySet());
                                if (s.getBeforeEach()  != null) all.addAll(s.getBeforeEach().keySet());
                                if (s.getTemplates()   != null) all.addAll(s.getTemplates().keySet());
                                List<de.bund.zrb.model.TestCase> cases = s.getTestCases();
                                if (cases != null) {
                                    for (int ci = 0; ci < cases.size(); ci++) {
                                        de.bund.zrb.model.TestCase tc = cases.get(ci);
                                        if (tc.getBefore()    != null) all.addAll(tc.getBefore().keySet());
                                        if (tc.getTemplates() != null) all.addAll(tc.getTemplates().keySet());
                                    }
                                }
                            }
                        }
                        List<String> sorted = new ArrayList<String>(all);
                        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                        return sorted;
                    }
                };

                Supplier<Map<String, DescribedItem>> fnSupplier = new Supplier<Map<String, DescribedItem>>() {
                    @Override public Map<String, DescribedItem> get() {
                        Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                        ExpressionRegistryImpl reg = ExpressionRegistryImpl.getInstance();
                        Set<String> keys = reg.getKeys();
                        List<String> sorted = new ArrayList<String>(keys);
                        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

                        for (int i = 0; i < sorted.size(); i++) {
                            final String name = sorted.get(i);
                            ExpressionFunction builtin = reg.get(name);
                            if (builtin != null) {
                                out.put(name, builtin);
                                continue;
                            }
                            final de.bund.zrb.expressions.domain.FunctionMetadata m = reg.getMetadata(name);
                            out.put(name, new DescribedItem() {
                                public String getDescription() {
                                    return m != null && m.getDescription() != null ? m.getDescription() : "";
                                }
                                public java.util.List<String> getParamNames() {
                                    return m != null && m.getParameterNames() != null ? m.getParameterNames()
                                            : java.util.Collections.<String>emptyList();
                                }
                                public java.util.List<String> getParamDescriptions() {
                                    return m != null && m.getParameterDescriptions() != null ? m.getParameterDescriptions()
                                            : java.util.Collections.<String>emptyList();
                                }
                                public Object getMetadata() { return m; }
                            });
                        }
                        return out;
                    }
                };

                Supplier<Map<String, DescribedItem>> rxSupplier = new Supplier<Map<String, DescribedItem>>() {
                    @Override public Map<String, DescribedItem> get() {
                        Map<String, DescribedItem> out = new LinkedHashMap<String, DescribedItem>();
                        RegexPatternRegistry rx = RegexPatternRegistry.getInstance();
                        List<String> titles = rx.getTitlePresets();
                        for (int i = 0; i < titles.size(); i++) {
                            final String val = titles.get(i);
                            out.put(val, new DescribedItem() {
                                @Override public String getDescription() { return "Regex-Preset (Title)"; }
                            });
                        }
                        List<String> msgs = rx.getMessagePresets();
                        for (int i = 0; i < msgs.size(); i++) {
                            final String val = msgs.get(i);
                            out.put(val, new DescribedItem() {
                                @Override public String getDescription() { return "Regex-Preset (Message)"; }
                            });
                        }
                        return out;
                    }
                };

                return new ExpressionCellEditor(varSupplier, fnSupplier, rxSupplier);
            }
        };

        // UX-Grundsetup
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));

        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(1).setPreferredWidth(480);
        }

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                model.addEmptyRow();
            }
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    if (!model.canRemoveRow(row)) {
                        JOptionPane.showMessageDialog(
                                MapTablePanel.this,
                                "\"user\" kann nicht gelöscht werden.",
                                "Hinweis",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }
                    model.removeRow(row);
                }
            }
        });

        JButton editBtn = new JButton("Bearbeiten");
        editBtn.setToolTipText("Expression in ausgewählter Zeile bearbeiten");
        editBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) return;
                int exprCol = 1;
                table.editCellAt(row, exprCol);
                Component editorComp = table.getEditorComponent();
                if (editorComp != null) {
                    editorComp.requestFocusInWindow();
                }
            }
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                TestRegistry.getInstance().save();
                JOptionPane.showMessageDialog(
                        MapTablePanel.this,
                        "Gespeichert.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // ==== Renderer/Editor-Helfer ====

    /** Gray, italic, locked look for the fixed user name cell. */
    static final class UserNameLockedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(isSelected ? table.getSelectionForeground() : Color.GRAY);
            c.setFont(c.getFont().deriveFont(Font.ITALIC));
            return c;
        }
    }

    static final class UserComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        interface ChoicesProvider { List<String> getUsers(); }
        private final JComboBox<String> combo;
        private final ChoicesProvider provider;

        UserComboBoxCellEditor(ChoicesProvider provider) {
            this.provider = provider;
            this.combo = new JComboBox<String>();
            this.combo.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
            this.combo.setEditable(false);
            rebuild();
        }

        private void rebuild() {
            combo.removeAllItems();
            combo.addItem(""); // leerer Eintrag
            List<String> users = provider != null ? provider.getUsers() : null;
            if (users != null) {
                for (int i = 0; i < users.size(); i++) {
                    String u = users.get(i);
                    if (u != null) {
                        String t = u.trim();
                        if (t.length() > 0) combo.addItem(t);
                    }
                }
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (combo.getItemCount() == 0) rebuild();
            String v = value == null ? "" : String.valueOf(value);
            combo.setSelectedItem(v);
            return combo;
        }

        public Object getCellEditorValue() {
            Object sel = combo.getSelectedItem();
            return sel == null ? "" : String.valueOf(sel);
        }
    }

    /** Monospaced 3-Zeilen Renderer für Expressions (wie gehabt). */
    static final class MultiLineMonoRenderer extends JTextArea implements TableCellRenderer {
        private final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        MultiLineMonoRenderer() {
            setFont(mono);
            setLineWrap(true);
            setWrapStyleWord(false);
            setOpaque(true);
            setRows(3);
            setBorder(null);
        }
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : String.valueOf(value));
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            int fmH = getFontMetrics(getFont()).getHeight();
            int desired = Math.max(table.getRowHeight(), 3 * fmH + 8);
            if (table.getRowHeight() < desired) {
                table.setRowHeight(desired);
            }
            return this;
        }
    }
}
