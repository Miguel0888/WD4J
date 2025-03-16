package app.config;

import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

public class AppSecurityManager extends SecurityManager {

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>();
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>();

    static {
        // ✅ Standard-Java-Pakete (ALLE aus deiner ursprünglichen Version!)
        ALLOWED_PACKAGES.add("java.");
        ALLOWED_PACKAGES.add("javax.");
        ALLOWED_PACKAGES.add("sun.");
        ALLOWED_PACKAGES.add("com.sun.");
        ALLOWED_PACKAGES.add("org.xml.sax");
        ALLOWED_PACKAGES.add("org.w3c.dom");

        // ✅ Externe Bibliotheken (ALLE aus deiner ursprünglichen Version!)
        ALLOWED_PACKAGES.add("com.azul.tooling");
        ALLOWED_PACKAGES.add("org.slf4j");
        ALLOWED_PACKAGES.add("ch.qos.logback");
        ALLOWED_PACKAGES.add("groovy.lang");
        ALLOWED_PACKAGES.add("org.reflections");
        ALLOWED_PACKAGES.add("org.reflections.");
        ALLOWED_PACKAGES.add("javassist");
        ALLOWED_PACKAGES.add("com.google.gson");
        ALLOWED_PACKAGES.add("org.java_websocket");  // 🔥 FIX: Entferne den Punkt!

        // ✅ Eigene Klassen & Pakete
        ALLOWED_PACKAGES.add("app");
        ALLOWED_PACKAGES.add("app.");
        ALLOWED_PACKAGES.add("wd4j.api");
        ALLOWED_PACKAGES.add("wd4j.impl.");

        // ✅ Einzelne erlaubte Klassen
        ALLOWED_CLASSES.add("java.lang.Thread");
        ALLOWED_CLASSES.add("java.lang.Class");
        ALLOWED_CLASSES.add("java.security.AccessController");
        ALLOWED_CLASSES.add("java.util.ResourceBundle");
        ALLOWED_CLASSES.add("java.awt.Toolkit");
        ALLOWED_CLASSES.add("java.awt.EventQueue");
        ALLOWED_CLASSES.add("javax.swing.SwingUtilities");
        ALLOWED_CLASSES.add("sun.reflect.NativeMethodAccessorImpl");
        ALLOWED_CLASSES.add("sun.reflect.DelegatingMethodAccessorImpl");
        ALLOWED_CLASSES.add("sun.reflect.NativeConstructorAccessorImpl");
        ALLOWED_CLASSES.add("sun.reflect.DelegatingConstructorAccessorImpl");
        ALLOWED_CLASSES.add("app.Main");
        ALLOWED_CLASSES.add("app.controller.MainController");
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
            return; // Zugriff erlaubt
        }

        // 🔥 DEBUG: Logge blockierte Pakete für bessere Fehlersuche
        System.err.println("🔴 BLOCKED PACKAGE: " + pkg);

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
        if (className == null) {
            return false;
        }

        // 🔥 1️⃣ Prüfe ALLOWED_PACKAGES (fix für checkPackageAccess)
        for (String allowedPackage : ALLOWED_PACKAGES) {
            if (className.startsWith(allowedPackage)) {
                return true;
            }
        }

        // 🔥 2️⃣ Prüfe ALLOWED_CLASSES (fix für einzelne Klassen)
        for (String allowedClass : ALLOWED_CLASSES) {
            if (className.equals(allowedClass)) {
                return true;
            }
        }

        return false; // Blockieren
    }
}
