package app.config;

import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

public class AppSecurityManager extends SecurityManager {

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>();
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();

    static {
        // 🔹 Standard-Java-Pakete
        ALLOWED_PACKAGES.add("java.");        // Erlaubt ALLE Java-Standardpakete
        ALLOWED_PACKAGES.add("javax.");
        ALLOWED_PACKAGES.add("sun.");
        ALLOWED_PACKAGES.add("com.sun.");
        ALLOWED_PACKAGES.add("org.xml.sax."); // Erlaubt Logback-Zugriff auf XML-Konfiguration

        // 🔹 Externe Bibliotheken
        ALLOWED_PACKAGES.add("com.azul.tooling");
        ALLOWED_PACKAGES.add("org.slf4j");
        ALLOWED_PACKAGES.add("ch.qos.logback");
        ALLOWED_PACKAGES.add("org.reflections.");
        ALLOWED_PACKAGES.add("com.google.gson");
        ALLOWED_PACKAGES.add("org.java_websocket.");

        // 🔹 Eigene Klassen
        ALLOWED_PACKAGES.add("app.");  // Erlaubt alle `app.*` Pakete
        ALLOWED_PACKAGES.add("wd4j.api");
        ALLOWED_PACKAGES.add("wd4j.impl.");

        // 🔹 Falls spezifische `app.*` Pakete gesperrt bleiben sollen, stattdessen:
        // ALLOWED_PACKAGES.add("app.controller.");
        // ALLOWED_PACKAGES.add("app.ui.");
    }

    @Override
    public void checkPermission(Permission perm) {
        if (perm.getName().startsWith("setSecurityManager")) {
            throw new SecurityException("SecurityManager is blocked!");
        }

        if (perm.getName().startsWith("suppressAccessChecks") || perm.getName().startsWith("createClassLoader")) {
            checkStackTrace("Reflection or ClassLoader access is blocked!");
        }
    }

    @Override
    public void checkPackageAccess(String pkg) {
        if (isAllowed(pkg)) {
            return; // Erlaubt den Zugriff
        }
        throw new SecurityException("Access to package " + pkg + " is blocked!");
    }

    private void checkStackTrace(String errorMessage) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (isAllowed(element.getClassName())) {
                return;
            }
        }
        throw new SecurityException(errorMessage);
    }

    private boolean isAllowed(String className) {
        return ALLOWED_PACKAGES.stream().anyMatch(className::startsWith) ||
                ALLOWED_CLASSES.stream().anyMatch(className::startsWith);
    }
}
