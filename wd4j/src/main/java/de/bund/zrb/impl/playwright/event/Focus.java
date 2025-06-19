package de.bund.zrb.impl.playwright.event;

import de.bund.zrb.impl.manager.WDScriptManager;
import de.bund.zrb.impl.webdriver.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.impl.webdriver.type.script.WDEvaluateResult;
import de.bund.zrb.impl.webdriver.type.script.WDRealm;
import de.bund.zrb.impl.webdriver.type.script.WDRemoteReference;


import java.util.HashMap;
import java.util.Map;

/**
 * NOT IMPLEMENTED, NOT A PLAYWRIGHT CLASS, BUT A COMMON PLAYWRIGHT EVENT CLASS
 *
 * This class is responsible for triggering focus and blur events on elements.
 */
public class Focus {
    private final WDScriptManager scriptManager;
    private final String contextId;
    private final WDRemoteReference.SharedReference elementHandle;
    private final WDRealm realm;
    private final WDBrowsingContext browsingContext;

    public Focus(String contextId, WDRemoteReference.SharedReference elementHandle, WDRealm realm, WDBrowsingContext browsingContext) {
        this.realm = realm;
        this.browsingContext = browsingContext;
        this.scriptManager = null; // ToDo: Implement this, how to get the script manager? Might be a constructor parameter?
        this.contextId = contextId;
        this.elementHandle = elementHandle;
    }

    /**
     * Triggers a 'focus' event on the element.
     */
    public void focus() {
        dispatchFocusEvent("focus", null);
    }

    /**
     * Triggers a 'blur' event on the element.
     */
    public void blur() {
        dispatchFocusEvent("blur", null);
    }

    /**
     * Dispatches a FocusEvent with optional eventInit properties.
     *
     * @param type         Either "focus" or "blur"
     * @param relatedTarget (Optional) The element that previously had focus
     */
    private void dispatchFocusEvent(String type, WDRemoteReference.SharedReference relatedTarget) {
        String script = "(el, eventType, init) => el.dispatchEvent(new FocusEvent(eventType, init))";

        // Erstellen des eventInit-Objekts
        Map<String, Object> eventInit = new HashMap<>();
        if (relatedTarget != null) {
            eventInit.put("relatedTarget", relatedTarget);
        }

        // WebDriver BiDi Evaluate-Request aufrufen
        WDEvaluateResult result = null;
        // ToDo: Implementierung
//        result = scriptManager.evaluate(
//                script,
//                new WDTarget.RealmTarget(realm), // WDTarget mit Realm-Target oder browsingContext
//                true, // awaitPromise
//                elementHandle, // Elementhandle als erstes Argument
//                type,          // Event-Typ ("focus" oder "blur")
//                eventInit      // Optionale eventInit-Parameter
//        );

        if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Failed to dispatch " + type + " event: " +
                    ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails().getText());
        }
    }
}
