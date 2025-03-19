package app.ui;

import app.controller.MainController;

import javax.swing.*;
import java.util.Vector;

public class ContextTab implements UIComponent {
    public final MainController controller;
    public final JToolBar contextToolbar;
    private JComboBox<Object> userContextDropdown;
    private JComboBox<Object> browsingContextDropdown;

    public ContextTab(MainController controller) {
        this.controller = controller;
        this.contextToolbar = createContextsToolbar();
    }

    private JToolBar createContextsToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // User Context Combobox (leere Liste)
        userContextDropdown = new JComboBox<>(new DefaultComboBoxModel<>(new Vector<>()));
        userContextDropdown.addItem("default"); // Standardwert
        userContextDropdown.addActionListener(e -> controller.updateSelectedUserContext());

        // Browsing Context Combobox (leere Liste)
        browsingContextDropdown = new JComboBox<>(new DefaultComboBoxModel<>(new Vector<>()));
        browsingContextDropdown.addActionListener(e -> controller.switchSelectedPage());

        JButton newContext = new JButton("+");
        newContext.setToolTipText("Create new browsing context");
        newContext.addActionListener(e -> controller.createBrowsingContext());
        JButton closeContext = new JButton("-");
        closeContext.setToolTipText("Close browsing context");
        closeContext.addActionListener(e -> controller.closePage());

        // Labels & Dropdowns hinzufügen
        toolbar.add(new JLabel("User Context:"));
        toolbar.add(userContextDropdown);
        toolbar.add(new JLabel("Browsing Context:"));
        toolbar.add(browsingContextDropdown);
        toolbar.add(newContext);
        toolbar.add(closeContext);

        return toolbar;
    }

    public JComboBox<Object> getBrowsingContextDropdown() {
        return browsingContextDropdown;
    }

    public JComboBox<Object> getUserContextDropdown() {
        return userContextDropdown;
    }

    public JToolBar getToolbar() {
        return contextToolbar;
    }

    @Override
    public String getComponentTitle() {
        return "Contexts";
    }
}
