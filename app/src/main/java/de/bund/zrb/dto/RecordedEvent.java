package de.bund.zrb.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecordedEvent {
    private String selector;
    private String action;
    private String value;
    private String key;
    private String inputName;
    private String buttonText;
    private String pagination;
    private String elementId;
    private String classes;
    private String xpath;
    private Map<String, String> aria;
    private Map<String, String> attributes;
    private Map<String, String> test;
    private Map<String, String> extractedValues; // 🆕 Ersetzt extractedText & extractedColumns

    // For DOM-Events only:
    private String oldValue;
    private String newValue;

    // Standard-Konstruktor (wichtig für GSON)
    public RecordedEvent() {
        this.extractedValues = new LinkedHashMap<>();
    }

    // Getter & Setter
    public String getCss() { return selector; }
    public void setCss(String selector) { this.selector = selector; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getInputName() { return inputName; }
    public void setInputName(String inputName) { this.inputName = inputName; }

    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }

    public String getPagination() { return pagination; }
    public void setPagination(String pagination) { this.pagination = pagination; }

    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }

    public String getClasses() { return classes; }
    public void setClasses(String classes) { this.classes = classes; }

    public String getXpath() { return xpath; }
    public void setXpath(String xpath) { this.xpath = xpath; }

    public Map<String, String> getAria() { return aria; }
    public void setAria(Map<String, String> aria) { this.aria = aria; }

    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }

    public Map<String, String> getTest() { return test; }
    public void setTest(Map<String, String> test) { this.test = test; }

    public Map<String, String> getExtractedValues() { return extractedValues; }
    public void setExtractedValues(Map<String, String> extractedValues) { this.extractedValues = extractedValues; }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
