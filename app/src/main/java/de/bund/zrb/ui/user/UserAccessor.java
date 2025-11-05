package de.bund.zrb.ui.user;

import java.lang.reflect.Method;

/** Read/write "user" via reflection to avoid forcing model changes now. */
public final class UserAccessor {

    private UserAccessor() { }

    public static String readUser(Object scope) {
        if (scope == null) return null;
        try {
            Method m = scope.getClass().getMethod("getUser");
            Object v = m.invoke(scope);
            return v != null ? String.valueOf(v) : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static void writeUser(Object scope, String user) {
        if (scope == null) return;
        try {
            Method m = scope.getClass().getMethod("setUser", String.class);
            m.invoke(scope, normalize(user));
        } catch (Exception ignore) {
            // swallow; UI-only step for now
        }
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }
}
