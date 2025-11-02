package de.bund.zrb.ui.giveneditor;

import de.bund.zrb.service.TestRegistry;

import javax.swing.*;
import java.awt.*;

public class MapTablePanel extends JPanel {

    public MapTablePanel(final java.util.Map<String,String> backing,
                         final String scopeName) {
        super(new BorderLayout());

        final MapTableModel model = new MapTableModel(backing);
        final JTable table = new JTable(model);

        // Toolbar
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton addBtn = new JButton("+");
        addBtn.setToolTipText(scopeName + " Eintrag hinzufügen");
        addBtn.addActionListener(e -> {
            model.addEmptyRow();
        });

        JButton delBtn = new JButton("–");
        delBtn.setToolTipText("Ausgewählte Zeile löschen");
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                model.removeRow(row);
            }
        });

        JButton saveBtn = new JButton("💾");
        saveBtn.setToolTipText("Speichern");
        saveBtn.addActionListener(e -> {
            TestRegistry.getInstance().save();
            JOptionPane.showMessageDialog(
                    MapTablePanel.this,
                    "Gespeichert.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        bar.add(addBtn);
        bar.add(delBtn);
        bar.addSeparator();
        bar.add(saveBtn);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}
