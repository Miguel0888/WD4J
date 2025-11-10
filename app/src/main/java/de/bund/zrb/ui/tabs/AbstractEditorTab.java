package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractEditorTab<T> extends JPanel {
    private final T model;
    private final String title;

    public AbstractEditorTab(String title, T model) {
        super(new BorderLayout());
        this.title = title;
        this.model = model;
        // WICHTIG: Kein Tab-Header-Management hier.
        // Persistente Tabs bekommen ihren ClosableTabHeader ausschließlich durch den TabManager.
        // Preview-Tabs bleiben ohne Schließen-Button.
    }

    public T getModel() {
        return model;
    }

    public String getTabTitle() {
        return title;
    }
}
