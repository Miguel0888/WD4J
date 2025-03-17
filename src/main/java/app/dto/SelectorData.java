package app.dto;

import java.util.List;

public class SelectorData {
    private String elementId;  // Eindeutige ID des Elements
    private List<String> cssSelectors;
    private List<String> xpathSelectors;
    private List<String> idSelectors;

    public String getElementId() {
        return elementId;
    }

    public List<String> getCssSelectors() {
        return cssSelectors;
    }

    public List<String> getXpathSelectors() {
        return xpathSelectors;
    }

    public List<String> getIdSelectors() {
        return idSelectors;
    }
}
