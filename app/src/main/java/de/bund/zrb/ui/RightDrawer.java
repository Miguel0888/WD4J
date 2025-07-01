package de.bund.zrb.ui;

import de.bund.zrb.controller.RecordingEventRouter;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.RecorderService;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class RightDrawer extends JPanel {

    private final BrowserServiceImpl browserService;
    private final RecordingEventRouter router;

    private final JTabbedPane recorderTabs = new JTabbedPane();

    public RightDrawer(BrowserServiceImpl browserService) {
        super(new BorderLayout(8, 8));
        this.browserService = browserService;
        this.router = new RecordingEventRouter();

        add(recorderTabs, BorderLayout.CENTER);

        addPlusTab();
        addNewRecorderSession();

        recorderTabs.addChangeListener(e -> {
            int plusTabIndex = recorderTabs.getTabCount() - 1;
            int selectedIndex = recorderTabs.getSelectedIndex();
            if (selectedIndex == plusTabIndex && plusTabIndex > 0) {
                recorderTabs.setSelectedIndex(plusTabIndex - 1);
            }
        });
    }

    private void addNewRecorderSession() {
        RecorderSession session = new RecorderSession(browserService);
        String shortId = session.getContextId().substring(0, 6);

        router.addRecorder(session.getContextId(), session.getRecorderService());

        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, session, null, insertIndex);
        recorderTabs.setTabComponentAt(insertIndex, createTabTitle("📝 " + shortId, session));
        recorderTabs.setSelectedComponent(session);
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

    private Component createTabTitle(String title, Component tabContent) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        tabPanel.add(titleLabel);

        JButton closeButton = new JButton("×");
        closeButton.setMargin(new Insets(0, 5, 0, 5));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setFocusable(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setToolTipText("Tab schließen");

        closeButton.addActionListener(e -> {
            int index = recorderTabs.indexOfComponent(tabContent);
            if (index >= 0 && index != recorderTabs.getTabCount() - 1) {
                RecorderSession session = (RecorderSession) tabContent;
                router.removeRecorder(session.getContextId());
                recorderTabs.remove(index);
            }
        });

        tabPanel.add(closeButton);
        return tabPanel;
    }
}
