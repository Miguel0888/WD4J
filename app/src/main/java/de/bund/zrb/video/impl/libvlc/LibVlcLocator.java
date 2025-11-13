package de.bund.zrb.video.impl.libvlc;

import java.io.File;

/** Locate local VLC and set environment for vlcj 3.x (Java 8). */
public final class LibVlcLocator {

    private LibVlcLocator() {}

    public static boolean isVlcjAvailable() {
        try {
            // Check for vlcj 3.x classes, not 4.x
            Class.forName("uk.co.caprica.vlcj.Info");
            Class.forName("uk.co.caprica.vlcj.player.MediaPlayerFactory");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean useVlcjDiscovery() {
        try {
            Class<?> nd = Class.forName("uk.co.caprica.vlcj.discovery.NativeDiscovery");
            Object discovery = nd.getConstructor().newInstance();
            Boolean ok = (Boolean) nd.getMethod("discover").invoke(discovery);
            return ok != null && ok; // unboxing vermeiden
        } catch (Throwable t) {
            return false;
        }
    }

    /** Set JNA and plugin paths for common OS locations, then re-try discovery. */
    public static boolean locateAndConfigure() {
        String[] candidates = new String[] {
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC",
                "/Applications/VLC.app/Contents/MacOS/lib",
                "/usr/lib",
                "/usr/lib64",
                "/usr/lib/x86_64-linux-gnu"
        };
        boolean configured = false;
        for (String cand : candidates) {
            File base = new File(cand);
            if (!base.exists()) continue;
            configured = applyBasePath(base);
            if (configured) break;
        }
        return configured && useVlcjDiscovery();
    }

    /** Configure explicit VLC base dir (manual override), then try discovery. */
    public static boolean configureBasePath(String basePath) {
        if (basePath == null || basePath.trim().isEmpty()) return false;
        File base = new File(basePath.trim());
        if (!base.exists()) return false;
        boolean configured = applyBasePath(base);
        return configured && useVlcjDiscovery();
    }

    private static boolean applyBasePath(File base) {
        if (base == null || !base.exists()) return false;
        File plugins = new File(base, "plugins");

        String sep = System.getProperty("path.separator");
        String existing = System.getProperty("jna.library.path");
        String combined = (existing == null || existing.isEmpty())
                ? base.getAbsolutePath()
                : existing + sep + base.getAbsolutePath();
        System.setProperty("jna.library.path", combined);
        if (plugins.exists()) {
            System.setProperty("VLC_PLUGIN_PATH", plugins.getAbsolutePath());
        }
        return true;
    }
}
