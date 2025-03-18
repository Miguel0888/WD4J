package app.dto;

import wd4j.api.options.AriaRole;

import java.util.List;
import java.util.Map;

public class TestAction {
    private boolean selected;
    private String action;
    private String selectedSelector;  // Der tatsächlich verwendete Selektor
    private String locatorType; // "css", "xpath", "id", "text", "role", "label", "placeholder", "altText"
    private String value;
    private Map<String, String> extractedValues; // Für die Extraktion von Werten, die später zum Auffinden evtl. benötigt werden
    private int timeout;

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
        this.extractedValues = extractedValues;
        this.timeout = timeout;
    }

    // Falls nur ein Action-Name übergeben wird (für @Given / @Then)
    public TestAction(String action) {
        this.action = action;
    }

    public TestAction() {
        // ToDo: Correct?
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
}
