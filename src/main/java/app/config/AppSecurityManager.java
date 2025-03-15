package app.config;

import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

public class AppSecurityManager extends SecurityManager {

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>();
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();

    static {
        // Erlaubte Packages
        ALLOWED_PACKAGES.add("javax.swing.");
        ALLOWED_PACKAGES.add("java.awt.");
        ALLOWED_PACKAGES.add("java.security.");
        ALLOWED_PACKAGES.add("java.lang.");
        ALLOWED_PACKAGES.add("java.beans.");
        ALLOWED_PACKAGES.add("java.text.");
        ALLOWED_PACKAGES.add("java.util.");
        ALLOWED_PACKAGES.add("java.io.");
        ALLOWED_PACKAGES.add("java.nio.");
        ALLOWED_PACKAGES.add("javax.imageio.");
        ALLOWED_PACKAGES.add("com.sun.imageio.");
        ALLOWED_PACKAGES.add("java.net.");
        ALLOWED_PACKAGES.add("sun.reflect.");
        ALLOWED_PACKAGES.add("sun.swing.");
        ALLOWED_PACKAGES.add("sun.font.");
        ALLOWED_PACKAGES.add("sun.nio.");
        ALLOWED_PACKAGES.add("sun.awt.");
        ALLOWED_PACKAGES.add("sun.security.");
        ALLOWED_PACKAGES.add("sun.net.");
        ALLOWED_PACKAGES.add("sun.util.");

        // Erlaubte externe Libraries
        ALLOWED_PACKAGES.add("com.azul.tooling");
        ALLOWED_PACKAGES.add("org.slf4j");
        ALLOWED_PACKAGES.add("ch.qos.logback");
        ALLOWED_PACKAGES.add("org.reflections.");
        ALLOWED_PACKAGES.add("com.google.gson");
        ALLOWED_PACKAGES.add("org.java_websocket.");

        // Erlaubte eigene Klassen
        ALLOWED_CLASSES.add("wd4j.api");
        ALLOWED_CLASSES.add("app.Main");
        ALLOWED_CLASSES.add("app.controller");
        ALLOWED_CLASSES.add("wd4j.impl.");
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
        if (!pkg.equals("java.io") && isAllowed(pkg)) {
            return;
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
