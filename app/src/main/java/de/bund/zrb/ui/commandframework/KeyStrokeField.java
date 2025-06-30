package de.bund.zrb.ui.commandframework;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyStrokeField extends JTextField {

    private KeyStroke keyStroke;

    public KeyStrokeField(KeyStroke initial) {
        super(20); // Sichtbare Breite
        setEditable(false); // Benutzer soll nicht tippen, sondern Tastenkombination drücken
        this.keyStroke = initial;

        if (initial != null) {
            setText(toText(initial));
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                setText(toText(keyStroke));
            }
        });
    }

    public KeyStroke getKeyStroke() {
        return keyStroke;
    }

    private String toText(KeyStroke ks) {
        if (ks == null) return "";
        return ks.toString().replace("pressed ", "");
    }

    public void clear() {
        setText("");
        keyStroke = null;
    }

}
