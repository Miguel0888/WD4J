package de.bund.zrb.model;

import com.google.gson.JsonObject;
import com.microsoft.playwright.options.AriaRole;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.util.LocatorType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represent a single test action with locator info and an optional value template.
 * Keep this class declarative. Do not generate OTP or resolve parameters here.
 */
public class TestAction {

    ////////////////////////////////////////////////////////////////////////////////
    // Core metadata
    ////////////////////////////////////////////////////////////////////////////////

    private String user; // e.g. "userA", "userB"
    private ActionType type = ActionType.WHEN; // GIVEN | WHEN | THEN
    private boolean selected;

    private String action;              // "click", "fill", "navigate", "wait", ...
    private String selectedSelector;    // Chosen selector for playback
    private LocatorType locatorType;    // css, xpath, id, text, role, label, placeholder, altText

    private int timeout;

    ////////////////////////////////////////////////////////////////////////////////
    // Value template
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Template string containing literal text and optional placeholders {{...}}.
     * Examples:
     *   "{{OTP}}"
     *   "Bestellung {{Belegnummer}}"
     *   "Ich bin {{username}} und mein Code ist {{OTP}}"
     *
     * The template will be resolved at playback time in InputValueResolver.
     */
    private String value;

    ////////////////////////////////////////////////////////////////////////////////
    // Recorded context / hints
    ////////////////////////////////////////////////////////////////////////////////

    private Map<String, String> locators = new LinkedHashMap<String, String>();
    private Map<String, String> extractedValues = new LinkedHashMap<String, String>();
    private Map<String, String> extractedAttributes = new LinkedHashMap<String, String>();
    private Map<String, String> extractedTestIds = new LinkedHashMap<String, String>();
    private Map<String, String> extractedAriaRoles = new LinkedHashMap<String, String>();

    private RecordedEvent raw; // raw recorder payload (optional)

    // Higher-level semantic locator hints
    private String textContent; // for getByText
    private AriaRole role;      // for getByRole
    private String label;       // for getByLabel

    ////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////

    public TestAction(String action, LocatorType locatorType, String selectedSelector,
                      Map<String, String> extractedValues, int timeout) {
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

    public TestAction(String action) {
        this.action = action;
    }

    public TestAction() {
        // default for serializers
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Accessors
    ////////////////////////////////////////////////////////////////////////////////

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

    public LocatorType getLocatorType() {
        return locatorType;
    }

    public void setLocatorType(LocatorType locatorType) {
        this.locatorType = locatorType;
    }

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
    // Serialization
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Export a stable representation for reporting / persistence.
     * Use raw template value, not the resolved runtime string.
     */
    public JsonObject toPlaywrightJson() {
        JsonObject obj = new JsonObject();

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
    // Equality / hashCode
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestAction that = (TestAction) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (selectedSelector != null
                ? !selectedSelector.equals(that.selectedSelector)
                : that.selectedSelector != null) return false;
        return locatorType == that.locatorType;
    }

    @Override
    public int hashCode() {
        int result = (action != null) ? action.hashCode() : 0;
        result = 31 * result + (selectedSelector != null ? selectedSelector.hashCode() : 0);
        result = 31 * result + (locatorType != null ? locatorType.hashCode() : 0);
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Nested types
    ////////////////////////////////////////////////////////////////////////////////

    public enum ActionType {
        GIVEN, WHEN, THEN
    }
}
