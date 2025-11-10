package de.bund.zrb.ui.settings;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.runtime.ExpressionRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provide a UI panel to view, edit, register, remove and test expressions/functions
 * stored in the ExpressionRegistry.
 *
 * Intent:
 * - Allow power users / test engineers to maintain dynamic functions like otp(), wrap(), etc.
 * - Allow evaluating a function live with parameters to verify behavior.
 * - Persist registry changes via ExpressionRegistryImpl.save().
 *
 * Scope:
 * - This is pure UI (Swing). It does not decide when expressions are resolved in the workflow.
 * - The panel is embed-friendly (use inside a dialog or tab).
 */
public class ExpressionEditorPanel extends JPanel {

    // Code editor for the selected expression's source/definition
    private final RSyntaxTextArea codeArea = new RSyntaxTextArea(20, 60);

    // Result preview output from "evaluate"
    private final JTextArea resultArea = new JTextArea(5, 40);

    // Dropdown that lists all registered expression keys
    private final JComboBox<String> keyDropdown = new JComboBox<String>();

    // Free-form parameter input (JSON array or comma-separated) for EXECUTION
    private final JTextField execParamInput = new JTextField();

    // Metadata: description and parameters (name + description per row)
    private final JTextArea descArea = new JTextArea(3, 40);
    private final JTable paramTable = new JTable();
    private final ParamTableModel paramModel = new ParamTableModel();

    // Backing registry (singleton for now)
    private final ExpressionRegistry registry = ExpressionRegistryImpl.getInstance();

    private final Gson gson = new Gson();

    public ExpressionEditorPanel() {
        setLayout(new BorderLayout(10, 10));

        // --- Top: Key selector ---
        JPanel header = new JPanel(new BorderLayout(6, 6));
        header.add(buildKeyRow(), BorderLayout.NORTH);
        header.add(buildToolbarRow(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // --- Center: Editor (left) + Metadata (right) ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildEditorPane(), buildMetadataPane());
        split.setResizeWeight(0.66);
        add(split, BorderLayout.CENTER);

        // --- South: Execution panel (params + run button) + result panel ---
        add(buildExecutionPanel(), BorderLayout.SOUTH);

        // Initialize UI state
        keyDropdown.setSelectedItem("");
        prefillBoilerplateForEmptySelection();
        resultArea.setText("");
    }

    // ---------- UI builders ----------

    private JComponent buildKeyRow() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        keyDropdown.addItem(""); // empty entry as "nothing selected"
        for (String key : registry.getKeys()) {
            keyDropdown.addItem(key);
        }
        keyDropdown.setToolTipText("Vorhandenes Template auswählen oder leere Auswahl für neues Template");
        keyDropdown.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String key = (String) keyDropdown.getSelectedItem();
                if (key == null || key.trim().length() == 0) {
                    prefillBoilerplateForEmptySelection();
                    clearMetadata();
                } else {
                    codeArea.setText(formatForEditor(key));
                    loadMetadataFor(key);
                }
            }
        });
        p.add(new JLabel("Template:"), BorderLayout.WEST);
        p.add(keyDropdown, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildToolbarRow() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        JButton saveButton     = new JButton("💾 Speichern …");
        JButton saveMetaButton = new JButton("💾 Metadaten speichern");
        JButton formatButton   = new JButton("✨ Formatieren");
        JButton removeButton   = new JButton("❌ Entfernen");

        saveButton.setToolTipText("Quellcode (und Metadaten) unter Namen speichern (Strg+S)");
        saveMetaButton.setToolTipText("Nur Metadaten speichern (Strg+Shift+S)");
        formatButton.setToolTipText("Code formatieren (Strg+L)");
        removeButton.setToolTipText("Ausgewähltes Template entfernen");

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { saveWithPrefilledName(); }
        });
        saveMetaButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { saveMetadataForCurrent(); }
        });
        formatButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { formatCode(); }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { removeCurrent(); }
        });

        // Shortcuts
        registerKeyStroke(saveButton, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveWithPrefilledName(); }
        });
        registerKeyStroke(saveMetaButton, KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK), "saveMeta", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveMetadataForCurrent(); }
        });
        registerKeyStroke(formatButton, KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "format", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { formatCode(); }
        });

        bar.add(saveButton);
        bar.add(saveMetaButton);
        bar.add(formatButton);
        bar.add(removeButton);
        return bar;
    }

    private JComponent buildEditorPane() {
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        return new RTextScrollPane(codeArea);
    }

    private JComponent buildMetadataPane() {
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        paramTable.setModel(paramModel);

        JPanel right = new JPanel(new BorderLayout(6, 6));

        JPanel descPanel = new JPanel(new BorderLayout(4, 4));
        descPanel.add(new JLabel("Beschreibung:"), BorderLayout.NORTH);
        descPanel.add(new JScrollPane(descArea), BorderLayout.CENTER);

        JPanel paramHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton btAddParam = new JButton("+");
        JButton btDelParam = new JButton("–");
        btAddParam.setToolTipText("Parameter hinzufügen");
        btDelParam.setToolTipText("Markierten Parameter entfernen");
        btAddParam.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { paramModel.addRow(); }
        });
        btDelParam.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = paramTable.getSelectedRow();
                if (row >= 0) paramModel.removeRow(row);
            }
        });
        paramHeader.add(new JLabel("Parameter:"));
        paramHeader.add(btAddParam);
        paramHeader.add(btDelParam);

        JPanel paramPanel = new JPanel(new BorderLayout(4, 4));
        paramPanel.add(paramHeader, BorderLayout.NORTH);
        paramPanel.add(new JScrollPane(paramTable), BorderLayout.CENTER);

        right.add(descPanel, BorderLayout.NORTH);
        right.add(paramPanel, BorderLayout.CENTER);
        return right;
    }

    /** Build execution panel with params field (tooltip) + run button and result area. */
    private JComponent buildExecutionPanel() {
        JPanel exec = new JPanel(new BorderLayout(6, 6));
        exec.setBorder(BorderFactory.createTitledBorder("Test"));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel("Parameter:"), BorderLayout.WEST);

        // Explain parameter formats right on the field
        execParamInput.setToolTipText("Format: JSON-Array [\"Alice\",\"42\"] oder CSV: Alice,42");
        row.add(execParamInput, BorderLayout.CENTER);

        JButton runButton = new JButton("▶ Ausführen");
        runButton.setToolTipText("Mit Parametern ausführen (Strg+Enter). Format: [\"Alice\",\"42\"] oder CSV");
        row.add(runButton, BorderLayout.EAST);

        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { evaluate(); }
        });
        registerKeyStroke(exec,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                "run",
                new AbstractAction() { public void actionPerformed(ActionEvent e) { evaluate(); } });

        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);

        exec.add(row, BorderLayout.NORTH);
        exec.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return exec;
    }

    // ---------- Actions ----------

    /**
     * Load code from registry for the given key.
     */
    private String formatForEditor(String key) {
        return registry.getCode(key).orElse("");
    }

    /**
     * Try to pretty-format the code in the editor using google-java-format.
     * Show an error dialog if formatting fails.
     */
    private void formatCode() {
        try {
            String formatted = new Formatter().formatSource(codeArea.getText());
            codeArea.setText(formatted);
        } catch (FormatterException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Fehler beim Formatieren:\n" + ex.getMessage(),
                    "Formatierfehler",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Evaluate the currently selected key with the given parameters.
     * Show the result (or error message) in resultArea.
     */
    private void evaluate() {
        try {
            String key = (String) keyDropdown.getSelectedItem();
            if (key == null || key.trim().isEmpty()) {
                resultArea.setText("Kein Ausdruck ausgewählt.");
                return;
            }

            List<String> params = parseParameters(execParamInput.getText());
            String result = registry.evaluate(key, params);
            resultArea.setText(result);
        } catch (Exception ex) {
            resultArea.setText("Fehler: " + ex.getMessage());
        }
    }

    /**
     * Parse parameter input.
     * Accept JSON array syntax like ["Alice","42"]
     * or comma-separated "Alice,42".
     */
    private List<String> parseParameters(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String input = raw.trim();
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> parsed = gson.fromJson(input, listType);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception ignore) {
            // Fall back to CSV
        }

        String[] split = input.split("\\s*,\\s*");
        return Arrays.asList(split);
    }

    /**
     * Save the current code (and current metadata) under a name.
     * Prefill with the currently selected key; select all text for quick overwrite.
     * Replaces <NEW_FUNCTION_NAME> in the code with a sanitized class name derived from the entered name.
     */
    private void saveWithPrefilledName() {
        String current = (String) keyDropdown.getSelectedItem();
        String name = promptForName(current != null ? current : "");
        if (name == null) {
            return; // user canceled
        }
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name darf nicht leer sein.", "Eingabefehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Replace placeholder class name if present
        String code = codeArea.getText();
        if (code != null && code.indexOf("<NEW_FUNCTION_NAME>") >= 0) {
            String className = deriveClassNameFromKey(name);
            code = code.replace("<NEW_FUNCTION_NAME>", className);
            codeArea.setText(code);
        }

        // Register source
        registry.register(name, codeArea.getText());

        // Persist metadata alongside code
        FunctionMetadata meta = new FunctionMetadata(
                name,
                descArea.getText(),
                paramModel.getNames(),
                paramModel.getDescs()
        );
        ((ExpressionRegistryImpl) registry).setMetadata(name, meta);

        registry.save();

        if (!containsKeyInDropdown(name)) {
            keyDropdown.addItem(name);
        }
        keyDropdown.setSelectedItem(name);
    }

    /**
     * Save the current code (and current metadata) under a name.
     */
    private void saveCurrent() {
        String current = (String) keyDropdown.getSelectedItem();
        if(current == null || current.isEmpty()) {
            return;
        }

        // Replace placeholder class name if present
        String code = codeArea.getText();
        if (code != null && code.indexOf("<NEW_FUNCTION_NAME>") >= 0) {
            String className = deriveClassNameFromKey(current);
            code = code.replace("<NEW_FUNCTION_NAME>", className);
            codeArea.setText(code);
        }

        // Register source
        registry.register(current, codeArea.getText());

        // Persist metadata alongside code
        FunctionMetadata meta = new FunctionMetadata(
                current,
                descArea.getText(),
                paramModel.getNames(),
                paramModel.getDescs()
        );
        ((ExpressionRegistryImpl) registry).setMetadata(current, meta);

        registry.save();

        if (!containsKeyInDropdown(current)) {
            keyDropdown.addItem(current);
        }
        keyDropdown.setSelectedItem(current);
    }

    /**
     * Remove the currently selected key from the registry after confirmation.
     */
    private void removeCurrent() {
        String key = (String) keyDropdown.getSelectedItem();
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Ausdruck \"" + key + "\" wirklich entfernen?",
                "Bestätigen",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        registry.remove(key);
        registry.save();

        keyDropdown.removeItem(key);
        keyDropdown.setSelectedItem("");
        prefillBoilerplateForEmptySelection();
        clearMetadata();
        resultArea.setText("");
    }

    /**
     * Persist current registry state.
     * Call this before closing the dialog.
     */
    public void saveChanges() {
        saveCurrent();
        registry.save();
    }

    // ---------- Metadata helpers ----------

    private void loadMetadataFor(String key) {
        if (key == null || key.trim().isEmpty()) {
            clearMetadata();
            return;
        }
        FunctionMetadata m = ((ExpressionRegistryImpl) registry).getMetadata(key);
        descArea.setText(m.getDescription());
        paramModel.setData(m.getParameterNames(), m.getParameterDescriptions());
    }

    private void clearMetadata() {
        descArea.setText("");
        paramModel.setData(null, null);
    }

    // ---------- Boilerplate ----------

    private void prefillBoilerplateForEmptySelection() {
        codeArea.setText(buildBoilerplate());
    }

    private String buildBoilerplate() {
        String nl = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.List;").append(nl);
        sb.append("import java.util.function.Function;").append(nl);
        sb.append("public class <NEW_FUNCTION_NAME> implements Function<List<String>, String> {").append(nl);
        sb.append("    String result = null;").append(nl);
        sb.append("    public String apply(List<String> args) {").append(nl);
        sb.append("        // First Parameter:").append(nl);
        sb.append("        final String param1 = args.isEmpty() ? \"OK\" : String.valueOf(args.get(0));").append(nl);
        sb.append("        // Second Parameter:").append(nl);
        sb.append("        final String param2 = args.size() > 1 ? String.valueOf(args.get(1)) : \"\";").append(nl);
        sb.append("        // ..").append(nl).append(nl);
        sb.append("        // ToDo: Implement").append(nl).append(nl);
        sb.append("        return result;").append(nl);
        sb.append("    }").append(nl);
        sb.append("}").append(nl);
        return sb.toString();
    }

    private String deriveClassNameFromKey(String key) {
        // Build a simple valid Java identifier from key; prefix with 'Expr_' if needed
        String k = key.replaceAll("[^A-Za-z0-9_]", "_");
        if (k.length() == 0 || !Character.isJavaIdentifierStart(k.charAt(0))) {
            k = "Expr_" + k;
        }
        // Ensure all chars are identifier parts
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < k.length(); i++) {
            char ch = k.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) out.append(ch);
            else out.append('_');
        }
        return out.toString();
    }

    // ---------- Util ----------

    private boolean containsKeyInDropdown(String key) {
        for (int i = 0; i < keyDropdown.getItemCount(); i++) {
            String it = keyDropdown.getItemAt(i);
            if (key.equals(it)) return true;
        }
        return false;
    }

    private void registerKeyStroke(JComponent c, KeyStroke ks, String name, Action action) {
        InputMap im = c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = c.getActionMap();
        im.put(ks, name);
        am.put(name, action);
    }

    // ---------- Inner model for parameter table ----------

    /** Editable 2-column model: [Parameter-Name, Beschreibung]. */
    private static final class ParamTableModel extends AbstractTableModel {
        private final java.util.List<String> names = new ArrayList<String>();
        private final java.util.List<String> descs = new ArrayList<String>();

        @Override public int getRowCount() { return names.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int c) { return c == 0 ? "Parameter" : "Beschreibung"; }
        @Override public boolean isCellEditable(int r, int c) { return true; }

        @Override
        public Object getValueAt(int row, int col) {
            return col == 0 ? names.get(row) : descs.get(row);
        }

        @Override
        public void setValueAt(Object aValue, int row, int col) {
            String v = aValue != null ? String.valueOf(aValue) : "";
            if (col == 0) names.set(row, v);
            else descs.set(row, v);
            fireTableRowsUpdated(row, row);
        }

        public void setData(java.util.List<String> n, java.util.List<String> d) {
            names.clear(); descs.clear();
            if (n != null) names.addAll(n);
            if (d != null) descs.addAll(d);
            while (descs.size() < names.size()) descs.add("");
            fireTableDataChanged();
        }

        public java.util.List<String> getNames() { return new ArrayList<String>(names); }
        public java.util.List<String> getDescs() { return new ArrayList<String>(descs); }

        public void addRow() {
            names.add(""); descs.add("");
            int idx = names.size() - 1;
            fireTableRowsInserted(idx, idx);
        }

        public void removeRow(int row) {
            if (row < 0 || row >= names.size()) return;
            names.remove(row); descs.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    /** Save current UI metadata for the selected key. */
    private void saveMetadataForCurrent() {
        String key = (String) keyDropdown.getSelectedItem();
        if (key == null || key.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kein Ausdruck ausgewählt.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        FunctionMetadata meta = new FunctionMetadata(
                key,
                descArea.getText(),
                paramModel.getNames(),
                paramModel.getDescs()
        );
        ((ExpressionRegistryImpl) registry).setMetadata(key, meta);
        registry.save();
        JOptionPane.showMessageDialog(this, "Metadaten gespeichert.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Show an input dialog with a prefilled, selected name. */
    private String promptForName(String defaultName) {
        final JTextField tf = new JTextField(defaultName != null ? defaultName : "");
        tf.selectAll(); // select entire text for quick overwrite
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("Name für Ausdruck:"), BorderLayout.NORTH);
        panel.add(tf, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(
                this, panel, "Speichern", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (res == JOptionPane.OK_OPTION) {
            return tf.getText();
        }
        return null;
    }

}
