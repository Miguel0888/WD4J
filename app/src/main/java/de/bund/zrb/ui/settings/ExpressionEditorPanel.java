package de.bund.zrb.ui.settings;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.runtime.ExpressionRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;
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

    // Free-form parameter input (JSON array or comma-separated)
    private final JTextField paramInput = new JTextField();

    // Backing registry (singleton for now)
    private final ExpressionRegistry registry = ExpressionRegistryImpl.getInstance();

    private final Gson gson = new Gson();

    public ExpressionEditorPanel() {
        setLayout(new BorderLayout(10, 10));

        // Fill dropdown with known keys
        keyDropdown.addItem(""); // empty entry as "nothing selected"
        List<String> keys = registry.getKeys();
        for (int i = 0; i < keys.size(); i++) {
            keyDropdown.addItem(keys.get(i));
        }

        // Configure editor area
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Configure result area
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);

        // When user changes the dropdown selection, update codeArea
        keyDropdown.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String key = (String) keyDropdown.getSelectedItem();
                String txt = "";
                if (key != null && key.trim().length() > 0) {
                    txt = formatForEditor(key);
                }
                codeArea.setText(txt);
            }
        });

        // Build top section: dropdown + code editor
        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(keyDropdown, BorderLayout.NORTH);
        top.add(new RTextScrollPane(codeArea), BorderLayout.CENTER);

        // Build mid section: parameter input for evaluation
        JPanel paramPanel = new JPanel(new BorderLayout(5, 5));
        paramPanel.add(new JLabel("Parameter (z.B. [\"Alice\",\"42\"] oder CSV):"), BorderLayout.NORTH);
        paramPanel.add(paramInput, BorderLayout.CENTER);

        // Build bottom section: buttons + result preview
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton evalButton    = new JButton("▶ Ausführen");
        JButton saveButton    = new JButton("💾 Speichern unter...");
        JButton removeButton  = new JButton("❌ Entfernen");
        JButton formatButton  = new JButton("✨ Formatieren");

        buttons.add(evalButton);
        buttons.add(saveButton);
        buttons.add(removeButton);
        buttons.add(formatButton);

        evalButton.addActionListener(e -> evaluate());
        saveButton.addActionListener(e -> saveAsNew());
        removeButton.addActionListener(e -> removeCurrent());
        formatButton.addActionListener(e -> formatCode());

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Assemble panel
        add(top, BorderLayout.NORTH);
        add(paramPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Initialize UI state
        keyDropdown.setSelectedItem("");
        codeArea.setText("");
        resultArea.setText("");
    }

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

            List<String> params = parseParameters(paramInput.getText());
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
     * Register a new key with the currently edited code.
     * Ask the user for the new key.
     */
    private void saveAsNew() {
        String newKey = JOptionPane.showInputDialog(
                this,
                "Name für neuen Ausdruck:",
                "Neuen Ausdruck speichern",
                JOptionPane.PLAIN_MESSAGE
        );

        if (newKey == null) {
            return;
        }
        newKey = newKey.trim();
        if (newKey.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Name darf nicht leer sein.",
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        registry.register(newKey, codeArea.getText());
        registry.save();

        // Update dropdown to include the new key and select it
        keyDropdown.addItem(newKey);
        keyDropdown.setSelectedItem(newKey);
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
        codeArea.setText("");
        resultArea.setText("");
    }

    /**
     * Persist current registry state.
     * Call this before closing the dialog.
     */
    public void saveChanges() {
        registry.save();
    }
}
