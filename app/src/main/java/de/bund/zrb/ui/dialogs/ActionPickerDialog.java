package de.bund.zrb.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/** Show a small modal dialog to choose an action (e.g., click, input...). Keep editable for custom entries. */
public class ActionPickerDialog extends JDialog {

    private final JComboBox<String> actionBox;
    private boolean confirmed;

    public ActionPickerDialog(Window owner, String title, String currentAction) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Known actions (keep in sync with ActionEditorTab)
        Set<String> knownActions = new TreeSet<String>(Arrays.asList(
                "click", "input", "select", "check", "radio", "screenshot"
        ));

        actionBox = new JComboBox<String>(knownActions.toArray(new String[0]));
        actionBox.setEditable(true);
        if (currentAction != null && currentAction.trim().length() > 0) {
            actionBox.setSelectedItem(currentAction.trim());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Action:"));
        form.add(actionBox);

        // --- Buttons ---
        JButton ok = new JButton("OK");
        ok.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        JButton cancel = new JButton("Abbrechen");
        cancel.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        // Default-Button für Enter
        getRootPane().setDefaultButton(ok);

        // ESC zum Abbrechen
        getRootPane().registerKeyboardAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Panel rechtsbündig, aber OK links von Abbrechen -> Reihenfolge: OK dann Cancel
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);      // OK links
        south.add(cancel);  // Abbrechen rechts


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    /** Return true if user confirmed with OK. */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** Return the chosen action string or empty string when none. */
    public String getChosenAction() {
        Object v = actionBox.getSelectedItem();
        return (v == null) ? "" : String.valueOf(v).trim();
    }
}
