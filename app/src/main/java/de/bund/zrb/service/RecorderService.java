package de.bund.zrb.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.model.TestAction;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Ein Recorder speichert Events pro Context.
 * Er ist KEIN Singleton mehr!
 */
public class RecorderService {

    private List<RecordedEvent> recordedEvents = new ArrayList<>();
    private final Gson gson = new Gson();
    private final List<RecorderListener> listeners = new ArrayList<>();

    public RecorderService() {
        // Jetzt öffentlich und pro Instanz nutzbar
    }

    /**
     * Registriere einen Listener.
     */
    public void addListener(RecorderListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RecorderListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        List<TestAction> currentActions = getAllTestActionsForDrawer();
        for (RecorderListener listener : listeners) {
            listener.onRecorderUpdated(currentActions);
        }
    }

    public void recordAction(String message) {
        try {
            Type listType = new TypeToken<List<RecordedEvent>>() {}.getType();
            List<RecordedEvent> events = gson.fromJson(message, listType);

            if (events != null) {
                recordedEvents.addAll(events);
                System.out.println("📌 Gespeicherte Events: " + events.size());
                notifyListeners();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Fehler beim Parsen der Daten: " + e.getMessage());
        }
    }

    public TestAction convertToTestAction(RecordedEvent event) {
        TestAction action = new TestAction();

        action.setTimeout(3000);
        action.setAction(event.getAction());
        action.getLocators().put("xpath", event.getXpath());
        action.getLocators().put("css", event.getCss());
        action.setLocatorType(event.getXpath() != null ? "xpath" : "css");
        action.setSelectedSelector(event.getXpath() != null ? event.getXpath() : event.getCss());
        action.setValue(event.getValue());

        if (event.getButtonText() != null) {
            if (action.getValue() == null) {
                action.setValue(event.getButtonText());
            } else {
                action.getExtractedValues().put("buttonText", event.getButtonText());
            }
        }

        if (event.getKey() != null) {
            if (action.getValue() == null) {
                action.setValue(event.getKey());
            } else {
                action.getExtractedValues().put("key", event.getKey());
            }
        }

        action.setExtractedValues(event.getExtractedValues() != null ?
                new LinkedHashMap<>(event.getExtractedValues()) : new LinkedHashMap<>());

        action.setExtractedAttributes(event.getAttributes() != null ?
                new LinkedHashMap<>(event.getAttributes()) : new LinkedHashMap<>());

        action.setExtractedTestIds(event.getTest() != null ?
                new LinkedHashMap<>(event.getTest()) : new LinkedHashMap<>());

        action.setExtractedAriaRoles(event.getAria() != null ?
                new LinkedHashMap<>(event.getAria()) : new LinkedHashMap<>());

        if (event.getPagination() != null) {
            action.getExtractedAttributes().put("pagination", event.getPagination());
        }

        if (event.getInputName() != null) {
            action.getExtractedAttributes().put("inputName", event.getInputName());
        }

        if (event.getClasses() != null) {
            action.getExtractedAttributes().put("classes", event.getClasses());
        }

        if (event.getOldValue() != null) {
            action.getExtractedAttributes().put("oldValue", event.getOldValue());
        }

        if (event.getNewValue() != null) {
            action.getExtractedAttributes().put("newValue", event.getNewValue());
        }

        System.out.println("🔄 Konvertierte TestAction: " + action);
        return action;
    }

    public List<RecordedEvent> getRecordedEvents() {
        return new ArrayList<>(recordedEvents);
    }

    public void clearRecordedEvents() {
        recordedEvents.clear();
        notifyListeners();
    }

    public void mergeInputEvents() {
        if (recordedEvents.isEmpty()) return;

        List<RecordedEvent> mergedEvents = new ArrayList<>();
        RecordedEvent lastInputEvent = null;
        StringBuilder pressedKeys = new StringBuilder();

        for (RecordedEvent event : recordedEvents) {
            if ("input".equals(event.getAction())) {
                if (lastInputEvent != null && isSameExceptValue(lastInputEvent, event)) {
                    lastInputEvent.setValue(event.getValue());
                } else {
                    if (lastInputEvent != null) {
                        mergedEvents.add(lastInputEvent);
                    }
                    lastInputEvent = event;
                }
            } else if ("press".equals(event.getAction())) {
                pressedKeys.append(event.getKey());
            } else {
                if (lastInputEvent != null) {
                    mergedEvents.add(lastInputEvent);
                    lastInputEvent = null;
                } else if (pressedKeys.length() > 0) {
                    RecordedEvent keyEvent = new RecordedEvent();
                    keyEvent.setAction("input");
                    keyEvent.setKey(pressedKeys.toString());
                    mergedEvents.add(keyEvent);
                    pressedKeys.setLength(0);
                }
                mergedEvents.add(event);
            }
        }

        if (lastInputEvent != null) {
            mergedEvents.add(lastInputEvent);
        }

        recordedEvents = mergedEvents;
    }

    private boolean isSameExceptValue(RecordedEvent a, RecordedEvent b) {
        return Objects.equals(a.getCss(), b.getCss()) &&
                Objects.equals(a.getAction(), b.getAction()) &&
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
            if (event.getCss().equals(selector)) {
                alternatives.add(event.getCss());
            }
        }
        return alternatives;
    }

    public List<TestAction> getAllTestActionsForDrawer() {
        mergeInputEvents();
        List<TestAction> testActions = new ArrayList<>();
        for (RecordedEvent event : recordedEvents) {
            testActions.add(convertToTestAction(event));
        }
        return testActions;
    }
}
