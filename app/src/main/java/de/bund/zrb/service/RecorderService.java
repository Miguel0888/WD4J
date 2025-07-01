package de.bund.zrb.service;

import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.model.TestAction;

import java.util.*;

public class RecorderService {

    // 🗂 Zentrale Registry pro Context
    private static final Map<String, RecorderService> RECORDERS = new HashMap<>();

    private final List<RecordedEvent> recordedEvents = new ArrayList<>();
    private final List<RecorderListener> listeners = new ArrayList<>();

    private RecorderService() {
        // Nur private Instanziierung, nur über getInstance erlaubt!
    }

    // ✅ Liefert Singleton für contextId
    public static synchronized RecorderService getInstance(String contextId) {
        if (contextId == null || contextId.trim().isEmpty()) {
            throw new IllegalArgumentException("ContextId must not be null or empty!");
        }
        return RECORDERS.computeIfAbsent(contextId, k -> new RecorderService());
    }

    public static synchronized void remove(String contextId) {
        RECORDERS.remove(contextId);
    }

    // === Normaler Instanzcode ===

    public void addListener(RecorderListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RecorderListener listener) {
        listeners.remove(listener);
    }

    public void recordAction(List<RecordedEvent> events) {
        if (events != null && !events.isEmpty()) {
            recordedEvents.addAll(events);
            notifyListeners();
        }
    }

    private void notifyListeners() {
        List<TestAction> actions = getAllTestActionsForDrawer();
        for (RecorderListener listener : listeners) {
            listener.onRecorderUpdated(actions);
        }
    }

    public List<TestAction> getAllTestActionsForDrawer() {
        mergeInputEvents();
        List<TestAction> actions = new ArrayList<>();
        for (RecordedEvent e : recordedEvents) {
            actions.add(convertToTestAction(e));
        }
        return actions;
    }

    public void clearRecordedEvents() {
        recordedEvents.clear();
        notifyListeners();
    }

    public List<RecordedEvent> getRecordedEvents() {
        return new ArrayList<>(recordedEvents);
    }

    // === Deine mergeInputEvents und convertToTestAction bleiben unverändert ===

    private void mergeInputEvents() {
        if (recordedEvents.isEmpty()) return;

        List<RecordedEvent> merged = new ArrayList<>();
        RecordedEvent lastInput = null;
        StringBuilder keys = new StringBuilder();

        for (RecordedEvent e : recordedEvents) {
            if ("input".equals(e.getAction())) {
                if (lastInput != null && isSameExceptValue(lastInput, e)) {
                    lastInput.setValue(e.getValue());
                } else {
                    if (lastInput != null) merged.add(lastInput);
                    lastInput = e;
                }
            } else if ("press".equals(e.getAction())) {
                keys.append(e.getKey());
            } else {
                if (lastInput != null) {
                    merged.add(lastInput);
                    lastInput = null;
                } else if (keys.length() > 0) {
                    RecordedEvent keyEvent = new RecordedEvent();
                    keyEvent.setAction("input");
                    keyEvent.setKey(keys.toString());
                    merged.add(keyEvent);
                    keys.setLength(0);
                }
                merged.add(e);
            }
        }
        if (lastInput != null) merged.add(lastInput);
        recordedEvents.clear();
        recordedEvents.addAll(merged);
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

    public TestAction convertToTestAction(RecordedEvent event) {
        // unverändert wie dein Original
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
        return action;
    }
}
