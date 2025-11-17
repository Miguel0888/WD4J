package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.settings.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsCommand extends ShortcutMenuCommand {

    private JDialog dialog;

    private final List<SettingsSubPanel> panels = new ArrayList<>();
    private final StretchyCardsPanel cards = new StretchyCardsPanel();
    private JList<String> nav;

    @Override
    public String getId() { return "file.configure"; }

    @Override
    public String getLabel() { return "Einstellungen..."; }

    @Override
    public void perform() {
        dialog = new JDialog((Frame) null, "Einstellungen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(buildContent());
        // Adaptive Größe mit Maximalbreite und -höhe, Scrollbars übernehmen Überlauf
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = 1400; // Maximalbreite
        int maxH = 1000; // optionale Maximalhöhe
        int targetW = Math.min((int) Math.round(screen.width * 0.9), maxW);
        int targetH = Math.min((int) Math.round(screen.height * 0.9), maxH);
        dialog.setSize(new Dimension(Math.max(900, targetW), Math.max(600, targetH)));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Panels registrieren (Reihenfolge = Navigation)
        panels.clear();
        panels.add(new GeneralSettingsPanel());   // enthält UI-Optionen
        panels.add(new BrowserSettingsPanel());
        panels.add(new RecordingSettingsPanel());
        panels.add(new InputSettingsPanel());
        panels.add(new NetworkSettingsPanel());
        panels.add(new ReportSettingsPanel());
        panels.add(new DebugSettingsPanel());
        panels.add(new OverlaySettingsPanel()); // NEU: Overlay-Einstellungen

        // Navigation links
        DefaultListModel<String> model = new DefaultListModel<>();
        for (SettingsSubPanel p : panels) model.addElement(p.getTitle());
        nav = new JList<>(model);
        nav.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nav.setVisibleRowCount(Math.min(12, model.size()));
        nav.setFixedCellHeight(26);
        nav.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        nav.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPanel(nav.getSelectedIndex());
        });
        JScrollPane leftScroll = new JScrollPane(nav,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Cards rechts (in ScrollPane, beide Richtungen AS_NEEDED)
        cards.removeAll();
        for (SettingsSubPanel p : panels) {
            p.loadFromSettings();
            cards.add(p.getComponent(), p.getId());
        }
        JScrollPane rightScroll = new JScrollPane(cards,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.25);
        split.setContinuousLayout(true);
        root.add(split, BorderLayout.CENTER);

        // Footer (Buttons)
        JPanel footer = buildFooter();
        root.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        root.add(footer, BorderLayout.SOUTH);

        // Erste Auswahl
        if (model.getSize() > 0) {
            nav.setSelectedIndex(0);
            showPanel(0);
        }
        return root;
    }

    private void showPanel(int index) {
        if (index < 0 || index >= panels.size()) return;
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, panels.get(index).getId());
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());

        JButton btOpenAppFolder = new JButton("App-Ordner öffnen");
        btOpenAppFolder.setToolTipText("Öffnet den Einstellungsordner (.wd4j) im Explorer.");
        btOpenAppFolder.setFocusable(false);
        btOpenAppFolder.addActionListener(e -> openAppFolder());

        JButton btSystem = new JButton("Systemeinstellungen");
        btSystem.setToolTipText("Browser-Pfade, Profile und WebSocket-Defaults konfigurieren");
        btSystem.setFocusable(false);
        btSystem.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(dialog);
            new de.bund.zrb.ui.system.BrowserSystemSettingsDialog(owner).setVisible(true);
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(btOpenAppFolder);
        left.add(btSystem);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btApply = new JButton("Übernehmen");
        JButton btOk    = new JButton("OK");
        JButton btCancel= new JButton("Abbrechen");

        btApply.addActionListener(e -> applyAll(false));
        btOk.addActionListener(e -> applyAll(true));
        btCancel.addActionListener(e -> dialog.dispose());

        right.add(btApply);
        right.add(btOk);
        right.add(btCancel);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void applyAll(boolean closeAfter) {
        try {
            // Sammle alle Werte aus Subpanels
            Map<String,Object> values = new LinkedHashMap<>();
            for (SettingsSubPanel p : panels) p.putTo(values);

            // Persistiere
            for (Map.Entry<String,Object> e : values.entrySet()) {
                SettingsService.getInstance().set(e.getKey(), e.getValue());
            }
            // Laufzeitwerte aktualisieren
            SettingsService.initAdapter();

            if (closeAfter) dialog.dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Eingabefehler", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, String.valueOf(ex), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAppFolder() {
        try {
            Path base = SettingsService.getInstance().getBasePath();
            if (base == null) {
                JOptionPane.showMessageDialog(dialog, "Basisordner konnte nicht ermittelt werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", base.toAbsolutePath().toString()).start();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(base.toFile());
            } else {
                JOptionPane.showMessageDialog(dialog, "Öffnen des Ordners wird auf diesem System nicht unterstützt.", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Konnte Ordner nicht öffnen:\n" + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Panel, das sich in der Breite an den Viewport anpasst, um horizontale Scrollbars zu vermeiden
    private static final class StretchyCardsPanel extends JPanel implements Scrollable {
        StretchyCardsPanel() { super(new CardLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(visibleRect.height - 32, 32); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
