package de.bund.zrb.ui;

import de.bund.zrb.RecordingEventRouter;
import de.bund.zrb.dto.RecordedEvent;
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
//            WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();
            List<RecordedEvent> events = extractRecordedEvents(message);

            recorderService.recordAction(events);

            SwingUtilities.invokeLater(() -> {
                actionTable.setActions(recorderService.getAllTestActionsForDrawer());
            });
        }

        private List<RecordedEvent> extractRecordedEvents(WDScriptEvent.Message message) {
            WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();
            List<RecordedEvent> result = new ArrayList<>();

            WDRemoteValue.ArrayRemoteValue eventsArray = null;

            for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : data.getValue().entrySet()) {
                if (entry.getKey() instanceof WDPrimitiveProtocolValue.StringValue) {
                    String key = ((WDPrimitiveProtocolValue.StringValue) entry.getKey()).getValue();
                    if ("events".equals(key)) {
                        eventsArray = (WDRemoteValue.ArrayRemoteValue) entry.getValue();
                        break;
                    }
                }
            }

            if (eventsArray == null) {
                System.err.println("⚠️ Keine Events gefunden!");
                return result;
            }

            for (WDRemoteValue item : eventsArray.getValue()) {
                if (item instanceof WDRemoteValue.ObjectRemoteValue) {
                    WDRemoteValue.ObjectRemoteValue eventObj = (WDRemoteValue.ObjectRemoteValue) item;
                    RecordedEvent event = new RecordedEvent();

                    event.setContextId(message.getParams().getSource().getContext().value());
                    event.setRealmId(message.getParams().getSource().getRealm());

                    for (Map.Entry<WDRemoteValue, WDRemoteValue> pair : eventObj.getValue().entrySet()) {
                        String key = ((WDPrimitiveProtocolValue.StringValue) pair.getKey()).getValue();
                        WDRemoteValue value = pair.getValue();

                        if (value instanceof WDPrimitiveProtocolValue.StringValue) {
                            String val = ((WDPrimitiveProtocolValue.StringValue) value).getValue();
                            switch (key) {
                                case "selector": event.setCss(val); break;
                                case "action": event.setAction(val); break;
                                case "buttonText": event.setButtonText(val); break;
                                case "xpath": event.setXpath(val); break;
                                case "classes": event.setClasses(val); break;
                                case "key": event.setKey(val); break;
                                case "value": event.setValue(val); break;
                                default: break;
                            }
                        }

                        if (value instanceof WDRemoteValue.ObjectRemoteValue) {
                            WDRemoteValue.ObjectRemoteValue objVal = (WDRemoteValue.ObjectRemoteValue) value;
                            Map<String, String> map = new LinkedHashMap<>();
                            for (Map.Entry<WDRemoteValue, WDRemoteValue> attr : objVal.getValue().entrySet()) {
                                String attrKey = ((WDPrimitiveProtocolValue.StringValue) attr.getKey()).getValue();
                                if (attr.getValue() instanceof WDPrimitiveProtocolValue.StringValue) {
                                    String attrVal = ((WDPrimitiveProtocolValue.StringValue) attr.getValue()).getValue();
                                    map.put(attrKey, attrVal);
                                }
                            }

                            if ("aria".equals(key)) event.setAria(map);
                            if ("attributes".equals(key)) event.setAttributes(map);
                        }
                    }

                    result.add(event);
                }
            }

            return result;
        }
    }
}
