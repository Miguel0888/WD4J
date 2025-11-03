// src/main/java/de/bund/zrb/ui/celleditors/DescribedItem.java
package de.bund.zrb.ui.celleditors;

/**
 * Provide a human-readable description to show in completion popups.
 * Implement this for functions and regex presets.
 */
public interface DescribedItem {
    /** Return a short, human-readable description (may be null). */
    String getDescription();
}
