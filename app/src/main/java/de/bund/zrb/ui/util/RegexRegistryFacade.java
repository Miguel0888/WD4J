package de.bund.zrb.ui.util;

import de.bund.zrb.service.RegexPatternRegistry;

import java.util.ArrayList;
import java.util.List;

/** Add regex presets to the registry in an idempotent way. */
public final class RegexRegistryFacade {

    public enum Target { TITLE, MESSAGE }
    public enum Result { ADDED, DUPLICATE, EMPTY }

    private RegexRegistryFacade() { }

    /** Add a regex to given target list; return ADDED or DUPLICATE/EMPTY. */
    public static Result addRegex(Target target, String regex) {
        if (regex == null) return Result.EMPTY;
        String trimmed = regex.trim();
        if (trimmed.length() == 0) return Result.EMPTY;

        RegexPatternRegistry reg = RegexPatternRegistry.getInstance();
        if (target == Target.TITLE) {
            List<String> current = new ArrayList<String>(reg.getTitlePresets());
            if (current.contains(trimmed)) return Result.DUPLICATE;
            current.add(trimmed);
            reg.setTitlePresets(current); // registry persists internally
            return Result.ADDED;
        } else {
            List<String> current = new ArrayList<String>(reg.getMessagePresets());
            if (current.contains(trimmed)) return Result.DUPLICATE;
            current.add(trimmed);
            reg.setMessagePresets(current); // registry persists internally
            return Result.ADDED;
        }
    }
}
