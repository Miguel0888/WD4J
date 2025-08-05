package de.bund.zrb.ui.tabs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.model.ThenExpectation;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;

public class ThenExpectationEditorTab extends JPanel {

    private final ThenExpectation expectation;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    private static final Gson gson = new Gson();

    public ThenExpectationEditorTab(ThenExpectation expectation) {
        this.expectation = expectation;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        typeBox = new JComboBox<>(ExpectationRegistry.getAllTypes().toArray(new String[0]));
        typeBox.setEditable(false);
        typeBox.setSelectedItem(expectation.getType());
        typeBox.addActionListener(e -> rebuildDynamicForm((String) typeBox.getSelectedItem()));

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel typeRow = new JPanel(new GridLayout(1, 2, 8, 8));
        typeRow.add(new JLabel("Typ:"));
        typeRow.add(typeBox);
        form.add(typeRow, BorderLayout.NORTH);

        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(this::save);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        rebuildDynamicForm(expectation.getType());
    }

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        Map<String, Object> paramMap = expectation.getParameterMap();
        ExpectationType def = ExpectationRegistry.getType(type);
        if (def == null) return;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (ExpectationParam param : def.params) {
            gbc.gridx = 0;
            gbc.weightx = 0;
            dynamicFieldsPanel.add(new JLabel(param.label + ":"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;

            JComponent input;
            Object defaultValue = paramMap.getOrDefault(param.name, param.defaultValue);

            if ("script".equals(param.name)) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(defaultValue != null ? defaultValue.toString() : "");
                input = new RTextScrollPane(editor);
                inputs.put(param.name, editor);
            } else {
                JTextField tf = new JTextField(defaultValue != null ? defaultValue.toString() : "");
                input = tf;
                inputs.put(param.name, tf);
            }

            dynamicFieldsPanel.add(input, gbc);
            gbc.gridy++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        expectation.setType(selectedType);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();
            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            }
        }

        expectation.setParameterMap(result);
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // Registry statisch, kann später extern ausgelagert werden
    private static class ExpectationRegistry {
        private static final Map<String, ExpectationType> TYPES = new LinkedHashMap<>();

        static {
            register(new ExpectationType("screenshot",
                    new ExpectationParam("selector", "CSS-Selektor", "body"),
                    new ExpectationParam("threshold", "Schwellwert", "0.01")
            ));

            register(new ExpectationType("js-eval",
                    new ExpectationParam("script", "JavaScript-Code", "return true;")
            ));
        }

        public static void register(ExpectationType def) {
            TYPES.put(def.name, def);
        }

        public static Set<String> getAllTypes() {
            return TYPES.keySet();
        }

        public static ExpectationType getType(String name) {
            return TYPES.get(name);
        }
    }

    private static class ExpectationType {
        public final String name;
        public final List<ExpectationParam> params;

        public ExpectationType(String name, ExpectationParam... params) {
            this.name = name;
            this.params = Arrays.asList(params);
        }
    }

    private static class ExpectationParam {
        public final String name;
        public final String label;
        public final String defaultValue;
        public final String type;
        public final List<String> options;

        public ExpectationParam(String name, String label, String defaultValue) {
            this(name, label, defaultValue, "text", Collections.emptyList());
        }

        public ExpectationParam(String name, String label, String defaultValue, String type) {
            this(name, label, defaultValue, type, Collections.emptyList());
        }

        public ExpectationParam(String name, String label, String defaultValue, String type, List<String> options) {
            this.name = name;
            this.label = label;
            this.defaultValue = defaultValue;
            this.type = type;
            this.options = options;
        }
    }

}
