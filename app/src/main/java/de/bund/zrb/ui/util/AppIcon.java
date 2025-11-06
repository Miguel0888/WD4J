package de.bund.zrb.ui.util;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Load and apply application icons from classpath resources. */
public final class AppIcon {

    private AppIcon() { }

    /** Install icons on the frame and, if available, on macOS Dock. */
    public static void install(JFrame frame) {
        if (frame == null) return;

        List<Image> images = new ArrayList<Image>();
        // Provide multiple sizes for best results on different DPIs
        addIfFound(images, "/icons/app-16.png");
        addIfFound(images, "/icons/app-24.png");
        addIfFound(images, "/icons/app-32.png");
        addIfFound(images, "/icons/app-48.png");
        addIfFound(images, "/icons/app-64.png");
        addIfFound(images, "/icons/app-128.png");
        addIfFound(images, "/icons/app-256.png");
        addIfFound(images, "/icons/app-512.png");

        if (!images.isEmpty()) {
            frame.setIconImages(images);
            setMacDockIconSafe(images.get(images.size() - 1)); // prefer largest
        }
        // else: silently do nothing if resources are missing
    }

    /** Load an image from the classpath and add to list if present. */
    private static void addIfFound(List<Image> out, String path) {
        try {
            InputStream in = AppIcon.class.getResourceAsStream(path);
            if (in == null) return;
            byte[] data = readAll(in);
            Image img = Toolkit.getDefaultToolkit().createImage(data);
            if (img != null) out.add(img);
        } catch (Exception ignore) { }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) bos.write(buf, 0, r);
        return bos.toByteArray();
    }

    /** Set Dock icon on macOS via reflection (Java 8 friendly, no compile-time dependency). */
    private static void setMacDockIconSafe(Image image) {
        try {
            if (!isMac()) return;
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.getMethod("getApplication").invoke(null);
            appClass.getMethod("setDockIconImage", Image.class).invoke(app, image);
        } catch (Throwable ignore) {
            // Not macOS or API not available – ignore
        }
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("mac");
    }
}
