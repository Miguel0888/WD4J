package de.bund.zrb.ui;

import com.google.gson.Gson;
import de.bund.zrb.RecordingEventRouter;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.service.BrowserServiceImpl;
import de.bund.zrb.service.RecorderService;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RightDrawer extends JPanel {

    private final BrowserServiceImpl browserService;

    private final JTabbedPane recorderTabs = new JTabbedPane();

    public RightDrawer(BrowserServiceImpl browserService) {
        super(new BorderLayout(8, 8));
        this.browserService = browserService;

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
        RecorderSession session = new RecorderSession();
        int insertIndex = Math.max(recorderTabs.getTabCount() - 1, 0);
        recorderTabs.insertTab(null, null, session, null, insertIndex);
        recorderTabs.setTabComponentAt(insertIndex, createTabTitle("📝 Recorder", session));
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
                session.unregister();
                recorderTabs.remove(index);
            }
        });

        tabPanel.add(closeButton);
        return tabPanel;
    }

    class RecorderSession extends JPanel implements RecordingEventRouter.RecordingEventListener {

        private final ActionTable actionTable;
        private final JToggleButton recordToggle;

        private String contextId;
        private RecorderService recorderService;

        public RecorderSession() {
            super(new BorderLayout(8, 8));
            this.actionTable = new ActionTable();

            this.recordToggle = new JToggleButton("\u2B24");
            recordToggle.setBackground(Color.RED);
            recordToggle.setFocusPainted(false);
            recordToggle.setToolTipText("Start Recording");

            recordToggle.addActionListener(e -> toggleRecording());

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            topPanel.add(recordToggle);

            add(topPanel, BorderLayout.NORTH);
            add(new JScrollPane(actionTable), BorderLayout.CENTER);
        }

        private void toggleRecording() {
            boolean shouldStart = recordToggle.isSelected();

            if (shouldStart) {
                this.contextId = browserService.getBrowser().getPages().getActivePageId();
                this.recorderService = new RecorderService();
                browserService.getRecordingEventRouter().addListener(contextId, this);

                System.out.println("📌 Start Recording for Context: " + contextId);

                recordToggle.setText("\u23F8");
                recordToggle.setToolTipText("Stop Recording");
                recordToggle.setBackground(Color.GRAY);

            } else {
                unregister();
                System.out.println("🛑 Stop Recording for Context: " + contextId);

                recordToggle.setText("\u2B24");
                recordToggle.setToolTipText("Start Recording");
                recordToggle.setBackground(Color.RED);
            }
        }

        public void unregister() {
            if (contextId != null) {
                browserService.getRecordingEventRouter().removeListener(contextId, this);
                contextId = null;
            }
        }

        @Override
        public void onRecordingEvent(WDScriptEvent.Message message) {
            WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();

            List<Map.Entry<WDRemoteValue, WDRemoteValue>> pairs = new ArrayList<>(data.getValue().entrySet());

            for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : pairs) {
                String key = ((WDPrimitiveProtocolValue.StringValue) entry.getKey()).getValue();
                if ("events".equals(key)) {
                    WDRemoteValue.ArrayRemoteValue eventsArray = (WDRemoteValue.ArrayRemoteValue) entry.getValue();

                    // Umwandeln in normale Map, dann als JSON:
                    List<Map<String, Object>> plainEvents = new ArrayList<>();
                    for (WDRemoteValue eventItem : eventsArray.getValue()) {
                        WDRemoteValue.ObjectRemoteValue eventObject = (WDRemoteValue.ObjectRemoteValue) eventItem;

                        Map<String, Object> eventMap = new LinkedHashMap<>();
                        for (Map.Entry<WDRemoteValue, WDRemoteValue> ev : eventObject.getValue().entrySet()) {
                            String evKey = ((WDPrimitiveProtocolValue.StringValue) ev.getKey()).getValue();
                            WDRemoteValue evValue = ev.getValue();
                            if (evValue instanceof WDPrimitiveProtocolValue.StringValue) {
                                eventMap.put(evKey, ((WDPrimitiveProtocolValue.StringValue) evValue).getValue());
                            } else if (evValue instanceof WDRemoteValue.ObjectRemoteValue) {
                                // Ignoriere oder erweitere bei Bedarf
                                eventMap.put(evKey, evValue.toString());
                            }
                        }
                        plainEvents.add(eventMap);
                    }

                    String json = new Gson().toJson(plainEvents);
                    recorderService.recordAction(json);

                    SwingUtilities.invokeLater(() -> {
                        actionTable.setActions(recorderService.getAllTestActionsForDrawer());
                    });
                }
            }
        }

    }
}
