// src/main/java/de/bund/zrb/ui/celleditors/ExpressionCellEditor.java
package de.bund.zrb.ui.celleditors;

import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Provide an RSyntaxTextArea with AutoComplete for expressions using {{ }} and functions().
 * Keep Java 8 compatibility and return the text area directly as editor component.
 */
public class ExpressionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {

    private final RSyntaxTextArea textArea = new RSyntaxTextArea(6, 40);
    private final AutoCompletion autoCompletion;
    private final DynamicCompletionProvider provider;

    private final Supplier<List<String>> variableNamesSupplier;
    private final Supplier<List<String>> regexNamesSupplier;

    public ExpressionCellEditor(Supplier<List<String>> variableNamesSupplier,
                                Supplier<List<String>> regexNamesSupplier) {
        this.variableNamesSupplier = variableNamesSupplier != null ? variableNamesSupplier : new Supplier<List<String>>() {
            @Override public List<String> get() { return java.util.Collections.<String>emptyList(); }
        };
        this.regexNamesSupplier = regexNamesSupplier != null ? regexNamesSupplier : new Supplier<List<String>>() {
            @Override public List<String> get() { return java.util.Collections.<String>emptyList(); }
        };

        configureEditorArea(textArea);

        provider = new DynamicCompletionProvider(this.variableNamesSupplier, this.regexNamesSupplier);
        provider.setAutoActivationRules(true, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_");
        fillFunctionCompletions(provider);
        fillShorthandCompletions(provider);

        autoCompletion = new AutoCompletion(provider);
        autoCompletion.setAutoActivationEnabled(true);
        autoCompletion.setParameterAssistanceEnabled(true);
        autoCompletion.setTriggerKey(KeyStroke.getKeyStroke("control SPACE"));
        autoCompletion.install(textArea);

        installEnterBehavior(textArea);
    }

    // ----- UI setup -----

    private static void configureEditorArea(RSyntaxTextArea ta) {
        // Configure base editor
        ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        ta.setCodeFoldingEnabled(false);
        ta.setBracketMatchingEnabled(true);
        ta.setAnimateBracketMatching(true);
        ta.setAutoIndentEnabled(true);
        ta.setTabsEmulated(true);
        ta.setTabSize(2);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);

        // Force a comfortable 3-line height
        ta.setRows(3);
        int lineH = ta.getFontMetrics(ta.getFont()).getHeight();
        ta.setPreferredSize(new java.awt.Dimension(0, 3 * lineH + 8));
    }

    private static void installEnterBehavior(final RSyntaxTextArea ta) {
        // Press Enter -> end editing; Shift+Enter or Ctrl/Cmd+Enter -> newline
        final InputMap im = ta.getInputMap();
        final ActionMap am = ta.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "commit-or-newline");
        am.put("commit-or-newline", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(); // Java 8
                int mods = e != null ? e.getModifiers() : 0;

                boolean ctrlOrCmd = (mods & menuMask) != 0;
                boolean shift = (mods & java.awt.event.InputEvent.SHIFT_MASK) != 0;

                if (ctrlOrCmd || shift) {
                    ta.replaceSelection("\n");
                } else {
                    // Commit edit through Swing's editing lifecycle
                    ta.requestFocus(false);
                    ta.transferFocus();
                }
            }
        });
    }

    // ----- Static completions (functions + snippets) -----

    private static void fillFunctionCompletions(DefaultCompletionProvider provider) {
        List<String> functionNames = new ArrayList<String>(ExpressionRegistryImpl.getInstance().getKeys());
        Collections.sort(functionNames, String.CASE_INSENSITIVE_ORDER);

        for (String fn : functionNames) {
            FunctionCompletion c = new FunctionCompletion(provider, fn, "String");
            c.setParams(createDummyParams()); // Replace with real signature mapping if available
            c.setShortDescription("Funktion aus ExpressionRegistry");
            provider.addCompletion(c);
        }
    }

    private static List<ParameterizedCompletion.Parameter> createDummyParams() {
        List<ParameterizedCompletion.Parameter> params =
                new ArrayList<ParameterizedCompletion.Parameter>(3);
        params.add(new ParameterizedCompletion.Parameter("String", "p1"));
        params.add(new ParameterizedCompletion.Parameter("String", "p2"));
        params.add(new ParameterizedCompletion.Parameter("String", "p3"));
        return params;
    }

    private static void fillShorthandCompletions(DefaultCompletionProvider provider) {
        provider.addCompletion(new ShorthandCompletion(provider, "fn", "{{${cursor}}()}}", "Funktionsaufruf-Template"));
        provider.addCompletion(new ShorthandCompletion(provider, "{{", "{{${cursor}}}}", "Mustache-Variable"));
        provider.addCompletion(new ShorthandCompletion(provider, "regex", "{{REGEX:${cursor}}}", "Regex-Platzhalter"));
    }

    // ----- Dynamic provider -----

    /**
     * Extend DefaultCompletionProvider to inject dynamic completions (variables, regex presets)
     * based on caret context while keeping the base behavior.
     */
    private static final class DynamicCompletionProvider extends DefaultCompletionProvider {
        private final Supplier<List<String>> variableNamesSupplier;
        private final Supplier<List<String>> regexNamesSupplier;

        DynamicCompletionProvider(Supplier<List<String>> variableNamesSupplier,
                                  Supplier<List<String>> regexNamesSupplier) {
            this.variableNamesSupplier = variableNamesSupplier;
            this.regexNamesSupplier = regexNamesSupplier;
        }

        @Override
        public List getCompletions(JTextComponent comp) {
            List out = new ArrayList(super.getCompletions(comp)); // keep defaults
            try {
                String text = comp.getDocument().getText(0, comp.getDocument().getLength());
                int caret = comp.getCaretPosition();

                int open = text.lastIndexOf("{{", caret);
                int close = text.lastIndexOf("}}", caret);
                boolean insideMustache = open >= 0 && (close < 0 || close < open);

                if (insideMustache) {
                    // Offer variable names
                    for (String v : variableNamesSupplier.get()) {
                        out.add(new BasicCompletion(this, v, "Variable", "Variable"));
                    }
                    // Offer template markers for registry functions
                    for (String f : ExpressionRegistryImpl.getInstance().getKeys()) {
                        out.add(new BasicCompletion(this, "*" + f, "Template", "Template-Funktion"));
                    }
                }

                int tag = text.lastIndexOf("REGEX:", caret);
                if (tag >= 0 && tag >= open) {
                    for (String r : regexNamesSupplier.get()) {
                        out.add(new BasicCompletion(this, r, "Regex-Preset", "Vordefinierte Regex"));
                    }
                }
            } catch (Exception ignore) {
                // Keep base completions if anything goes wrong
            }
            return out;
        }
    }

    // ----- TableCellEditor -----

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        textArea.selectAll();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { textArea.requestFocusInWindow(); }
        });
        return textArea; // Return the text area directly (no scroll pane)
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }
}
