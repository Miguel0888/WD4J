package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import de.bund.zrb.RecordingEventRouter;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.model.LocatorType;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;
import de.bund.zrb.util.CssSelectorSanitizer;

import java.util.*;

/**
 * Ein Recorder speichert TestActions pro Page.
 * Er meldet sich selbst beim RecordingEventRouter an.
 */
public class RecorderService implements RecordingEventRouter.RecordingEventListener {

    private static final Map<Object, RecorderService> RECORDERS = new HashMap<>();

    private final List<TestAction> recordedActions = new ArrayList<>();
    private final List<RecorderListener> listeners = new ArrayList<>();

    private final Object key; // kann Page ODER BrowserContext sein

    private RecorderService(Object key) {
        this.key = key;
    }
    public static synchronized RecorderService getInstance(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null! Use Page or Context.");
        }
        return RECORDERS.computeIfAbsent(key, k -> new RecorderService(key));
    }

    public static synchronized void remove(Object key) {
        RECORDERS.remove(key);
    }

    public void addListener(RecorderListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RecorderListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (RecorderListener listener : listeners) {
            listener.onRecorderUpdated(getAllTestActionsForDrawer());
        }
    }

    @Override
    public void onRecordingEvent(WDScriptEvent.Message message) {
        WDRemoteValue.ObjectRemoteValue data = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();
        List<RecordedEvent> events = extractRecordedEvents(data);

        for (RecordedEvent event : events) {
            TestAction action = convertToTestAction(event);
            action.setRaw(event); // Save raw event
            recordedActions.add(action);
        }

        notifyListeners();
    }

    public void setRecordedActions(List<TestAction> newOrder) {
        recordedActions.clear();
        recordedActions.addAll(newOrder);
        notifyListeners();
    }

    public List<TestAction> getAllTestActionsForDrawer() {
        mergeInputEvents();
        return new ArrayList<>(recordedActions);
    }

    public void clearRecordedActions() {
        recordedActions.clear();
        notifyListeners();
    }

    private List<RecordedEvent> extractRecordedEvents(WDRemoteValue.ObjectRemoteValue data) {
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
            System.err.println("⚠️ No events found!");
            return result;
        }

        for (WDRemoteValue item : eventsArray.getValue()) {
            if (item instanceof WDRemoteValue.ObjectRemoteValue) {
                WDRemoteValue.ObjectRemoteValue eventObj = (WDRemoteValue.ObjectRemoteValue) item;
                RecordedEvent event = new RecordedEvent();

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
                            case "value": event.setValue(val); break;
                            case "inputName": event.setInputName(val); break;
                            case "elementId": event.setElementId(val); break;
                        }

                    }
                }

                result.add(event);
            }
        }

        return result;
    }

    public TestAction convertToTestAction(RecordedEvent event) {
        TestAction action = new TestAction();
        action.setTimeout(30_000);
        action.setAction(event.getAction());

        // 1) Collect raw locators from the event
        String rawXpath = event.getXpath();
        String rawCss   = event.getCss();

        // 2) Sanitize CSS early (handle JSF/PrimeFaces ids with colons)
        String sanitizedCss = rawCss != null ? CssSelectorSanitizer.sanitize(rawCss) : null;

        // 3) Put locators into the map (store sanitized CSS)
        action.getLocators().put("xpath", rawXpath);
        action.getLocators().put("css", sanitizedCss);

        // Optional: if the event exposes a distinct DOM id, store it for future use
        if (event.getElementId() != null && event.getElementId().trim().length() > 0) {
            action.getLocators().put("id", event.getElementId().trim());
        }

        // 4) Choose locator type and selected selector (prefer xpath if present, else css)
        if (rawXpath != null && rawXpath.trim().length() > 0) {
            action.setLocatorType(LocatorType.XPATH);
            action.setSelectedSelector(rawXpath);
        } else {
            action.setLocatorType(LocatorType.CSS);
            action.setSelectedSelector(sanitizedCss);
        }

        // 5) Value
        action.setValue(event.getValue());

        // 6) Map current user
        UserRegistry.User currentUser = UserContextMappingService.getInstance().getCurrentUser();
        if (currentUser != null) {
            action.setUser(currentUser.getUsername());
        } else {
            System.err.println("⚠️ Kein aktueller User im MappingService gefunden!");
        }

        // 7) Copy additional extracted info (unchanged from your version)
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
        if (event.getElementId() != null) {
            action.getExtractedAttributes().put("elementId", event.getElementId());
        }

        action.setExtractedValues(event.getExtractedValues() != null
                ? new LinkedHashMap<String, String>(event.getExtractedValues())
                : new LinkedHashMap<String, String>());
        action.setExtractedAttributes(event.getAttributes() != null
                ? new LinkedHashMap<String, String>(event.getAttributes())
                : new LinkedHashMap<String, String>());
        action.setExtractedTestIds(event.getTest() != null
                ? new LinkedHashMap<String, String>(event.getTest())
                : new LinkedHashMap<String, String>());
        action.setExtractedAriaRoles(event.getAria() != null
                ? new LinkedHashMap<String, String>(event.getAria())
                : new LinkedHashMap<String, String>());

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

    private void mergeInputEvents() {
        if (recordedActions.isEmpty()) return;

        List<TestAction> merged = new ArrayList<>();
        TestAction lastInput = null;
        StringBuilder keys = new StringBuilder();

        for (TestAction action : recordedActions) {
            if ("input".equals(action.getAction())) {
                if (lastInput != null && isSameExceptValue(lastInput, action)) {
                    lastInput.setValue(action.getValue());
                } else {
                    if (lastInput != null) merged.add(lastInput);
                    lastInput = action;
                }
            } else if ("press".equals(action.getAction())) {
                keys.append(action.getValue());
            } else {
                if (lastInput != null) {
                    merged.add(lastInput);
                    lastInput = null;
                } else if (keys.length() > 0) {
                    TestAction keyAction = new TestAction();
                    keyAction.setAction("input");
                    keyAction.setValue(keys.toString());
                    merged.add(keyAction);
                    keys.setLength(0);
                }
                merged.add(action);
            }
        }
        if (lastInput != null) merged.add(lastInput);

        recordedActions.clear();
        recordedActions.addAll(merged);
    }

    private boolean isSameExceptValue(TestAction a, TestAction b) {
        return Objects.equals(a.getSelectedSelector(), b.getSelectedSelector()) &&
                Objects.equals(a.getAction(), b.getAction()) &&
                Objects.equals(a.getLocatorType(), b.getLocatorType());
    }

    public void clearRecordedEvents() {
        recordedActions.clear();
    }
}
