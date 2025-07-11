package de.bund.zrb.support;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents supported actionability checks and maps them to their requirements.
 */
public enum ActionabilityCheck {

    CHECK,
    CLICK,
    DBLCLICK,
    SET_CHECKED,
    TAP,
    UNCHECK,
    HOVER,
    DRAG_TO,
    SCREENSHOT,
    FILL,
    CLEAR,
    SELECT_OPTION,
    SELECT_TEXT,
    SCROLL_INTO_VIEW_IF_NEEDED;

    private static final Map<ActionabilityCheck, EnumSet<ActionabilityRequirement>> ACTIONABILITY_MATRIX = new HashMap<>();

    static {
        ACTIONABILITY_MATRIX.put(CHECK, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(CLICK, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(DBLCLICK, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(SET_CHECKED, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(TAP, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(UNCHECK, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(HOVER, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS
        ));
        ACTIONABILITY_MATRIX.put(DRAG_TO, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE,
                ActionabilityRequirement.RECEIVES_EVENTS
        ));
        ACTIONABILITY_MATRIX.put(SCREENSHOT, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.STABLE
        ));
        ACTIONABILITY_MATRIX.put(FILL, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.ENABLED,
                ActionabilityRequirement.EDITABLE
        ));
        ACTIONABILITY_MATRIX.put(CLEAR, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.ENABLED,
                ActionabilityRequirement.EDITABLE
        ));
        ACTIONABILITY_MATRIX.put(SELECT_OPTION, EnumSet.of(
                ActionabilityRequirement.VISIBLE,
                ActionabilityRequirement.ENABLED
        ));
        ACTIONABILITY_MATRIX.put(SELECT_TEXT, EnumSet.of(
                ActionabilityRequirement.VISIBLE
        ));
        ACTIONABILITY_MATRIX.put(SCROLL_INTO_VIEW_IF_NEEDED, EnumSet.of(
                ActionabilityRequirement.STABLE
        ));
    }

    /**
     * Returns all requirements for this check.
     *
     * @return requirements for this actionability check
     */
    public EnumSet<ActionabilityRequirement> getRequirements() {
        return ACTIONABILITY_MATRIX.get(this);
    }
}
