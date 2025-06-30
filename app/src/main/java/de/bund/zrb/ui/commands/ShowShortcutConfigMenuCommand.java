package de.bund.zrb.ui.commands;


import de.bund.zrb.ui.commandframework.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ShowShortcutConfigMenuCommand extends ShortcutMenuCommand {

    private final JFrame parent;

    public ShowShortcutConfigMenuCommand(JFrame parent) {
        this.parent = parent;
    }

    @Override
    public String getId() {
        return "file.shortcuts";
    }

    @Override
    public String getLabel() {
        return "Shortcuts...";
    }

    @Override
    public void perform() {
        List<MenuCommand> all = new ArrayList<>(CommandRegistryImpl.getInstance().getAll());
        Map<MenuCommand, KeyStrokeField> shortcutFields = new LinkedHashMap<>();

        JPanel commandPanel = new JPanel(new GridLayout(0, 1));

        for (MenuCommand cmd : all) {
            JPanel line = new JPanel(new BorderLayout(4, 0));

            JLabel label = new JLabel(cmd.getLabel());
            KeyStroke initial = ShortcutManager.getKeyStrokeFor(cmd);
            KeyStrokeField field = new KeyStrokeField(initial);
            shortcutFields.put(cmd, field);

            // Fokus-Highlighting
            field.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    field.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    field.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                }
            });

            // Löschen-Button ❌
            JButton clearButton = new JButton("❌");
//            clearButton.setForeground(Color.RED);
            clearButton.setMargin(new Insets(0, 4, 0, 4));
            clearButton.setPreferredSize(new Dimension(28, 24));
            clearButton.setFocusable(false);
            clearButton.setToolTipText("Shortcut entfernen");

            clearButton.addActionListener(e -> field.clear());

            JPanel fieldPanel = new JPanel(new BorderLayout(2, 0));
            fieldPanel.add(field, BorderLayout.CENTER);
            fieldPanel.add(clearButton, BorderLayout.EAST);

            line.add(label, BorderLayout.CENTER);
            line.add(fieldPanel, BorderLayout.EAST);
            commandPanel.add(line);
        }

        JPanel fullPanel = new JPanel(new BorderLayout(8, 8));
        fullPanel.add(new JScrollPane(commandPanel), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(parent, fullPanel,
                "Tastenkombinationen bearbeiten", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            for (Map.Entry<MenuCommand, KeyStrokeField> entry : shortcutFields.entrySet()) {
                KeyStrokeField field = entry.getValue();
                KeyStroke ks = field.getKeyStroke();

                if (ks == null || field.getText().trim().isEmpty()) {
                    ShortcutManager.assignShortcut(entry.getKey().getId(), null); // Löschen
                } else {
                    ShortcutManager.assignShortcut(entry.getKey().getId(), ks);   // Speichern
                }
            }
            ShortcutManager.saveShortcuts();
            ShortcutManager.registerGlobalShortcuts(parent.getRootPane());
        }
    }
}
