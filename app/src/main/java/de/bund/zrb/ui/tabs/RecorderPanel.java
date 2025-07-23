package de.bund.zrb.ui.tabs;

import javax.swing.*;
import java.awt.*;

public class RecorderPanel extends JPanel {
    public RecorderPanel() {
        super(new BorderLayout());
        add(new JLabel("Recorder-Modus: Hier werden Tests aufgezeichnet und editiert."), BorderLayout.NORTH);
        add(new JScrollPane(new JTable()), BorderLayout.CENTER);
    }
}
