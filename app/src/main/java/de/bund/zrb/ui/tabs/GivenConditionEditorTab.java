package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.ui.parts.Code;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Editor für eine GivenCondition – nur ergänzt um eine User-ComboBox wie im ActionEditorTab.
 */
public class GivenConditionEditorTab extends JPanel {

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JComboBox<String> userBox;

    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ----- Header: Typ + User (wie im When-Tab) -----
        JPanel header = new JPanel(new GridLayout(0, 2, 8, 8));

        // Typ
        header.add(new JLabel("Typ:"));
        String[] types = GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new);
        typeBox = new JComboBox<>(types);
        typeBox.setEditable(true);
        typeBox.setSelectedItem(condition.getType());
        header.add(typeBox);

        // User (wie im ActionEditorTab)
        header.add(new JLabel("User:"));
        String[] users = UserRegistry.getInstance().getAll().stream()
                .map(UserRegistry.User::getUsername)
                .toArray(String[]::new);
        userBox = new JComboBox<>(users);
        // Vorbelegung aus Param-String (username=…)
        String initialUser = (String) parseValueMap(condition.getValue()).get("username");
        if (initialUser != null && !initialUser.trim().isEmpty()) {
            userBox.setSelectedItem(initialUser.trim());
        }
        header.add(userBox);

        // Container (oben Header, unten dynamische Felder)
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(header, BorderLayout.NORTH);
        form.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        // Rebuild dynamische Felder bei Typwechsel
        typeBox.addActionListener(e -> rebuildDynamicForm((String) typeBox.getSelectedItem()));

        // Footer (Speichern)
        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(this::save);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        // initial aufbauen
        rebuildDynamicForm(condition.getType());
    }

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) { revalidate(); repaint(); return; }

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        for (GivenField field : def.getFields().values()) {
            Object value = paramMap.getOrDefault(field.name, field.defaultValue);

            if (field.type == Code.class) {
                // Label (volle Breite)
                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                // Editor (volle Breite, wächst)
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? value.toString() : "");
                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                gbc.gridy = row++; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor);
                continue;
            }

            // Label (links)
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            // Input (rechts) – Standard: Textfeld
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
            JTextField tf = new JTextField(value != null ? value.toString() : "");
            dynamicFieldsPanel.add(tf, gbc);
            inputs.put(field.name, tf);
            row++;
        }

        revalidate();
        repaint();
    }

    private void save(ActionEvent e) {
        // Typ speichern
        condition.setType(String.valueOf(typeBox.getSelectedItem()));

        Map<String, String> result = new LinkedHashMap<>();

        // User speichern (wie im When-Tab)
        Object u = userBox.getSelectedItem();
        if (u != null && u.toString().trim().length() > 0) {
            result.put("username", u.toString().trim());
        }

        // Dynamische Felder speichern
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();

            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            }
        }

        condition.setValue(serializeValueMap(result));
        TestRegistry.getInstance().save();
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // ---------- Helpers ----------

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) result.put(kv[0], kv[1]);
            }
        }
        return result;
    }

    private String serializeValueMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }
}
