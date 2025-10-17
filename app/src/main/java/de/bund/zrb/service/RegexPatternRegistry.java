package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.model.RegexPatterns;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manage regex presets loaded from ~/.wd4j/regex.json.
 * Provide defaults when file is missing or invalid.
 */
public final class RegexPatternRegistry {

    private static final Type TYPE = new TypeToken<RegexPatterns>(){}.getType();

    private static RegexPatternRegistry instance;

    private RegexPatterns cache;

    private RegexPatternRegistry() {
        reload();
    }

    public static synchronized RegexPatternRegistry getInstance() {
        if (instance == null) {
            instance = new RegexPatternRegistry();
        }
        return instance;
    }

    /** Reload data from disk; fall back to defaults if necessary. */
    public synchronized void reload() {
        String file = SettingsService.getRegexFileName();
        RegexPatterns loaded = SettingsService.getInstance().load(file, TYPE);
        if (loaded == null) {
            loaded = defaults();
            // Save defaults once so users can edit the file.
            SettingsService.getInstance().save(file, loaded);
        }
        this.cache = loaded;
        if (this.cache.getTitlePresets() == null) this.cache.setTitlePresets(new ArrayList<String>());
        if (this.cache.getMessagePresets() == null) this.cache.setMessagePresets(new ArrayList<String>());
    }

    /** Persist current cache to disk. */
    public synchronized void save() {
        String file = SettingsService.getRegexFileName();
        SettingsService.getInstance().save(file, cache);
    }

    /** Get immutable copy of title presets. */
    public synchronized List<String> getTitlePresets() {
        return Collections.unmodifiableList(new ArrayList<String>(cache.getTitlePresets()));
    }

    /** Get immutable copy of message presets. */
    public synchronized List<String> getMessagePresets() {
        return Collections.unmodifiableList(new ArrayList<String>(cache.getMessagePresets()));
    }

    /** Replace title presets and persist. */
    public synchronized void setTitlePresets(List<String> presets) {
        cache.setTitlePresets(sanitize(presets));
        save();
    }

    /** Replace message presets and persist. */
    public synchronized void setMessagePresets(List<String> presets) {
        cache.setMessagePresets(sanitize(presets));
        save();
    }

    private static List<String> sanitize(List<String> presets) {
        List<String> out = new ArrayList<String>();
        if (presets != null) {
            for (String s : presets) {
                if (s == null) continue;
                String t = s.trim();
                if (t.length() == 0) continue;
                if (!out.contains(t)) out.add(t);
            }
        }
        return out;
    }

    private static RegexPatterns defaults() {
        RegexPatterns p = new RegexPatterns();
        // Title defaults
        List<String> t = new ArrayList<String>();
        t.add(".*");
        t.add("(?i).*success.*");
        t.add("(?i).*warning.*");
        t.add("(?i).*error.*");
        t.add("(?i).*fatal.*");
        p.setTitlePresets(t);

        // Message defaults
        List<String> m = new ArrayList<String>();
        m.add(".*");
        m.add("(?s).*");
        m.add("(?i).*erfolg.*");
        m.add("(?i).*fehler.*");
        m.add(".*\\d+.*");
        m.add("(?i).*gespeichert.*");
        m.add("(?i).*nicht gefunden.*");
        m.add("(?i).*berechtigt.*");
        p.setMessagePresets(m);

        return p;
    }
}
