package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

public class UIHelper {
    public static JTabbedPane findTabbedPane(JFrame frame) {
        for (Component comp : frame.getContentPane().getComponents()) {
            if (comp instanceof JSplitPane) {
                JSplitPane outerSplit = (JSplitPane) comp;
                Component right = outerSplit.getRightComponent();
                if (right instanceof JSplitPane) {
                    JSplitPane innerSplit = (JSplitPane) right;
                    Component center = innerSplit.getLeftComponent();
                    if (center instanceof JPanel) {
                        JPanel panel = (JPanel) center;
                        for (Component child : panel.getComponents()) {
                            if (child instanceof JTabbedPane) {
                                return (JTabbedPane) child;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
