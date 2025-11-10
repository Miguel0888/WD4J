package de.bund.zrb.service;

import javax.swing.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lädt bei Bedarf (zur Laufzeit) die minimal notwendigen Video-Libs (JavaCV/FFmpeg/Javacpp)
 * aus einer definierten Quelle (Maven Central) und hängt sie dem System-ClassLoader an.
 *
 * Zielplattform: Windows x86_64; Java 8 (SystemClassLoader ist URLClassLoader).
 */
public final class VideoRuntimeLoader {

    private static final String MAVEN = "https://repo1.maven.org/maven2";

    // Versionen zentral – bei Upgrades hier anpassen
    private static final String JAVACV_VER = "1.5.10";
    private static final String JAVACPP_VER = "1.5.10";
    private static final String FFMPEG_VER = "6.1.1-1.5.10";

    private static final List<FileDef> WINDOWS_FILES = new ArrayList<FileDef>() {{
        add(new FileDef(path("org/bytedeco/javacv/", JAVACV_VER, "javacv-" + JAVACV_VER + ".jar")));
        add(new FileDef(path("org/bytedeco/javacpp/", JAVACPP_VER, "javacpp-" + JAVACPP_VER + ".jar")));
        add(new FileDef(path("org/bytedeco/javacpp/", JAVACPP_VER, "javacpp-" + JAVACPP_VER + "-windows-x86_64.jar")));
        add(new FileDef(path("org/bytedeco/ffmpeg/", FFMPEG_VER, "ffmpeg-" + FFMPEG_VER + "-windows-x86_64.jar")));
    }};

    private static String path(String groupPath, String version, String file) {
        return String.format("%s/%s%s/%s", MAVEN, groupPath, version, file);
    }

    private static Path cacheDir() {
        String base = System.getProperty("user.home");
        Path dir = Paths.get(base, ".wd4j", "video-libs");
        try { Files.createDirectories(dir); } catch (IOException ignore) {}
        return dir;
    }

    static class FileDef {
        final String url;
        FileDef(String url) { this.url = Objects.requireNonNull(url); }
        String fileName() { return url.substring(url.lastIndexOf('/') + 1); }
    }

    public static boolean ensureVideoLibsAvailableInteractively() {
        // Bereits verfügbar? (schnell)
        if (VideoRecordingService.quickCheckAvailable()) return true;

        int choice = JOptionPane.showConfirmDialog(
                null,
                "Die Video-Funktion benötigt zusätzliche Bibliotheken (JavaCV/FFmpeg).\n" +
                        "Sollen diese jetzt (~50-100 MB) heruntergeladen und installiert werden?",
                "Video-Libs nachladen",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) return false;

        Path dir = cacheDir();
        List<Path> downloaded = new ArrayList<>();
        for (FileDef f : WINDOWS_FILES) {
            try {
                Path target = dir.resolve(f.fileName());
                if (!Files.exists(target)) {
                    downloadToFile(f.url, target);
                }
                downloaded.add(target);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Download fehlgeschlagen: " + f.url + "\n" + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        try {
            attachToSystemClassLoader(downloaded);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Konnte Bibliotheken nicht am Classpath anmelden:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return VideoRecordingService.quickCheckAvailable();
    }

    private static void downloadToFile(String urlStr, Path target) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "WD4J-VideoLoader/1.0");
        conn.connect();
        if (conn.getResponseCode() / 100 != 2) {
            throw new IOException("HTTP " + conn.getResponseCode() + " " + conn.getResponseMessage());
        }
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(target.toFile())) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) >= 0) {
                out.write(buf, 0, r);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void attachToSystemClassLoader(List<Path> jars) throws Exception {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            throw new IllegalStateException("SystemClassLoader ist kein URLClassLoader (benötigt Java 8)");
        }
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        for (Path p : jars) {
            addURL.invoke(cl, p.toUri().toURL());
        }
    }
}

