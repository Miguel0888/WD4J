package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

public class RunnerPanel extends JPanel {
    public RunnerPanel() {
        super(new BorderLayout());
        add(new JLabel("Test Runner: Hier können Tests verwaltet und ausgeführt werden."), BorderLayout.NORTH);
        add(new JScrollPane(new JList<>()), BorderLayout.CENTER);
    }
}