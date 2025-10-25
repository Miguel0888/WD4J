package de.bund.zrb.ui;

import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.RecorderCoordinator;
import de.bund.zrb.service.UserContextMappingService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class RightDrawer extends JPanel {

    private final BrowserServiceImpl browserService;
    private final JTabbedPane recorderTabs = new JTabbedPane();

    // Mappe User → RecorderTab, damit wir schnell selektieren können
    private final Map<UserRegistry.User, RecorderTab> tabsByUser = new IdentityHashMap<>();

    // Hört auf „currentUser“-Wechsel im Mapping-Service
    private final PropertyChangeListener currentUserListener = this::onCurrentUserChanged;

    public RightDrawer(BrowserServiceImpl browserService) {
        super(new BorderLayout(8, 8));
        this.browserService = browserService;

        add(recorderTabs, BorderLayout.CENTER);

        addPlusTab();
        openTabsForAllUsers();

        // Plus-Tab Verhalten lassen wir wie gehabt
        recorderTabs.addChangeListener(e -> {
            int plusTabIndex = recorderTabs.getTabCount() - 1;
            int selectedIndex = recorderTabs.getSelectedIndex();
            if (selectedIndex == plusTabIndex && plusTabIndex > 0) {
                recorderTabs.setSelectedIndex(plusTabIndex - 1);
            }
        });

        // 🔌 Auf Mapping-Service hören → Tab mit aktivem User selektieren
        UserContextMappingService.getInstance().addPropertyChangeListener(currentUserListener);

        // Optional: initialen Zustand spiegeln
        UserRegistry.User init = UserContextMappingService.getInstance().getCurrentUser();
        if (init != null) {
            selectTabForUser(init, /*createIfMissing*/ true);
        }
    }

    // Wichtig: zum Aufräumen aufrufen (z.B. beim Fenster schließen)
    public void unregister() {
        UserContextMappingService.getInstance().removePropertyChangeListener(currentUserListener);
    }

    private void onCurrentUserChanged(PropertyChangeEvent evt) {
        if (!"currentUser".equals(evt.getPropertyName())) return;
        UserRegistry.User u = (UserRegistry.User) evt.getNewValue();
        SwingUtilities.invokeLater(() -> selectTabForUser(u, /*createIfMissing*/ true));
    }

    private void selectTabForUser(UserRegistry.User user, boolean createIfMissing) {
        if (user == null) return;
        RecorderTab tab = tabsByUser.get(user);
        if (tab == null && createIfMissing) {
            tab = addRecorderTabForUser(user);
        }
        if (tab != null) {
            int idx = recorderTabs.indexOfComponent(tab);
            if (idx >= 0) recorderTabs.setSelectedIndex(idx);
        }
    }

    private void openTabsForAllUsers() {
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Benutzer vorhanden! Bitte zuerst Benutzer anlegen.",
                    "Fehler", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (UserRegistry.User user : users) {
            addRecorderTabForUser(user);
        }
    }

    private RecorderTab addRecorderTabForUser(UserRegistry.User user) {
        RecorderTab session = new RecorderTab(this, user);
        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, session, null, insertIndex);

        // TabHeader mit rotem Punkt
        TabHeader header = createTabTitle(user, session);
        recorderTabs.setTabComponentAt(insertIndex, header);

        tabsByUser.put(user, session);
        recorderTabs.setSelectedComponent(session);
        return session;
    }

    private void addNewRecorderSession() {
        List<UserRegistry.User> users = UserRegistry.getInstance().getAll();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Benutzer vorhanden! Bitte zuerst Benutzer anlegen.",
                    "Fehler", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserRegistry.User selectedUser = (UserRegistry.User) JOptionPane.showInputDialog(
                this,
                "Bitte wähle einen Benutzer aus:",
                "Neuer Recorder-Tab",
                JOptionPane.PLAIN_MESSAGE,
                null,
                users.toArray(),
                users.get(0)
        );

        if (selectedUser != null) {
            selectTabForUser(selectedUser, /*createIfMissing*/ true);
        }
    }

    private void addPlusTab() {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JButton openButton = new JButton("＋");
        openButton.setMargin(new Insets(0, 0, 0, 0));
        openButton.setBorder(BorderFactory.createEmptyBorder());
        openButton.setFocusable(false);
        openButton.setContentAreaFilled(true);
        openButton.setToolTipText("Neuen Recorder-Tab öffnen");
        openButton.addActionListener(e -> addNewRecorderSession());

        tabPanel.add(openButton);

        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, null, null, insertIndex);
        recorderTabs.setTabComponentAt(insertIndex, tabPanel);
        recorderTabs.setEnabledAt(recorderTabs.getTabCount() - 1, false);
    }

    private TabHeader createTabTitle(UserRegistry.User user, RecorderTab tabContent) {
        String title = "📝 " + user.getUsername();

        Runnable onClose = () -> {
            int index = recorderTabs.indexOfComponent(tabContent);
            if (index >= 0 && index != recorderTabs.getTabCount() - 1) {
                tabContent.unregister();
                recorderTabs.remove(index);
                tabsByUser.entrySet().removeIf(en -> en.getValue() == tabContent);
            }
        };

        Runnable onStopRecording = () ->
                RecorderCoordinator.getInstance().stopForUser(user.getUsername());

        return new TabHeader(title, onClose, onStopRecording);
    }

    public BrowserServiceImpl getBrowserService() {
        return browserService;
    }

    public void setTabRecording(UserRegistry.User user, boolean recording) {
        if (user == null) return;
        SwingUtilities.invokeLater(() -> {
            RecorderTab tab = tabsByUser.get(user);
            if (tab == null) return;
            int idx = recorderTabs.indexOfComponent(tab);
            if (idx < 0) return;
            Component comp = recorderTabs.getTabComponentAt(idx);
            if (comp instanceof TabHeader) {
                ((TabHeader) comp).setRecording(recording);
            }
        });
    }

    // --- Tab-Header mit rotem Aufnahmepunkt ---
    private static final class TabHeader extends JPanel {
        private final JLabel recDot = new JLabel("●"); // roter Punkt
        private final JLabel titleLabel = new JLabel();
        private final JButton closeButton = new JButton("×");
        private boolean recording = false;

        TabHeader(String title, Runnable onClose, Runnable onStopRecordingClick) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            setOpaque(false);

            // roter Punkt (anfangs unsichtbar)
            recDot.setForeground(Color.RED);
            recDot.setVisible(false);
            recDot.setToolTipText("Aufnahme läuft – klicken zum Stoppen");
            recDot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            recDot.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (recording && onStopRecordingClick != null) onStopRecordingClick.run();
                }
            });

            titleLabel.setText(title);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 6));

            closeButton.setMargin(new Insets(0, 5, 0, 5));
            closeButton.setBorder(BorderFactory.createEmptyBorder());
            closeButton.setFocusable(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setToolTipText("Tab schließen");
            closeButton.addActionListener(e -> { if (onClose != null) onClose.run(); });

            add(recDot);
            add(titleLabel);
            add(closeButton);
        }

        void setRecording(boolean on) {
            this.recording = on;
            recDot.setVisible(on);
            // Optional: Titel fett während Aufnahme
            titleLabel.setFont(titleLabel.getFont().deriveFont(on ? Font.BOLD : Font.PLAIN));
            revalidate(); repaint();
        }
    }
}
