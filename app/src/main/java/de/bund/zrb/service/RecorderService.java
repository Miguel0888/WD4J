package de.bund.zrb.service;

import de.bund.zrb.RecordingEventRouter;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.util.LocatorType;
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
        List<RecordedEvent> result = new ArrayList<RecordedEvent>();
        WDRemoteValue.ArrayRemoteValue eventsArray = null;

        // Find "events"
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
            if (!(item instanceof WDRemoteValue.ObjectRemoteValue)) continue;

            WDRemoteValue.ObjectRemoteValue eventObj = (WDRemoteValue.ObjectRemoteValue) item;
            RecordedEvent event = new RecordedEvent();

            for (Map.Entry<WDRemoteValue, WDRemoteValue> pair : eventObj.getValue().entrySet()) {
                if (!(pair.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;

                String key = ((WDPrimitiveProtocolValue.StringValue) pair.getKey()).getValue();
                WDRemoteValue value = pair.getValue();

                if (value instanceof WDPrimitiveProtocolValue.StringValue) {
                    String val = ((WDPrimitiveProtocolValue.StringValue) value).getValue();
                    applySimpleField(event, key, val);
                    continue;
                }

                if (value instanceof WDRemoteValue.ObjectRemoteValue) {
                    parseNestedObject(event, key, (WDRemoteValue.ObjectRemoteValue) value);
                    continue;
                }

                // Arrays derzeit ignorieren
            }

            result.add(event);
        }

        return result;
    }

    /** Map simple string fields coming from the recorder. */
    private void applySimpleField(RecordedEvent event, String key, String val) {
        if (val == null) return;

        if ("selector".equals(key)) { event.setCss(val); return; }
        if ("action".equals(key)) { event.setAction(val); return; }
        if ("buttonText".equals(key)) { event.setButtonText(val); return; }
        if ("xpath".equals(key)) { event.setXpath(val); return; }
        if ("classes".equals(key)) { event.setClasses(val); return; }
        if ("value".equals(key)) { event.setValue(val); return; }
        if ("inputName".equals(key)) { event.setInputName(val); return; }
        if ("elementId".equals(key)) { event.setElementId(val); return; }

        // Unbekannte einfache Felder landen in extractedValues
        if (event.getExtractedValues() == null) {
            event.setExtractedValues(new LinkedHashMap<String, String>());
        }
        event.getExtractedValues().put(key, val);
    }

    /** Parse a nested object; pull out known groups and common keys (e.g., "text"). */
    private void parseNestedObject(RecordedEvent event, String parentKey, WDRemoteValue.ObjectRemoteValue obj) {
        Map<String, String> flat = objectToStringMap(obj);

        if ("extractedValues".equals(parentKey)) {
            if (event.getExtractedValues() == null) event.setExtractedValues(new LinkedHashMap<String, String>());
            event.getExtractedValues().putAll(flat);
        } else if ("aria".equals(parentKey)) {
            if (event.getAria() == null) event.setAria(new LinkedHashMap<String, String>());
            event.getAria().putAll(flat);
        } else if ("attributes".equals(parentKey)) {
            if (event.getAttributes() == null) event.setAttributes(new LinkedHashMap<String, String>());
            event.getAttributes().putAll(flat);
        } else if ("test".equals(parentKey)) {
            if (event.getTest() == null) event.setTest(new LinkedHashMap<String, String>());
            event.getTest().putAll(flat);
        } else {
            // Unbekanntes Objekt: alles nach extractedValues kippen
            if (event.getExtractedValues() == null) event.setExtractedValues(new LinkedHashMap<String, String>());
            event.getExtractedValues().putAll(flat);
        }

        // Promote common keys for convenience
        String text = flat.get("text");
        if (text != null && text.trim().length() > 0) {
            if (event.getExtractedValues() == null) event.setExtractedValues(new LinkedHashMap<String, String>());
            event.getExtractedValues().put("text", text);
            if (event.getButtonText() == null || event.getButtonText().trim().length() == 0) {
                event.setButtonText(text); // hilfreich für Clicks
            }
        }

        // Rolle/Name ggf. in aria zusammenführen, falls so geliefert
        String role = flat.get("role");
        String name = flat.get("name");
        if ((role != null && role.trim().length() > 0) || (name != null && name.trim().length() > 0)) {
            if (event.getAria() == null) event.setAria(new LinkedHashMap<String, String>());
            if (role != null) event.getAria().put("role", role);
            if (name != null) event.getAria().put("name", name);
        }

        // Manchmal liegen id/classes/xpath auch im Objekt
        if (flat.get("id") != null && (event.getElementId() == null || event.getElementId().length() == 0)) {
            event.setElementId(flat.get("id"));
        }
        if (flat.get("classes") != null && (event.getClasses() == null || event.getClasses().length() == 0)) {
            event.setClasses(flat.get("classes"));
        }
        if (flat.get("xpath") != null && (event.getXpath() == null || event.getXpath().length() == 0)) {
            event.setXpath(flat.get("xpath"));
        }
    }

    /** Flatten only string leaves from an ObjectRemoteValue. */
    private Map<String, String> objectToStringMap(WDRemoteValue.ObjectRemoteValue obj) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<WDRemoteValue, WDRemoteValue> e : obj.getValue().entrySet()) {
            if (!(e.getKey() instanceof WDPrimitiveProtocolValue.StringValue)) continue;
            String k = ((WDPrimitiveProtocolValue.StringValue) e.getKey()).getValue();
            WDRemoteValue v = e.getValue();
            if (v instanceof WDPrimitiveProtocolValue.StringValue) {
                String sv = ((WDPrimitiveProtocolValue.StringValue) v).getValue();
                if (sv != null) out.put(k, sv);
            }
        }
        return out;
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
        if (rawXpath != null && rawXpath.trim().length() > 0) {
            action.getLocators().put("xpath", rawXpath.trim());
        }
        if (sanitizedCss != null && sanitizedCss.trim().length() > 0) {
            action.getLocators().put("css", sanitizedCss.trim());
        }

        // 3a) Optional: id → always useful
        if (event.getElementId() != null && event.getElementId().trim().length() > 0) {
            String id = event.getElementId().trim();
            action.getLocators().put("id", id);
            // Backfill robust CSS/XPath if missing
            if (action.getLocators().get("css") == null) {
                action.getLocators().put("css", "[id='" + id.replace("'", "\\'") + "']");
            }
            if (action.getLocators().get("xpath") == null) {
                action.getLocators().put("xpath", "//*[@id='" + id.replace("'", "\\'") + "']");
            }
        }

        // 3b) Text ableiten: prefer buttonText, else extractedValues.text, else value for click
        String text = event.getButtonText();
        if ((text == null || text.trim().length() == 0) && event.getExtractedValues() != null) {
            String evText = event.getExtractedValues().get("text");
            if (evText != null && evText.trim().length() > 0) text = evText;
        }
        if ((text == null || text.trim().length() == 0)
                && "click".equalsIgnoreCase(event.getAction())
                && event.getValue() != null && event.getValue().trim().length() > 0) {
            text = event.getValue();
        }
        if (text != null && text.trim().length() > 0) {
            action.getLocators().put("text", text.trim());
        }

        // 3c) Role (accessibility): build "role=<role>;name=<name>" if present
        if (event.getAria() != null) {
            String role = event.getAria().get("role");
            String name = event.getAria().get("name"); // accessible name
            if (role != null && role.trim().length() > 0) {
                StringBuilder roleSel = new StringBuilder();
                roleSel.append("role=").append(role.trim());
                if (name != null && name.trim().length() > 0) {
                    roleSel.append(";name=").append(name.trim());
                }
                action.getLocators().put("role", roleSel.toString());
            }
        }

        // 3d) Label / placeholder / altText aus Attributes (falls geliefert)
        if (event.getAttributes() != null) {
            String label = event.getAttributes().get("label");
            if ((label == null || label.trim().length() == 0) && event.getInputName() != null) {
                label = event.getInputName();
            }
            if ((label == null || label.trim().length() == 0)) {
                String nameAttr = event.getAttributes().get("name");
                if (nameAttr != null) label = nameAttr;
            }
            if (label != null && label.trim().length() > 0) {
                action.getLocators().put("label", label.trim());
            }

            String placeholder = event.getAttributes().get("placeholder");
            if (placeholder != null && placeholder.trim().length() > 0) {
                action.getLocators().put("placeholder", placeholder.trim());
            }

            String alt = event.getAttributes().get("alt");
            if (alt != null && alt.trim().length() > 0) {
                action.getLocators().put("altText", alt.trim());
            }
        } else {
            // Fallback: use inputName as label when attributes map is missing
            if (event.getInputName() != null && event.getInputName().trim().length() > 0) {
                action.getLocators().put("label", event.getInputName().trim());
            }
        }

        // 4) Choose locator type and selected selector (prefer id > css > xpath > text)
        if (action.getLocators().get("id") != null) {
            action.setLocatorType(LocatorType.ID);
            action.setSelectedSelector(action.getLocators().get("id"));
        } else if (sanitizedCss != null && sanitizedCss.trim().length() > 0) {
            action.setLocatorType(LocatorType.CSS);
            action.setSelectedSelector(sanitizedCss);
        } else if (rawXpath != null && rawXpath.trim().length() > 0) {
            action.setLocatorType(LocatorType.XPATH);
            action.setSelectedSelector(rawXpath);
        } else if (action.getLocators().get("text") != null) {
            // Keep text as last fallback (BiDi innerText not supported yet)
            action.setLocatorType(LocatorType.TEXT);
            action.setSelectedSelector(action.getLocators().get("text"));
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

        // 7) Copy additional extracted info (unchanged)
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

    public void clearRecordedEvents() {
        recordedActions.clear();
    }

    // ---- Helpers (keep package-private or private) ----

    private void safePut(Map<String, String> map, String key, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.length() == 0) return;
        map.put(key, v);
    }

    /** Join non-empty parts with delimiter; skip null/empty inputs. */
    private String joinNonEmpty(String a, String b, String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (a != null && a.trim().length() > 0) sb.append(a.trim());
        if (b != null && b.trim().length() > 0) {
            if (sb.length() > 0) sb.append(delimiter);
            sb.append(b.trim());
        }
        return sb.toString();
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

}
