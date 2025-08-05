package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.ThenExpectation;
import de.bund.zrb.model.ExpectationRegistry;
import de.bund.zrb.model.ExpectationTypeDefinition;
import de.bund.zrb.model.ExpectationTypeDefinition.ExpectationField;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class ThenExpectationEditorTab extends JPanel {

    private final ThenExpectation expectation;
    private final JComboBox<String> typeBox;
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    public ThenExpectationEditorTab(ThenExpectation expectation) {
        this.expectation = expectation;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lade alle Typen aus dem model.ExpectationRegistry
        typeBox = new JComboBox<>(ExpectationRegistry.getInstance().getAll()
                .stream().map(ExpectationTypeDefinition::getType).toArray(String[]::new));
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
        ExpectationTypeDefinition def = ExpectationRegistry.getInstance().get(type);
        if (def == null) return;

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (ExpectationField field : def.getFields().values()) {
            gbc.gridx = 0;
            gbc.weightx = 0;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;

            Object value = paramMap.getOrDefault(field.name, field.defaultValue);
            JComponent input;

            if ("script".equals(field.name)) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? value.toString() : "");
                input = new RTextScrollPane(editor);
                inputs.put(field.name, editor);
            } else if (field.type == Boolean.class) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(Boolean.TRUE.equals(value));
                input = checkBox;
                inputs.put(field.name, checkBox);
            } else if (field.type == Integer.class || field.type == Double.class) {
                Number number = (value instanceof Number)
                        ? (Number) value
                        : parseNumber(value, field.type);
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                        number != null ? number.doubleValue() : 0.0,
                        0, 1_000_000, 1));
                input = spinner;
                inputs.put(field.name, spinner);
            } else if (!field.options.isEmpty()) {
                JComboBox<Object> comboBox = new JComboBox<>(field.options.toArray());
                comboBox.setSelectedItem(value);
                input = comboBox;
                inputs.put(field.name, comboBox);
            } else {
                JTextField tf = new JTextField(value != null ? value.toString() : "");
                input = tf;
                inputs.put(field.name, tf);
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

            Object value = null;
            if (input instanceof JTextField) {
                value = ((JTextField) input).getText();
            } else if (input instanceof RSyntaxTextArea) {
                value = ((RSyntaxTextArea) input).getText();
            } else if (input instanceof JCheckBox) {
                value = ((JCheckBox) input).isSelected();
            } else if (input instanceof JSpinner) {
                value = ((JSpinner) input).getValue();
            } else if (input instanceof JComboBox) {
                value = ((JComboBox<?>) input).getSelectedItem();
            }

            result.put(name, value);
        }

        expectation.setParameterMap(result);
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    private Number parseNumber(Object value, Class<?> targetType) {
        try {
            String str = value != null ? value.toString() : "0";
            if (targetType == Integer.class) {
                return Integer.parseInt(str);
            } else if (targetType == Double.class) {
                return Double.parseDouble(str);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }

}
