package de.bund.zrb.model;

import com.google.gson.JsonObject;
import com.microsoft.playwright.options.AriaRole;
import de.bund.zrb.dto.RecordedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestAction {
    private String user; // z. B. "userA", "userB"
    private ActionType type = ActionType.WHEN; // given | when | then
    private boolean selected;
    private String action;
    private String selectedSelector;  // Der tatsächlich verwendete Selektor
    private String locatorType; // "css", "xpath", "id", "text", "role", "label", "placeholder", "altText"
    private String value;
    private Map<String, String> locators = new LinkedHashMap<>();
    private Map<String, String> extractedValues = new LinkedHashMap<>();
    private Map<String, String> extractedAttributes = new LinkedHashMap<>();
    private Map<String, String> extractedTestIds = new LinkedHashMap<>();
    private Map<String, String> extractedAriaRoles = new LinkedHashMap<>();
    private int timeout;

    private RecordedEvent raw; // optional

    private List<String> availableCssSelectors;
    private List<String> availableXpathSelectors;
    private List<String> availableIdSelectors;
    private String textContent; // Falls getByText verwendet wird
    private AriaRole role; // Falls getByRole verwendet wird
    private String label; // Falls getByLabel verwendet wird
    private String placeholder; // Falls getByPlaceholder verwendet wird
    private String altText; // Falls getByAltText verwendet wird

    public TestAction(String action, String locatorType, String selectedSelector, Map<String, String> extractedValues, int timeout) {
        this.action = action;
        this.locatorType = locatorType;
        this.selectedSelector = selectedSelector;
        if (extractedValues != null) {
            this.extractedValues = extractedValues;
        }
        else {
            this.extractedValues = new LinkedHashMap<>();
        }
        this.timeout = timeout;
    }

    // Falls nur ein Action-Name übergeben wird (für @Given / @Then)
    public TestAction(String action) {
        this.action = action;
    }

    public TestAction() {
        // ToDo: Correct?
    }

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

    public List<String> getAvailableCssSelectors() {
        return availableCssSelectors;
    }

    public List<String> getAvailableXpathSelectors() {
        return availableXpathSelectors;
    }

    public List<String> getAvailableIdSelectors() {
        return availableIdSelectors;
    }

    public String getAction() {
        return action;
    }

    public String getLocatorType() {
        return locatorType;
    }

    public String getValue() {
        return value;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getTextContent() {
        return textContent;
    }

    public AriaRole getRole() {
        return role;
    }

    public String getLabel() {
        return label;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public String getAltText() {
        return altText;
    }

    public String getText() {
        return textContent;
    }

    public void setAction(String valueAt) {
        this.action = valueAt;
    }

    public void setLocatorType(String valueAt) {
        this.locatorType = valueAt;
    }

    public void setTimeout(int valueAt) {
        this.timeout = valueAt;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, String> getExtractedValues() {
        return extractedValues;
    }

    public void setExtractedValues(Map<String, String> extractedValues) {
        this.extractedValues = extractedValues;
    }

    public Map<String, String> getExtractedAttributes() {
        return extractedAttributes;
    }

    public void setExtractedAttributes(Map<String, String> extractedAttributes) {
        this.extractedValues = extractedAttributes;
    }

    public Map<String, String> getExtractedTestIds() {
        return extractedTestIds;
    }

    public void setExtractedTestIds(Map<String, String> extractedTestIds) {
        this.extractedTestIds = extractedTestIds;
    }

    public Map<String, String> getExtractedAriaRoles() {
        return extractedAriaRoles;
    }

    public void setExtractedAriaRoles(Map<String, String> extractedAriaRoles) {
        this.extractedAriaRoles = extractedAriaRoles;
    }

    public Map<String, String> getLocators() {
        return locators;
    }

    public void setLocators(Map<String, String> locators) {
        this.locators = locators;
    }

    public JsonObject toPlaywrightJson() {
        JsonObject obj = new JsonObject();

        if (user != null) {
            obj.addProperty("user", user); // Zusätzlicher Key, kompatibel aber von PlayWright ignorierbar
        }

        obj.addProperty("action", action);
        obj.addProperty("selector", selectedSelector);

        if (value != null) {
            obj.addProperty("value", value);
        }

        return obj;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public enum ActionType {
        GIVEN, WHEN, THEN;
    }

    public RecordedEvent getRaw() {
        return raw;
    }

    public void setRaw(RecordedEvent raw) {
        this.raw = raw;
    }
}
