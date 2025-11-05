package de.bund.zrb.ui.commands;

import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.settings.ExpressionEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Open the ExpressionEditorPanel in a modal dialog.
 *
 * Intent:
 * - Allow the user to manage dynamic expression functions (otp(), wrap(), etc.)
 *   from the main application's menu.
 *
 * Behavior:
 * - Provide "Speichern" to persist current registry state.
 * - Provide "Schließen" to close the dialog.
 *
 * Note:
 * - This command mirrors the pattern of SettingsCommand, but focuses purely
 *   on expression / function maintenance.
 */
public class ExpressionEditorCommand extends ShortcutMenuCommand {

    private JDialog dialog;

    /**
     * Return the unique command id used by the menu/command framework.
     * This must be "file.expressions" as specified.
     */
    @Override
    public String getId() {
        return "file.expressions";
    }

    /**
     * Return the menu label shown to the user.
     */
    @Override
    public String getLabel() {
        return "Expressions / Funktionen...";
    }

    /**
     * Open the modal dialog that contains the ExpressionEditorPanel.
     * Block until the user closes the dialog.
     */
    @Override
    public void perform() {
        final ExpressionEditorPanel panel = new ExpressionEditorPanel();

        dialog = new JDialog((Frame) null, "Expressions / Funktionen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        // Center: the editor panel
        dialog.add(panel, BorderLayout.CENTER);

        // South: footer with action buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton btSave   = new JButton("OK");
        JButton btClose  = new JButton("Abbrechen");

        btSave.setToolTipText("Speichern und Schließen");
        btClose.setToolTipText("Schließe den Dialog.");

        btSave.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                panel.saveChanges();
                dialog.dispose();
            }
        });

        btClose.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dialog.dispose();
            }
        });

        footer.add(btSave);
        footer.add(btClose);

        dialog.add(footer, BorderLayout.SOUTH);

        // Show dialog centered
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
