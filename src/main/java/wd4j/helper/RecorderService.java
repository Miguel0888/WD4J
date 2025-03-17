package wd4j.helper;

import app.dto.TestAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import wd4j.helper.dto.RecordedEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecorderService {
    private static RecorderService instance;
    private List<RecordedEvent> recordedEvents = new ArrayList<>();
    private final Gson gson = new Gson();

    private RecorderService() {}

    public static RecorderService getInstance() {
        if (instance == null) {
            synchronized (RecorderService.class) {
                if (instance == null) {
                    instance = new RecorderService();
                }
            }
        }
        return instance;
    }

    public void recordAction(String message) {
        try {
            Type listType = new TypeToken<List<RecordedEvent>>() {}.getType();
            List<RecordedEvent> events = gson.fromJson(message, listType);

            if (events != null) {
                recordedEvents.addAll(events);
                System.out.println("📌 Gespeicherte Events: " + events.size());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Fehler beim Parsen der WebSocket-Daten: " + e.getMessage());
        }
    }

    public TestAction convertToTestAction(RecordedEvent event) {
        TestAction action = new TestAction();
        action.setAction(event.getAction());
        action.setSelectedSelector(event.getSelector());
        action.setLocatorType(event.getXpath() != null ? "xpath" : "css"); // Priorität für XPath
        action.setValue(event.getValue());
        action.setTimeout(3000); // Standard-Timeout

        return action;
    }

    public List<RecordedEvent> getRecordedEvents() {
        return new ArrayList<>(recordedEvents);
    }

    public void clearRecordedEvents() {
        recordedEvents.clear();
    }

    public void mergeInputEvents() {
        if (recordedEvents.isEmpty()) return;

        List<RecordedEvent> mergedEvents = new ArrayList<>();
        RecordedEvent lastInputEvent = null;

        for (RecordedEvent event : recordedEvents) {
            if ("input".equals(event.getAction())) {
                if (lastInputEvent != null && isSameExceptValue(lastInputEvent, event)) {
                    // Falls nur der Text unterschiedlich ist → Wert aktualisieren
                    lastInputEvent.setValue(event.getValue());
                } else {
                    // Anderes Feld oder komplett neues Input-Event → speichern
                    if (lastInputEvent != null) {
                        mergedEvents.add(lastInputEvent);
                    }
                    lastInputEvent = event;
                }
            } else if ("press".equals(event.getAction())) {
                // Ignoriere `press`, wenn es nur zwischen `input`-Events passiert
                continue;
            } else {
                // Anderes Event → vorherigen Input speichern, falls vorhanden
                if (lastInputEvent != null) {
                    mergedEvents.add(lastInputEvent);
                    lastInputEvent = null;
                }
                mergedEvents.add(event);
            }
        }

        // Letztes Input-Event hinzufügen, falls am Ende der Liste
        if (lastInputEvent != null) {
            mergedEvents.add(lastInputEvent);
        }

        recordedEvents = mergedEvents; // Alte Liste durch bereinigte ersetzen
    }

    /**
     * Vergleicht zwei RecordedEvent-Objekte, ob sie identisch sind – außer beim `value`.
     */
    private boolean isSameExceptValue(RecordedEvent a, RecordedEvent b) {
        return a.getSelector().equals(b.getSelector()) &&
                a.getAction().equals(b.getAction()) &&
                Objects.equals(a.getElementId(), b.getElementId()) &&
                Objects.equals(a.getClasses(), b.getClasses()) &&
                Objects.equals(a.getXpath(), b.getXpath()) &&
                Objects.equals(a.getAria(), b.getAria()) &&
                Objects.equals(a.getAttributes(), b.getAttributes()) &&
                Objects.equals(a.getTest(), b.getTest());
    }


    public List<String> getSelectorAlternatives(String selector) {
        List<String> alternatives = new ArrayList<>();

        for (RecordedEvent event : recordedEvents) {
            if (event.getSelector().equals(selector)) {
                alternatives.add(event.getSelector()); // Hier könnten weitere Methoden eingebaut werden
            }
        }

        return alternatives;
    }

}
