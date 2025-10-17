package de.bund.zrb.model;

import java.util.ArrayList;
import java.util.List;

/** Hold regex presets for title and message combos. */
public final class RegexPatterns {

    private List<String> titlePresets;
    private List<String> messagePresets;

    public RegexPatterns() {
        this.titlePresets = new ArrayList<String>();
        this.messagePresets = new ArrayList<String>();
    }

    public List<String> getTitlePresets() {
        return titlePresets;
    }

    public void setTitlePresets(List<String> titlePresets) {
        this.titlePresets = (titlePresets != null) ? titlePresets : new ArrayList<String>();
    }

    public List<String> getMessagePresets() {
        return messagePresets;
    }

    public void setMessagePresets(List<String> messagePresets) {
        this.messagePresets = (messagePresets != null) ? messagePresets : new ArrayList<String>();
    }
}
