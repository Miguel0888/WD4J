package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

/**
 * Wrapper-Container, der in der SOUTH-Region zwei Buttons (Änderungen verwerfen, Speichern)
 * anzeigt und oben den eigentlichen Editor-Content einbettet. Erwartet, dass der Content
 * Saveable und Revertable implementiert.
 */
public class SaveRevertContainer extends JPanel {
    private final JComponent content;
    private final Saveable saveable;
    private final Revertable revertable;

    public SaveRevertContainer(JComponent content) {
        super(new BorderLayout());
        this.content = content;
        if (!(content instanceof Saveable) || !(content instanceof Revertable)) {
            throw new IllegalArgumentException("Content must implement Saveable and Revertable");
        }
        this.saveable = (Saveable) content;
        this.revertable = (Revertable) content;

        add(content, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRevert = new JButton("Änderungen verwerfen");
        JButton btnSave = new JButton("Speichern");

        btnRevert.addActionListener(e -> revertable.revertChanges());
        btnSave.addActionListener(e -> saveable.saveChanges());

        south.add(btnRevert);
        south.add(btnSave);
        add(south, BorderLayout.SOUTH);
    }

    public Saveable getSaveable() { return saveable; }
    public Revertable getRevertable() { return revertable; }
}

