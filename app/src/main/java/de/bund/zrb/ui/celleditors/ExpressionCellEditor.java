// src/main/java/de/bund/zrb/ui/celleditors/ExpressionCellEditor.java
package de.bund.zrb.ui.celleditors;

import de.bund.zrb.runtime.ExpressionRegistryImpl;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent; // <-- wichtig: richtiger Import
import java.awt.Component;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Provide an RSyntaxTextArea with AutoComplete for expressions.
 * Keep Java 8 compatibility.
 */
public class ExpressionCellEditor extends javax.swing.AbstractCellEditor implements TableCellEditor {

    private final RSyntaxTextArea textArea = new RSyntaxTextArea(6, 40);
    private final RTextScrollPane scroller = new RTextScrollPane(textArea);

    private final AutoCompletion autoCompletion;
    private final DynamicCompletionProvider provider;

    // Suppliers for variable and regex names (can be no-op)
    private final Supplier<List<String>> variableNamesSupplier;
    private final Supplier<List<String>> regexNamesSupplier;

    public ExpressionCellEditor(Supplier<List<String>> variableNamesSupplier,
                                Supplier<List<String>> regexNamesSupplier) {

        this.variableNamesSupplier = variableNamesSupplier != null ? variableNamesSupplier : new Supplier<List<String>>() {
            @Override public List<String> get() { return Collections.<String>emptyList(); }
        };
        this.regexNamesSupplier = regexNamesSupplier != null ? regexNamesSupplier : new Supplier<List<String>>() {
            @Override public List<String> get() { return Collections.<String>emptyList(); }
        };

        configureEditorArea(textArea);

        // Build provider with static + dynamic completions
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
        // Configure the text area for Java-like editing features
        ta.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        ta.setCodeFoldingEnabled(true);
        ta.setBracketMatchingEnabled(true);
        ta.setAnimateBracketMatching(true);
        ta.setAutoIndentEnabled(true);
        ta.setTabsEmulated(true);
        ta.setTabSize(2);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);
    }

    private static void installEnterBehavior(final RSyntaxTextArea ta) {
        // Press Enter -> end editing; Shift+Enter -> newline; Ctrl+Enter -> newline
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
                    // Finish editing via Swing cell editor contract
                    Component c = ta;
                    while (c != null && !(c instanceof javax.swing.CellEditor)) {
                        c = c.getParent();
                    }
                    // Fallback: just fire editing stopped on our editor
                    // (CellEditor walk not strictly needed here)
                    ta.transferFocus(); // give visual feedback
                }
            }
        });
    }

    // ----- Static completions (functions + snippets) -----

    // in ExpressionCellEditor

    private static void fillFunctionCompletions(DefaultCompletionProvider provider) {
        // Provide functions from registry as parameterized completions
        List<String> functionNames = new ArrayList<String>(ExpressionRegistryImpl.getInstance().getKeys());
        Collections.sort(functionNames, String.CASE_INSENSITIVE_ORDER);

        for (String fn : functionNames) {
            // Use FunctionCompletion and set parameters via setParams(...)
            org.fife.ui.autocomplete.FunctionCompletion c =
                    new org.fife.ui.autocomplete.FunctionCompletion(provider, fn, "String");

            // Use setParams(List<Parameter>) instead of addParam(...)
            c.setParams(createDummyParams()); // 3 dummy params for call tips

            c.setShortDescription("Funktion aus ExpressionRegistry");
            provider.addCompletion(c);
        }
    }

    private static List<org.fife.ui.autocomplete.ParameterizedCompletion.Parameter> createDummyParams() {
        List<org.fife.ui.autocomplete.ParameterizedCompletion.Parameter> params =
                new ArrayList<org.fife.ui.autocomplete.ParameterizedCompletion.Parameter>(3);
        params.add(new org.fife.ui.autocomplete.ParameterizedCompletion.Parameter("String", "p1"));
        params.add(new org.fife.ui.autocomplete.ParameterizedCompletion.Parameter("String", "p2"));
        params.add(new org.fife.ui.autocomplete.ParameterizedCompletion.Parameter("String", "p3"));
        return params;
    }

    private static void fillShorthandCompletions(DefaultCompletionProvider provider) {
        // Insert snippet for a function call skeleton
        provider.addCompletion(new ShorthandCompletion(provider, "fn", "{{${cursor}}()}}", "Funktionsaufruf-Template"));
        // Mustache variable wrapper
        provider.addCompletion(new ShorthandCompletion(provider, "{{", "{{${cursor}}}}", "Mustache-Variable"));
        // Regex placeholder
        provider.addCompletion(new ShorthandCompletion(provider, "regex", "{{REGEX:${cursor}}}", "Regex-Platzhalter"));
    }

    // ----- Dynamic provider -----

    /**
     * Extend DefaultCompletionProvider to inject dynamic completions
     * (variables, regex presets) based on caret context while keeping
     * the base behavior for registered completions.
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                textArea.requestFocusInWindow();
            }
        });
        return scroller;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }
}
