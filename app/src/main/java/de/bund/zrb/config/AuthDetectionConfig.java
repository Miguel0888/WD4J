package de.bund.zrb.config;

import de.bund.zrb.service.SettingsService;

import java.util.*;

public final class AuthDetectionConfig {
    public final boolean enabled;
    public final String sessionCookieName;
    public final List<Integer> redirectStatusCodes;
    public final List<String> loginUrlPrefixes;

    private AuthDetectionConfig(boolean enabled, String sessionCookieName,
                                List<Integer> redirectStatusCodes, List<String> loginUrlPrefixes) {
        this.enabled = enabled;
        this.sessionCookieName = sessionCookieName;
        this.redirectStatusCodes = Collections.unmodifiableList(redirectStatusCodes);
        this.loginUrlPrefixes = Collections.unmodifiableList(loginUrlPrefixes);
    }

    @SuppressWarnings("unchecked")
    public static AuthDetectionConfig load() {
        SettingsService s = SettingsService.getInstance();

        Boolean en = s.get("auth.enabled", Boolean.class);
        String cookie = s.get("auth.sessionCookie", String.class);
        List<?> codesAny = s.get("auth.redirectStatus", List.class);
        List<?> prefixesAny = s.get("auth.loginUrlPrefixes", List.class);

        List<Integer> codes = new ArrayList<>(Arrays.asList(301, 302, 303, 307, 308));
        if (codesAny != null) {
            codes.clear();
            for (Object o : codesAny) {
                if (o instanceof Number) codes.add(((Number) o).intValue());
                else if (o instanceof String) try { codes.add(Integer.parseInt((String)o)); } catch (Exception ignored) {}
            }
        }

        List<String> prefixes = new ArrayList<>(Arrays.asList("/login", "/signin"));
        if (prefixesAny != null) {
            prefixes.clear();
            for (Object o : prefixesAny) if (o != null) prefixes.add(String.valueOf(o));
        }

        if (cookie == null || cookie.trim().isEmpty()) cookie = "JSESSIONID";
        boolean enabled = (en == null) || en;

        return new AuthDetectionConfig(enabled, cookie, codes, prefixes);
    }
}
