package wd4j.helper;

import app.model.TestAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import app.dto.RecordedEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /**
     * 🚀 Konvertiert ein {@link RecordedEvent} in eine {@link TestAction}.
     * <p>
     * Die folgende Tabelle zeigt, wie die Felder von {@link RecordedEvent} auf die Felder von {@link TestAction} abgebildet werden:
     * </p>
     *
     * <table>
     *   <tr>
     *     <th>📌 RecordedEvent-Feld</th>
     *     <th>➡ Mapping in TestAction</th>
     *     <th>📖 Beschreibung</th>
     *   </tr>
     *   <tr>
     *     <td><code>selector</code></td>
     *     <td><code>selectedSelector</code></td>
     *     <td>Direkt übernommen</td>
     *   </tr>
     *   <tr>
     *     <td><code>action</code></td>
     *     <td><code>action</code></td>
     *     <td>Direkt übernommen</td>
     *   </tr>
     *   <tr>
     *     <td><code>value</code></td>
     *     <td><code>value</code></td>
     *     <td>Direkt übernommen</td>
     *   </tr>
     *   <tr>
     *     <td><code>key</code></td>
     *     <td><code>value</code> (falls nicht null, sonst <code>extractedValues</code>)</td>
     *     <td>Mapped auf <code>value</code> oder <code>extractedValues</code></td>
     *   </tr>
     *   <tr>
     *     <td><code>buttonText</code></td>
     *     <td><code>value</code> (falls nicht null, sonst <code>extractedValues</code>)</td>
     *     <td>Mapped auf <code>value</code> oder <code>extractedValues</code></td>
     *   </tr>
     *   <tr>
     *     <td><code>inputName</code></td>
     *     <td><code>extractedAttributes</code> (Key: "inputName")</td>
     *     <td>In <code>extractedAttributes</code> gespeichert</td>
     *   </tr>
     *   <tr>
     *     <td><code>pagination</code></td>
     *     <td><code>extractedAttributes</code> (Key: "pagination")</td>
     *     <td>In <code>extractedAttributes</code> gespeichert</td>
     *   </tr>
     *   <tr>
     *     <td><code>elementId</code></td>
     *     <td><code>extractedAttributes</code> (Key: "elementId")</td>
     *     <td>In <code>extractedAttributes</code> gespeichert</td>
     *   </tr>
     *   <tr>
     *     <td><code>classes</code></td>
     *     <td><code>extractedAttributes</code> (Key: "classes")</td>
     *     <td>In <code>extractedAttributes</code> gespeichert</td>
     *   </tr>
     *   <tr>
     *     <td><code>xpath</code></td>
     *     <td><code>locatorType</code> ("xpath" falls vorhanden, sonst "css")</td>
     *     <td>XPath hat Priorität als Locator</td>
     *   </tr>
     *   <tr>
     *     <td><code>aria</code></td>
     *     <td><code>extractedAriaRoles</code></td>
     *     <td>Map mit allen Aria-Werten</td>
     *   </tr>
     *   <tr>
     *     <td><code>attributes</code></td>
     *     <td><code>extractedAttributes</code></td>
     *     <td>Map mit allen Attributen</td>
     *   </tr>
     *   <tr>
     *     <td><code>test</code></td>
     *     <td><code>extractedTestIds</code></td>
     *     <td>Map mit Test-IDs</td>
     *   </tr>
     *   <tr>
     *     <td><code>extractedValues</code></td>
     *     <td><code>extractedValues</code></td>
     *     <td>Direkt übernommen</td>
     *   </tr>
     *   <tr>
     *     <td><code>oldValue</code></td>
     *     <td><code>extractedAttributes</code> (Key: "oldValue")</td>
     *     <td>Für DOM-Events gespeichert</td>
     *   </tr>
     *   <tr>
     *     <td><code>newValue</code></td>
     *     <td><code>extractedAttributes</code> (Key: "newValue")</td>
     *     <td>Für DOM-Events gespeichert</td>
     *   </tr>
     * </table>
     *
     * <p>
     * Falls es zu Kollisionen kommt (z. B. <code>buttonText</code> und <code>key</code> gleichzeitig gesetzt),
     * wird der zusätzliche Wert in <code>extractedValues</code> gespeichert, um Datenverlust zu vermeiden. ✅
     * </p>
     *
     * @param event Das {@link RecordedEvent}, das konvertiert werden soll.
     * @return Die erzeugte {@link TestAction}-Instanz.
     */
    public TestAction convertToTestAction(RecordedEvent event) {
        TestAction action = new TestAction();

        action.setTimeout(3000); // Standard-Timeout
        action.setAction(event.getAction());
        action.setSelectedSelector(event.getSelector());
        action.setLocatorType(event.getXpath() != null ? "xpath" : "css"); // Priorität für XPath
        action.setValue(event.getValue() != null ? event.getValue() : null);

        // ✅ buttonText auf value mappen, falls value noch nicht gesetzt ist
        if (event.getButtonText() != null) {
            if (action.getValue() == null) {
                action.setValue(event.getButtonText());
            } else {
                action.getExtractedValues().put("buttonText", event.getButtonText()); // Falls bereits ein value existiert
            }
        }

        // ✅ Key-Events (Tastenanschläge) auf "value" mappen
        if (event.getKey() != null) {
            if (action.getValue() == null) {
                action.setValue(event.getKey());
            } else {
                action.getExtractedValues().put("key", event.getKey()); // Falls es bereits ein value gibt
            }
        }

        // ✅ extractedValues übernehmen
        action.setExtractedValues(event.getExtractedValues() != null ?
                new LinkedHashMap<>(event.getExtractedValues()) : new LinkedHashMap<>());

        // ✅ extractedAttributes übernehmen
        action.setExtractedAttributes(event.getAttributes() != null ?
                new LinkedHashMap<>(event.getAttributes()) : new LinkedHashMap<>());

        // ✅ extractedTestIds übernehmen
        action.setExtractedTestIds(event.getTest() != null ?
                new LinkedHashMap<>(event.getTest()) : new LinkedHashMap<>());

        // ✅ extractedAriaRoles übernehmen
        action.setExtractedAriaRoles(event.getAria() != null ?
                new LinkedHashMap<>(event.getAria()) : new LinkedHashMap<>());

        // ✅ pagination in extractedAttributes speichern
        if (event.getPagination() != null) {
            action.getExtractedAttributes().put("pagination", event.getPagination());
        }

        // ✅ inputName in extractedAttributes speichern
        if (event.getInputName() != null) {
            action.getExtractedAttributes().put("inputName", event.getInputName());
        }

        // ✅ classes als Attribut speichern
        if (event.getClasses() != null) {
            action.getExtractedAttributes().put("classes", event.getClasses());
        }

        // ✅ oldValue & newValue in extractedAttributes speichern (nur für DOM-Events)
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
