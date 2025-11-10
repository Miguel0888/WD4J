package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.celleditors.ExpressionCellEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
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

            private final DefaultCellEditor validatorTypeEditor = new DefaultCellEditor(new JComboBox<>(
                    new String[]{"", "regex", "fullregex", "contains", "equals", "starts", "ends", "range", "len"}
            ));

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Column 0: Enabled -> default boolean editor
                if (column == 0) return super.getCellEditor(row, column);

                // Column 1: Name -> lock when pinned
                if (column == 1 && includePinnedRow && row == 0) return null;

                // Column 2: Expression -> lock when pinned; otherwise Expression editor
                if (column == 2) {
                    if (includePinnedRow && row == 0) return null;
                    return exprEditor;
                }

                // Column 3: ValidatorType -> dropdown
                if (column == 3) return validatorTypeEditor;

                // Column 4: ValidatorValue -> default text editor
                if (column == 4) return super.getCellEditor(row, column);

                // Column 5: Description -> default editor
                if (column == 5) return super.getCellEditor(row, column);

                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                // Pinned look for name + expr
                if (includePinnedRow && row == 0 && (column == 1 || column == 2)) {
                    return new GrayItalicLockedRenderer();
                }
                // Expression monospaced
                if (column == 2) {
                    return new ExpressionRenderers.ExpressionRenderer();
                }
                // ValidatorValue monospaced for regex readability
                if (column == 4) {
                    return new ExpressionRenderers.ExpressionRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };

        // Table UX
        table.setFillsViewportHeight(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        int fmH = table.getFontMetrics(table.getFont()).getHeight();
        table.setRowHeight(Math.max(table.getRowHeight(), 3 * fmH + 8));

        // Sizes
        if (table.getColumnModel().getColumnCount() > 5) {
            table.getColumnModel().getColumn(2).setPreferredWidth(360); // Expression
            table.getColumnModel().getColumn(3).setPreferredWidth(120); // ValidatorType
            table.getColumnModel().getColumn(4).setPreferredWidth(280); // ValidatorValue
            table.getColumnModel().getColumn(5).setPreferredWidth(240); // Description
        }
        table.getColumnModel().getColumn(0).setMaxWidth(90); // Enabled

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

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> TestRegistry.getInstance().save());

        bar.add(addBtn);
        bar.add(delBtn);
        bar.add(editBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

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
}
