package de.bund.zrb.controller;

import de.bund.zrb.service.RecorderService;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;
import com.google.gson.Gson;

import java.util.*;

public class RecordingEventRouter {
    private final Map<String, RecorderService> contextRecorders = new HashMap<>();
    private final Gson gson = new Gson();

    public void addRecorder(String contextId, RecorderService recorder) {
        contextRecorders.put(contextId, recorder);
    }

    public void removeRecorder(String contextId) {
        contextRecorders.remove(contextId);
    }

    public void dispatch(WDScriptEvent.Message message) {
        String contextId = message.getParams().getSource().getContext().value();

        RecorderService recorder = contextRecorders.computeIfAbsent(contextId, id -> {
            System.out.printf("⚙️ Neuer Recorder für Context %s erstellt%n", id);
            return new RecorderService(); // KEIN Singleton, einfach new!
        });

        List<?> events = extractRecordedEvents(message);
        String json = gson.toJson(events);
        recorder.recordAction(json);
    }


    private List<Map<String, Object>> extractRecordedEvents(WDScriptEvent.Message message) {
        List<Map<String, Object>> extracted = new ArrayList<>();

        WDRemoteValue.ObjectRemoteValue remoteValue = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();
        Optional<Map.Entry<WDRemoteValue, WDRemoteValue>> eventsEntry = remoteValue.getValue().entrySet().stream()
                .filter(e -> e.getKey() instanceof WDPrimitiveProtocolValue.StringValue &&
                        "events".equals(((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue()))
                .findFirst();

        if (eventsEntry.isPresent()) {
            WDRemoteValue.ArrayRemoteValue eventsArray = (WDRemoteValue.ArrayRemoteValue) eventsEntry.get().getValue();

            for (WDRemoteValue item : eventsArray.getValue()) {
                WDRemoteValue.ObjectRemoteValue eventObject = (WDRemoteValue.ObjectRemoteValue) item;

                Map<String, Object> dto = new LinkedHashMap<>();
                WDRemoteValue.ObjectRemoteValue obj = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();
                for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : obj.getValue().entrySet()) {
                    String key = ((WDPrimitiveProtocolValue.StringValue) entry.getKey()).getValue();
                    if (entry.getValue() instanceof WDPrimitiveProtocolValue.StringValue) {
                        dto.put(key, ((WDPrimitiveProtocolValue.StringValue) entry.getValue()).getValue());
                    }
                    // Optional: Arrays, Objekte etc.
                }
                extracted.add(dto);
            }
        }

        return extracted;
    }
}
