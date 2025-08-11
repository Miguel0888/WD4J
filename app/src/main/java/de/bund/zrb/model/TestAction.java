package de.bund.zrb.model;

import com.google.gson.JsonObject;
import com.microsoft.playwright.options.AriaRole;
import de.bund.zrb.dto.RecordedEvent;
import de.bund.zrb.service.TotpService;
import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.util.LocatorType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represent a single test action with an explicit locator type and the selected selector.
 * Keep state minimal and expressive to support editor filtering and playback resolution.
 */
public class TestAction {

    private String user; // e.g. "userA", "userB"
    private ActionType type = ActionType.WHEN; // GIVEN | WHEN | THEN
    private boolean selected;

    private String action;
    private String selectedSelector;  // The actually used selector
    private LocatorType locatorType;  // css, xpath, id, text, role, label, placeholder, altText

    private String value;
    private Map<String, String> locators = new LinkedHashMap<String, String>();
    private Map<String, String> extractedValues = new LinkedHashMap<String, String>();
    private Map<String, String> extractedAttributes = new LinkedHashMap<String, String>();
    private Map<String, String> extractedTestIds = new LinkedHashMap<String, String>();
    private Map<String, String> extractedAriaRoles = new LinkedHashMap<String, String>();
    private int timeout;

    private RecordedEvent raw; // optional raw recording payload

    // Optional fields for higher-level locator semantics
    private String textContent; // If getByText is used
    private AriaRole role;      // If getByRole is used
    private String label;       // If getByLabel is used

    /**
     * Create a full TestAction with explicit locator type and selector.
     */
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

    /**
     * Create a TestAction with only the action name (useful for GIVEN/THEN scaffolding).
     */
    public TestAction(String action) {
        this.action = action;
    }

    /**
     * Default constructor for serializers.
     */
    public TestAction() {
        // Intentionally empty
    }

    // ----- Accessors -----

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getSelectedSelector() {
        return selectedSelector;
    }

    public void setSelectedSelector(String selectedSelector) {
        this.selectedSelector = selectedSelector;
    }

    public String getAction() {
        return action;
    }

    public LocatorType getLocatorType() {
        return locatorType;
    }

    /**
     * Set the locator type with enum. Prefer this in new code.
     */
    public void setLocatorType(LocatorType locatorType) {
        this.locatorType = locatorType;
    }

    /**
     * Backward-friendly setter to ease migration from String-based code paths.
     * Convert incoming key to enum when possible.
     */
    @Deprecated
    public void setLocatorType(String locatorTypeKey) {
        this.locatorType = LocatorType.fromKey(locatorTypeKey);
    }

    /**
     * Resolve value; generate OTP on demand when value equals "OTP".
     */
    public String getValue() {
        if (value != null && "OTP".equals(value)) {
            String userId = getUser();
            if (userId != null) {
                UserRegistry.User u = UserRegistry.getInstance().getUser(userId);
                if (u != null && u.getOtpSecret() != null) {
                    return String.format("%06d",
                            TotpService.getInstance().generateCurrentOtp(u.getOtpSecret()));
                }
            }
            // Fallback if secret is missing or user unknown
            return "######";
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    /** Keep legacy method name to avoid breaking callers. */
    public String getText() {
        return textContent;
    }

    public void setText(String textContent) {
        this.textContent = textContent;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getExtractedValues() {
        return extractedValues;
    }

    public void setExtractedValues(Map<String, String> extractedValues) {
        this.extractedValues = extractedValues != null ? extractedValues : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedAttributes() {
        return extractedAttributes;
    }

    public void setExtractedAttributes(Map<String, String> extractedAttributes) {
        // Fix bug: do not assign to extractedValues
        this.extractedAttributes = extractedAttributes != null ? extractedAttributes : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedTestIds() {
        return extractedTestIds;
    }

    public void setExtractedTestIds(Map<String, String> extractedTestIds) {
        this.extractedTestIds = extractedTestIds != null ? extractedTestIds : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getExtractedAriaRoles() {
        return extractedAriaRoles;
    }

    public void setExtractedAriaRoles(Map<String, String> extractedAriaRoles) {
        this.extractedAriaRoles = extractedAriaRoles != null ? extractedAriaRoles : new LinkedHashMap<String, String>();
    }

    public Map<String, String> getLocators() {
        return locators;
    }

    public void setLocators(Map<String, String> locators) {
        this.locators = locators != null ? locators : new LinkedHashMap<String, String>();
    }

    public RecordedEvent getRaw() {
        return raw;
    }

    public void setRaw(RecordedEvent raw) {
        this.raw = raw;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    /**
     * Convert to a minimal JSON that mirrors Playwright action intent.
     * Keep keys simple and compatible with downstream tooling.
     */
    public JsonObject toPlaywrightJson() {
        JsonObject obj = new JsonObject();
        if (user != null) {
            // Extra key; remain compatible while allowing multi-user playback
            obj.addProperty("user", user);
        }
        obj.addProperty("action", action);
        obj.addProperty("selector", selectedSelector);
        if (locatorType != null) {
            obj.addProperty("locatorType", locatorType.getKey());
        }
        if (value != null) {
            obj.addProperty("value", value);
        }
        return obj;
    }

    // ----- Equality -----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestAction that = (TestAction) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (selectedSelector != null ? !selectedSelector.equals(that.selectedSelector) : that.selectedSelector != null)
            return false;
        // Compare enum by identity for clarity
        return locatorType == that.locatorType;
    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (selectedSelector != null ? selectedSelector.hashCode() : 0);
        result = 31 * result + (locatorType != null ? locatorType.hashCode() : 0);
        return result;
    }

    // ----- Nested types -----

    public enum ActionType {
        GIVEN, WHEN, THEN
    }
}
