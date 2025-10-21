package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.GivenCondition;
import de.bund.zrb.model.GivenRegistry;
import de.bund.zrb.model.GivenTypeDefinition;
import de.bund.zrb.model.GivenTypeDefinition.GivenField;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;
import de.bund.zrb.ui.parts.Code;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Editor für eine GivenCondition.
 * Ergänzt globales "User:"-Dropdown (per Given), schreibt/liest es als "username" in der Parameter-Map.
 * Unterdrückt ein dynamisches Feld "username" aus dem GivenType-Definition-Formular (Vermeidung Doppelanzeige).
 */
public class GivenConditionEditorTab extends JPanel {

    private final GivenCondition condition;
    private final JComboBox<String> typeBox;
    private final JComboBox<String> userBox; // <— NEU: User-Auswahl pro Given
    private final JPanel dynamicFieldsPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> inputs = new LinkedHashMap<String, JComponent>();

    public GivenConditionEditorTab(GivenCondition condition) {
        this.condition = condition;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Typ-Auswahl (wie gehabt)
        typeBox = new JComboBox<String>(GivenRegistry.getInstance().getAll()
                .stream().map(GivenTypeDefinition::getType).toArray(String[]::new));
        typeBox.setSelectedItem(condition.getType());
        typeBox.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                rebuildDynamicForm((String) typeBox.getSelectedItem());
            }
        });

        // User-Auswahl (NEU): füllt aus UserRegistry; wählt vorhandenen username aus Param-Map vor
        List<String> users = UserRegistry.getInstance().getAll().stream()
                .map(User::getUsername).collect(Collectors.<String>toList());
        userBox = new JComboBox<String>(users.toArray(new String[0]));
        userBox.setEditable(false);
        String initialUser = readUsernameFromParamMap(condition.getValue());
        if (initialUser != null && initialUser.trim().length() > 0) {
            userBox.setSelectedItem(initialUser.trim());
        } else if (userBox.getItemCount() > 0) {
            userBox.setSelectedIndex(0);
        }

        // Obere Formular-Zeilen
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Typ:"));
        form.add(typeBox);
        form.add(new JLabel("User:"));
        form.add(userBox);

        // Dynamischer Feldbereich (darunter)
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(form, BorderLayout.NORTH);
        center.add(dynamicFieldsPanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Speichern
        JButton saveBtn = new JButton("Speichern");
        saveBtn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                save(e);
            }
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        rebuildDynamicForm(condition.getType());
    }

    // ---------- UI-Aufbau ----------

    private void rebuildDynamicForm(String type) {
        dynamicFieldsPanel.removeAll();
        inputs.clear();

        GivenTypeDefinition def = GivenRegistry.getInstance().get(type);
        if (def == null) {
            revalidate(); repaint();
            return;
        }

        Map<String, Object> paramMap = parseValueMap(condition.getValue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        for (GivenField field : def.getFields().values()) {
            // Unterdrücke das Feld "username" hier, weil es global über userBox bearbeitet wird
            if ("username".equals(field.name)) {
                continue;
            }

            Object value = paramMap.containsKey(field.name) ? paramMap.get(field.name) : field.defaultValue;
            JComponent input;

            if (field.type == Code.class) {
                RSyntaxTextArea editor = new RSyntaxTextArea(10, 40);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                editor.setCodeFoldingEnabled(true);
                editor.setText(value != null ? String.valueOf(value) : "");

                RTextScrollPane scrollPane = new RTextScrollPane(editor);

                // Label (volle Breite)
                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 2;
                gbc.weightx = 1.0;
                gbc.weighty = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dynamicFieldsPanel.add(new JLabel(field.label), gbc);

                // Editor (volle Breite, wächst)
                gbc.gridy = row++;
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weighty = 1.0;
                dynamicFieldsPanel.add(scrollPane, gbc);

                inputs.put(field.name, editor);
                continue;
            }

            // Label (linke Spalte)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.NONE;
            dynamicFieldsPanel.add(new JLabel(field.label + ":"), gbc);

            // Input (rechte Spalte)
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Einfache Textfelder (weitere Feldtypen kannst du analog ergänzen)
            JTextField tf = new JTextField(value != null ? String.valueOf(value) : "");
            input = tf;
            dynamicFieldsPanel.add(input, gbc);

            inputs.put(field.name, input);
            row++;
        }

        revalidate();
        repaint();
    }

    // ---------- Speichern ----------

    private void save(ActionEvent e) {
        String selectedType = (String) typeBox.getSelectedItem();
        condition.setType(selectedType);

        // Alle dynamischen Felder einsammeln
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JComponent> entry : inputs.entrySet()) {
            String name = entry.getKey();
            JComponent input = entry.getValue();
            if (input instanceof JTextField) {
                result.put(name, ((JTextField) input).getText());
            } else if (input instanceof RSyntaxTextArea) {
                result.put(name, ((RSyntaxTextArea) input).getText());
            }
        }

        // NEU: User aus Dropdown als "username" setzen (überschreibt ggf. vorhandenen Wert)
        Object u = userBox.getSelectedItem();
        if (u != null && String.valueOf(u).trim().length() > 0) {
            result.put("username", String.valueOf(u).trim());
        } else {
            result.remove("username");
        }

        condition.setValue(serializeValueMap(result));

        TestRegistry.getInstance().save();
        JOptionPane.showMessageDialog(this, "Änderungen gespeichert.");
    }

    // ---------- Param-Map Utils ----------

    private String readUsernameFromParamMap(String value) {
        Map<String, Object> map = parseValueMap(value);
        Object v = map.get("username");
        return (v == null) ? null : String.valueOf(v);
    }

    private Map<String, Object> parseValueMap(String value) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (value != null && value.contains("=")) {
            String[] pairs = value.split("&");
            for (int i = 0; i < pairs.length; i++) {
                String pair = pairs[i];
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
