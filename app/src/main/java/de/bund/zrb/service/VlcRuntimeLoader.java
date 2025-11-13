package de.bund.zrb.service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Load vlcj + JNA jars at runtime (Java 8, Windows/Linux/macOS). */
public final class VlcRuntimeLoader {

    private static final String MAVEN = "https://repo1.maven.org/maven2";
    // Pin versions (Java 8 compatible)
    private static final String VLCJ_VER = "4.8.2";
    private static final String JNA_VER  = "5.13.0";

    private VlcRuntimeLoader() {}

    public static boolean ensureVlcjOnClasspath() {
        // Fast path: already present
        if (isVlcjAvailable()) return true;

        Path dir = cacheDir();
        try {
            // Download minimal jars (no platform-specific natives; they come from local VLC)
            Path vlcj = dir.resolve("vlcj-" + VLCJ_VER + ".jar");
            Path jna = dir.resolve("jna-" + JNA_VER + ".jar");
            Path jnap = dir.resolve("jna-platform-" + JNA_VER + ".jar");

            downloadIfMissing(url("uk/co/caprica/vlcj", VLCJ_VER, "vlcj-" + VLCJ_VER + ".jar"), vlcj);
            downloadIfMissing(url("net/java/dev/jna", JNA_VER, "jna-" + JNA_VER + ".jar"), jna);
            downloadIfMissing(url("net/java/dev/jna", JNA_VER, "jna-platform-" + JNA_VER + ".jar"), jnap);

            attachToSystemClassLoader(vlcj);
            attachToSystemClassLoader(jna);
            attachToSystemClassLoader(jnap);

            return isVlcjAvailable();
        } catch (Exception ex) {
            // Optional: log to stdout to avoid UI coupling
            System.out.println("[VlcRuntimeLoader] Failed to load vlcj jars: " + ex.getMessage());
            return false;
        }
    }

    public static boolean isVlcjAvailable() {
        try {
            Class.forName("uk.co.caprica.vlcj.factory.MediaPlayerFactory");
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static Path cacheDir() {
        Path dir = Paths.get(System.getProperty("user.home"), ".wd4j", "vlc-libs");
        try { Files.createDirectories(dir); } catch (IOException ignore) {}
        return dir;
    }

    private static String url(String groupPath, String ver, String file) {
        return MAVEN + "/" + groupPath + "/" + ver + "/" + file;
    }

    private static void downloadIfMissing(String url, Path target) throws IOException {
        if (Files.exists(target)) return;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "WD4J-VLCJLoader/1.0");
        c.connect();
        if (c.getResponseCode() / 100 != 2) {
            throw new IOException("HTTP " + c.getResponseCode() + " " + c.getResponseMessage());
        }
        try (BufferedInputStream in = new BufferedInputStream(c.getInputStream());
             FileOutputStream out = new FileOutputStream(target.toFile())) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) >= 0) out.write(buf, 0, r);
        } finally {
            c.disconnect();
        }
    }

    private static void attachToSystemClassLoader(Path jar) throws Exception {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            throw new IllegalStateException("SystemClassLoader is not URLClassLoader (needs Java 8)");
        }
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", java.net.URL.class);
        addURL.setAccessible(true);
        addURL.invoke(cl, jar.toUri().toURL());
    }
}
