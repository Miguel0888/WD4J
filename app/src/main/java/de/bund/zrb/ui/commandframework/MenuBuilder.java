package de.bund.zrb.ui.commandframework;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a hierarchical menu tree.
 */
public class MenuBuilder {

    public static class MenuNode {
        private final MenuItemConfig config;
        private final List<MenuNode> children = new ArrayList<MenuNode>();

        public MenuNode(MenuItemConfig config) {
            this.config = config;
        }

        public void addChild(MenuNode child) {
            children.add(child);
        }

        public MenuItemConfig getConfig() {
            return config;
        }

        public List<MenuNode> getChildren() {
            return children;
        }
    }

    private final MenuNode root = new MenuNode(null);

    public MenuNode getRoot() {
        return root;
    }
}
