package de.bund.zrb.ui.celleditors;

import java.util.Collections;
import java.util.List;

/** Provide human-readable info for completion popups. */
public interface DescribedItem {
    /** Return a short, human-readable description (may be empty, never null). */
    String getDescription();

    /** Return parameter names in order (never null). */
    default List<String> getParamNames() { return Collections.<String>emptyList(); }

    /** Return parameter descriptions aligned to names (never null; may be smaller, UI paded). */
    default List<String> getParamDescriptions() { return Collections.<String>emptyList(); }
}
