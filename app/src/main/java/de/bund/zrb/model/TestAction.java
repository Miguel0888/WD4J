package de.bund.zrb.model;

import com.google.gson.JsonObject;
import com.microsoft.playwright.options.AriaRole;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.util.LocatorType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represent a single test action (a WHEN step) with locator info
 * and an optional dynamic value template.
 *
 * - Declarative only (recorded metadata, selector hints, value template).
 * - Runtime (TestPlayerService/InputValueResolver) is responsible for evaluating dynamic values.
 *
 * New in refactoring:
 *  - Each action has an id (UUID) and a parentId (the owning TestCase.id).
 *  - "type" continues to be ActionType (GIVEN|WHEN|THEN) for logging/status.
 */
public class TestAction {

    ////////////////////////////////////////////////////////////////////////////////
    // Identity / hierarchy for the refactored model tree
    ////////////////////////////////////////////////////////////////////////////////

    /** Unique id for this action node. */
    private String id;

    /** Points to the owning TestCase's id. */
    private String parentId;

    ////////////////////////////////////////////////////////////////////////////////
    // Core metadata
    ////////////////////////////////////////////////////////////////////////////////

    /** Which logical user/session executes this step. */
    private String user; // e.g. "userA", "userB"

    /** GIVEN / WHEN / THEN (we mostly use WHEN for steps in the tree). */
    private ActionType type = ActionType.WHEN;

    /** Was this action originally selected in the recorder UI (legacy flag)? */
    private boolean selected;

    /** Action verb: "click", "fill", "navigate", "wait", "press", ... */
    private String action;

    /** The selector actually chosen for playback. */
    private String selectedSelector;

    /** High-level selector strategy (css, xpath, id, text, role, ...). */
    private LocatorType locatorType;

    /** Timeout in ms for waits / Playwright calls. */
    private int timeout;

    ////////////////////////////////////////////////////////////////////////////////
    // Value template
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Raw template string for input/fill/type actions.
     *
     * Can contain text and placeholders with our Mustache-like syntax, e.g.:
     *   "{{username}}"
     *   "OTP({{username}})"
     *   "Ich bin {{username}} und mein Code ist {{otp({{username}})}}"
     *
     * We DO NOT resolve here. Runtime will call InputValueResolver.resolveDynamicText(this).
     */
    private String value;

    ////////////////////////////////////////////////////////////////////////////////
    // Recorded context / hints from the browser recorder
    ////////////////////////////////////////////////////////////////////////////////

    /** All alternative locators Playwright recorder saw (css,xpath,id,...) */
    private Map<String, String> locators = new LinkedHashMap<String, String>();

    /** Any extracted literal text values near the target element. */
    private Map<String, String> extractedValues = new LinkedHashMap<String, String>();

    /** Extracted attributes (classes, etc.). */
    private Map<String, String> extractedAttributes = new LinkedHashMap<String, String>();

    /** Extracted data-testid / similar. */
    private Map<String, String> extractedTestIds = new LinkedHashMap<String, String>();

    /** Extracted aria roles, accessible names, etc. */
    private Map<String, String> extractedAriaRoles = new LinkedHashMap<String, String>();

    /** Low-level recorder payload, mostly for debugging / tooling. */
    private RecordedEvent raw; // optional

    ////////////////////////////////////////////////////////////////////////////////
    // Higher-level semantic locator hints
    ////////////////////////////////////////////////////////////////////////////////

    /** e.g. for getByText(...) */
    private String textContent;

    /** e.g. for getByRole(role, { name: ... }) */
    private AriaRole role;

    /** e.g. for getByLabel(...) */
    private String label;

    ////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Full-ish constructor used by recorder code (not always called manually).
     */
    public TestAction(String action,
                      LocatorType locatorType,
                      String selectedSelector,
                      Map<String, String> extractedValues,
                      int timeout) {

        this.id = UUID.randomUUID().toString();
        this.action = action;
        this.locatorType = locatorType;
        this.selectedSelector = selectedSelector;

        if (extractedValues != null) {
            this.extractedValues = extractedValues;
        } else {
            this.extractedValues = new LinkedHashMap<String, String>();
        }

        this.timeout = timeout;
    }

    /**
     * Convenience for manually creating a new step via UI ("Neuer Schritt").
     */
    public TestAction(String action) {
        this.id = UUID.randomUUID().toString();
        this.action = action;
    }

    /**
     * No-arg constructor for Gson / reflection. Ensures id != null.
     */
    public TestAction() {
        this.id = UUID.randomUUID().toString();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Accessors / Getters+Setters
    ////////////////////////////////////////////////////////////////////////////////

    // ----- identity -----
    public String getId() {
        return id;
    }

    /** Used by TestRegistry.repairTreeIdsAndParents if id was missing in legacy JSON. */
    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    /** Called by TestRegistry.repairTreeIdsAndParents to wire this Action to its TestCase. */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    // ----- metadata -----
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Returns the enum-based locator type for runtime (LocatorResolver etc.).
     */
    public LocatorType getLocatorType() {
        return locatorType;
    }

    /**
     * Accept enum directly (preferred from now on).
     */
    public void setLocatorType(LocatorType locatorType) {
        this.locatorType = locatorType;
    }

    /**
     * Legacy support for old JSON where locatorType was stored as plain string ("CSS","XPATH",...).
     * Gson will happily call this setter if it sees a String.
     */
    @Deprecated
    public void setLocatorType(String locatorTypeKey) {
        this.locatorType = LocatorType.fromKey(locatorTypeKey);
    }

    public String getSelectedSelector() {
        return selectedSelector;
    }

    public void setSelectedSelector(String selectedSelector) {
        this.selectedSelector = selectedSelector;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // ----- value template -----
    /**
     * Return the raw template string (may still contain placeholders like {{OTP}}).
     * Playback must resolve placeholders before typing.
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the raw template string.
     * Store placeholders literally, do not resolve here.
     */
    public void setValue(String value) {
        this.value = value;
    }

    // ----- recorded context -----
    public Map<String, String> getLocators() {
        return locators;
    }

    public void setLocators(Map<String, String> locators) {
        this.locators = (locators != null) ? locators : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedValues() {
        return extractedValues;
    }

    public void setExtractedValues(Map<String, String> extractedValues) {
        this.extractedValues =
                (extractedValues != null) ? extractedValues : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedAttributes() {
        return extractedAttributes;
    }

    public void setExtractedAttributes(Map<String, String> extractedAttributes) {
        this.extractedAttributes =
                (extractedAttributes != null) ? extractedAttributes : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedTestIds() {
        return extractedTestIds;
    }

    public void setExtractedTestIds(Map<String, String> extractedTestIds) {
        this.extractedTestIds =
                (extractedTestIds != null) ? extractedTestIds : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedAriaRoles() {
        return extractedAriaRoles;
    }

    public void setExtractedAriaRoles(Map<String, String> extractedAriaRoles) {
        this.extractedAriaRoles =
                (extractedAriaRoles != null) ? extractedAriaRoles : new LinkedHashMap<String, String>();
    }

    public RecordedEvent getRaw() {
        return raw;
    }

    public void setRaw(RecordedEvent raw) {
        this.raw = raw;
    }

    // ----- semantic hints -----

    public String getText() {
        return textContent;
    }

    public void setText(String textContent) {
        this.textContent = textContent;
    }

    public AriaRole getRole() {
        return role;
    }

    public void setRole(AriaRole role) {
        this.role = role;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Serialization (export snapshot to report)
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Export a stable representation for reporting / persistence.
     * Use raw template value, not the resolved runtime string.
     */
    @Deprecated // not used anymore
    public JsonObject toPlaywrightJson() {
        JsonObject obj = new JsonObject();

        if (id != null) {
            obj.addProperty("id", id);
        }
        if (parentId != null) {
            obj.addProperty("parentId", parentId);
        }
        if (user != null) {
            obj.addProperty("user", user);
        }

        obj.addProperty("action", action);
        obj.addProperty("selector", selectedSelector);

        if (locatorType != null) {
            obj.addProperty("locatorType", locatorType.getKey());
        }
        if (value != null) {
            obj.addProperty("value", value); // keep template, do not resolve here
        }

        return obj;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // equals / hashCode   (keine Änderung nötig für Runtime, aber für Maps hilfreich)
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestAction)) return false;

        TestAction that = (TestAction) o;

        // Hauptkriterium: id
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }

        // Fallback: legacy comparison
        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (selectedSelector != null
                ? !selectedSelector.equals(that.selectedSelector)
                : that.selectedSelector != null) return false;
        return locatorType == that.locatorType;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        int result = (action != null) ? action.hashCode() : 0;
        result = 31 * result + (selectedSelector != null ? selectedSelector.hashCode() : 0);
        result = 31 * result + (locatorType != null ? locatorType.hashCode() : 0);
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Nested types
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Semantische Phase des Steps.
     * We keep this because TestPlayerService logs action.getType().name().
     */
    public enum ActionType {
        GIVEN, WHEN, THEN
    }
}
