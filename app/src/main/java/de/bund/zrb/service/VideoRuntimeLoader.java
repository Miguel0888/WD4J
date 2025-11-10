package de.bund.zrb.service;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.filechooser.FileNameExtensionFilter;

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
        // Basis-FFmpeg-JAR (enthält Wrapper-Klassen)
        add(new FileDef(path("org/bytedeco/ffmpeg/", FFMPEG_VER, "ffmpeg-" + FFMPEG_VER + ".jar")));
        // Natives für Windows
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

    // Zielverzeichnis für persistente Libs (settingsDir/lib)
    private static Path persistentLibDir() {
        Path settingsDir = resolveSettingsDir();
        Path libDir = settingsDir.resolve("lib");
        try { Files.createDirectories(libDir); } catch (IOException ignore) {}
        return libDir;
    }

    private static Path resolveSettingsDir() {
        // Versuche SettingsService (falls vorhanden)
        try {
            Class<?> settingsSvcClass = Class.forName("de.bund.zrb.service.SettingsService");
            Method m = settingsSvcClass.getMethod("getInstance");
            Object inst = m.invoke(null);
            // Heuristik: Methode getSettingsPath() oder getWorkingDirectory()
            for (String candidate : new String[]{"getSettingsPath", "getWorkingDirectory", "getBasePath"}) {
                try {
                    Method mm = settingsSvcClass.getMethod(candidate);
                    Object r = mm.invoke(inst);
                    if (r instanceof String && !((String) r).trim().isEmpty()) {
                        Path p = Paths.get(((String) r).trim());
                        if (Files.exists(p)) return p;
                    }
                    if (r instanceof Path) {
                        Path p = (Path) r;
                        if (Files.exists(p)) return p;
                    }
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        // Fallback Benutzerverzeichnis/.wd4j
        Path fallback = Paths.get(System.getProperty("user.home"), ".wd4j");
        try { Files.createDirectories(fallback); } catch (IOException ignore) {}
        return fallback;
    }

    // Prüft, ob alle benötigten Artefakte bereits im persistenten Verzeichnis liegen
    private static boolean allPersistedPresent() {
        Path dir = persistentLibDir();
        List<String> needed = WINDOWS_FILES.stream().map(FileDef::fileName).collect(Collectors.toList());
        for (String n : needed) {
            if (!Files.exists(dir.resolve(n))) return false;
        }
        return true;
    }

    static class FileDef {
        final String url;
        FileDef(String url) { this.url = Objects.requireNonNull(url); }
        String fileName() { return url.substring(url.lastIndexOf('/') + 1); }
    }

    public static boolean ensureVideoLibsAvailableInteractively() {
        // Bereits verfügbar (Klassen geladen) oder persistent vorhanden -> direkt versuchen zu attachen
        if (VideoRecordingService.quickCheckAvailable()) return true;
        if (allPersistedPresent()) {
            try {
                attachToSystemClassLoader(WINDOWS_FILES.stream()
                        .map(f -> persistentLibDir().resolve(f.fileName()))
                        .collect(Collectors.toList()));
                return VideoRecordingService.quickCheckAvailable();
            } catch (Exception ex) {
                // Falls Attach fehlschlägt, weiter zum Dialog
            }
        }

        // Interaktiver Dialog mit Auswahlmöglichkeiten
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.add(new JLabel("Video-Bibliotheken fehlen. Wähle eine Option:"), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(new JLabel("Benötigt werden folgende Dateien:"));
        for (FileDef f : WINDOWS_FILES) {
            center.add(linkLabel(f.url));
        }
        center.add(Box.createVerticalStrut(8));
        center.add(new JLabel("Hinweis: Download ~50-100 MB insgesamt."));
        center.add(Box.createVerticalStrut(6));
        center.add(new JLabel("Tipp: Für die manuelle Auswahl mehrere Dateien mit STRG-Klick markieren."));
        panel.add(center, BorderLayout.CENTER);

        JButton autoBtn = new JButton("Automatisch herunterladen");
        JButton manualBtn = new JButton("Manuell auswählen...");
        JButton cancelBtn = new JButton("Abbrechen");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(autoBtn);
        actions.add(manualBtn);
        actions.add(cancelBtn);
        panel.add(actions, BorderLayout.SOUTH);

        final JDialog dialog = new JDialog((Frame) null, "Video-Libs nachladen", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        final boolean[] result = new boolean[]{false};

        autoBtn.addActionListener(e -> {
            if (performAutoDownload()) {
                result[0] = true;
            }
            dialog.dispose();
        });
        manualBtn.addActionListener(e -> {
            if (performManualSelection()) {
                result[0] = true;
            }
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        dialog.setVisible(true);

        if (!result[0]) return false;
        return VideoRecordingService.quickCheckAvailable();
    }

    private static JLabel linkLabel(String url) {
        JLabel lbl = new JLabel("<html><u>" + url + "</u></html>");
        lbl.setForeground(new Color(0,102,204));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setToolTipText("Im Browser öffnen: " + url);
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ex) { /* ignore */ }
            }
        });
        return lbl;
    }

    private static boolean performAutoDownload() {
        Path dir = persistentLibDir();
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
        return attach(downloaded);
    }

    private static boolean performManualSelection() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setDialogTitle("JAR-Dateien auswählen (alle benötigten Video-JARs)");
        // JAR-Filter
        FileNameExtensionFilter jarFilter = new FileNameExtensionFilter("JAR-Dateien (*.jar)", "jar");
        chooser.setFileFilter(jarFilter);
        // Start im typischen Download-Ordner, falls vorhanden
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.exists()) chooser.setCurrentDirectory(downloads);

        int rc = chooser.showOpenDialog(null);
        if (rc != JFileChooser.APPROVE_OPTION) return false;
        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0) return false;
        // Validierung grob: Alle benötigten Namen müssen vorkommen
        List<String> need = WINDOWS_FILES.stream().map(FileDef::fileName).collect(Collectors.toList());
        List<String> chosen = Arrays.stream(files).map(f -> f.getName()).collect(Collectors.toList());

        // Falls nicht alle ausgewählt wurden, versuche dieselben im selben Ordner der ersten Auswahl zu finden
        File baseDir = files[0].getParentFile();
        for (String n : new ArrayList<String>(need)) {
            if (!chosen.contains(n) && baseDir != null) {
                File candidate = new File(baseDir, n);
                if (candidate.exists()) {
                    // virtuell zur Auswahl hinzufügen
                    files = Arrays.copyOf(files, files.length + 1);
                    files[files.length - 1] = candidate;
                    chosen.add(n);
                }
            }
        }

        List<String> missing = new ArrayList<>();
        for (String n : need) {
            if (!chosen.contains(n)) missing.add(n);
        }
        if (!missing.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Es fehlen benötigte Dateien:\n - " + String.join("\n - ", missing),
                    "Validierungsfehler",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Path dir = persistentLibDir();
        List<Path> copied = new ArrayList<>();
        for (File f : files) {
            try {
                Path target = dir.resolve(f.getName());
                if (!Files.exists(target)) {
                    Files.copy(f.toPath(), target);
                }
                copied.add(target);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Konnte Datei nicht kopieren: " + f.getName() + "\n" + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return attach(copied);
    }

    private static boolean attach(List<Path> jars) {
        try {
            attachToSystemClassLoader(jars);
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Konnte Bibliotheken nicht am Classpath anmelden:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
