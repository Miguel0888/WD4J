package de.bund.zrb.ui.commandframework;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Converts a MenuBuilder tree into a Swing JMenuBar.
 */
public class SwingMenuAdapter {

    private final CommandRegistry registry;

    public SwingMenuAdapter(CommandRegistry registry) {
        this.registry = registry;
    }

    public JMenuBar build(MenuBuilder.MenuNode root) {
        JMenuBar menuBar = new JMenuBar();
        for (MenuBuilder.MenuNode child : root.getChildren()) {
            menuBar.add(buildMenu(child));
        }
        return menuBar;
    }

    private JMenu buildMenu(MenuBuilder.MenuNode node) {
        JMenu menu = new JMenu(node.getConfig().getLabel());
        for (MenuBuilder.MenuNode child : node.getChildren()) {
            if (child.getChildren().isEmpty()) {
                menu.add(buildMenuItem(child.getConfig()));
            } else {
                menu.add(buildMenu(child));
            }
        }
        return menu;
    }

    private JMenuItem buildMenuItem(MenuItemConfig config) {
        JMenuItem item = new JMenuItem(config.getLabel());
        Command command = registry.getCommand(config.getId());
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                command.execute(new CommandContext());
            }
        });
        return item;
    }
}
