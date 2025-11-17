package de.bund.zrb.ui.tabs;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.SavedEntityEvent;
import java.time.Instant;
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
        btnSave.addActionListener(e -> onSave());

        south.add(btnRevert);
        south.add(btnSave);
        add(south, BorderLayout.SOUTH);
    }

    private void onSave() {
        try {
            saveable.saveChanges();
            publishSavedEvent();
        } catch (Throwable t) {
            javax.swing.JOptionPane.showMessageDialog(this, "Speichern fehlgeschlagen: " + t.getMessage(), "Fehler", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void publishSavedEvent() {
        Object model = extractModel();
        String type = "Unknown";
        String name = null;
        String id = null;
        if (model != null) {
            if (model.getClass().getName().endsWith("RootNode")) type = "Root"; else
            if (model.getClass().getName().endsWith("TestSuite")) type = "Suite"; else
            if (model.getClass().getName().endsWith("TestCase")) type = "Case"; else
            if (model.getClass().getName().endsWith("TestAction")) type = "Action";
            try { id = invoke(model, "getId"); } catch (Exception ignore) {}
            try { name = invoke(model, type.equals("Action")?"getAction":"getName"); } catch (Exception ignore) {}
        }
        ApplicationEventBus.getInstance().publish(new SavedEntityEvent(new SavedEntityEvent.Payload(type, name, id, Instant.now())));
    }

    private String invoke(Object o, String m) throws Exception {
        java.lang.reflect.Method mm = o.getClass().getMethod(m);
        Object v = mm.invoke(o);
        return v==null?null:String.valueOf(v);
    }

    private Object extractModel() {
        // Falls das Kind selbst ein EditorTab ist, versuche modelRef via Reflection zu ermitteln (heuristisch)
        if (content != null) {
            try {
                java.lang.reflect.Field f = content.getClass().getDeclaredField("testCase");
                f.setAccessible(true); return f.get(content);
            } catch (Exception ignore) {}
            try {
                java.lang.reflect.Field f = content.getClass().getDeclaredField("suite");
                f.setAccessible(true); return f.get(content);
            } catch (Exception ignore) {}
            try {
                java.lang.reflect.Field f = content.getClass().getDeclaredField("root");
                f.setAccessible(true); return f.get(content);
            } catch (Exception ignore) {}
            try {
                java.lang.reflect.Field f = content.getClass().getDeclaredField("action");
                f.setAccessible(true); return f.get(content);
            } catch (Exception ignore) {}
        }
        return null;
    }

    public Saveable getSaveable() { return saveable; }
    public Revertable getRevertable() { return revertable; }
}
