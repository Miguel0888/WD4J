package de.bund.zrb;

import de.bund.zrb.bootstrap.AppBootstrap;
import de.bund.zrb.service.RecorderEventBridge;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.bund.zrb.settings.SettingsBootstrap.ensureUserSettingsPresent;

public class Main {
    {
        ensureUserSettingsPresent();
    }

    public static final String RECORD_FLAG = "-d";
    public static final String ALT_RECORD_FLAG = "--debug";

    public static final String AGENT_LAUNCHED_FLAG = "appmap.launcher.active";

    // Feste Version, kein Chaos
    public static final String APPMAP_JAR_FILE_NAME = "appmap-agent-1.28.0.jar";
    public static final String APPMAP_DOWNLOAD_URL =
            "https://repo1.maven.org/maven2/com/appland/appmap-agent/1.28.0/appmap-agent-1.28.0.jar";

    public static final String APPMAP_CONFIG_FILE_NAME = "appmap.yml";
    public static final String APPMAP_CONFIG_CONTENT =
            "language: \"java\"\n" +
                    "name: \"WD4J\"\n" +
                    "appmap_dir: \"tmp/appmap\"\n" +
                    "packages:\n" +
                    "- path: \"com.microsoft.playwright\"\n" +
                    "- path: \"de.bund.zrb\"\n";

    public static void main(String[] args) {
        boolean recordingRequested = hasArgument(args, RECORD_FLAG) || hasArgument(args, ALT_RECORD_FLAG);

        if (recordingRequested && !isLauncherActive()) {
            // Sicherstellen, dass appmap.yml existiert
            ensureAppMapConfigExists();

            // Versuchen, vorhandenen Agent zu finden
            File existingAgent = findExistingAgentJar();

            if (existingAgent != null && existingAgent.isFile()) {
                System.out.println("Using existing AppMap agent: " + existingAgent.getAbsolutePath());
                relaunchWithAgent(args, existingAgent);
                return;
            }

            // Falls nicht vorhanden: fragen, ob wir ihn aus Maven Central ziehen sollen
            if (askToDownloadAgent()) {
                File target = getPreferredDownloadLocation();
                if (downloadAgentJar(target)) {
                    relaunchWithAgent(args, target);
                    return;
                } else {
                    System.out.println("AppMap agent download failed, continue without recording.");
                }
            } else {
                System.out.println("AppMap recording disabled by user (no agent download).");
            }

            // Kein Agent -> kein Recording
            recordingRequested = false;
        }

        // Normaler Start (zweiter Prozess mit Agent, oder ohne Debug)
        AppBootstrap.initialize();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                convertLegacyTestFile();
                new MainWindow().initUI();
            }
        });
    }

    private static boolean hasArgument(String[] args, String flag) {
        if (args == null || flag == null) {
            return false;
        }
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLauncherActive() {
        return Boolean.getBoolean(AGENT_LAUNCHED_FLAG);
    }

    private static void ensureAppMapConfigExists() {
        String baseDir = System.getProperty("user.dir");
        File configFile = new File(baseDir, APPMAP_CONFIG_FILE_NAME);

        if (configFile.exists()) {
            System.out.println("Found existing " + APPMAP_CONFIG_FILE_NAME + " at " + configFile.getAbsolutePath());
            return;
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(configFile);
            writer.write(APPMAP_CONFIG_CONTENT);
            writer.flush();
            System.out.println("Created default " + APPMAP_CONFIG_FILE_NAME + " at " + configFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Failed to create " + APPMAP_CONFIG_FILE_NAME + ": " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                    // Ignorieren
                }
            }
        }
    }

    /**
     * Check known locations for an existing agent JAR with fixed versioned name.
     */
    private static File findExistingAgentJar() {
        String baseDir = System.getProperty("user.dir");
        File inWorkDir = new File(baseDir, APPMAP_JAR_FILE_NAME);
        if (inWorkDir.exists() && inWorkDir.isFile()) {
            return inWorkDir;
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File inHome = new File(userHome + File.separator
                    + ".appmap" + File.separator
                    + "lib" + File.separator
                    + "java" + File.separator
                    + APPMAP_JAR_FILE_NAME);
            if (inHome.exists() && inHome.isFile()) {
                return inHome;
            }
        }

        return null;
    }

    /**
     * Preferred download location when agent is missing.
     * Use working dir first; if not writable, fallback to ~/.appmap/lib/java.
     */
    private static File getPreferredDownloadLocation() {
        String baseDir = System.getProperty("user.dir");
        File local = new File(baseDir, APPMAP_JAR_FILE_NAME);
        if (canWriteFile(local)) {
            return local;
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File homeDir = new File(userHome + File.separator
                    + ".appmap" + File.separator
                    + "lib" + File.separator
                    + "java");
            if (!homeDir.exists()) {
                homeDir.mkdirs();
            }
            File homeJar = new File(homeDir, APPMAP_JAR_FILE_NAME);
            if (canWriteFile(homeJar)) {
                return homeJar;
            }
        }

        return new File(System.getProperty("java.io.tmpdir"), APPMAP_JAR_FILE_NAME);
    }

    private static boolean canWriteFile(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            if (parent != null && !parent.canWrite()) {
                return false;
            }

            File probe = new File(parent, file.getName() + ".tmp_probe");
            if (probe.exists() && !probe.delete()) {
                return false;
            }
            if (probe.createNewFile()) {
                return probe.delete();
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean askToDownloadAgent() {
        String message = "Die Anwendung wurde mit dem Parameter \"debug\" gestartet.\n"
                + "Die Debug-Bibliothek (AppMap Agent "
                + APPMAP_JAR_FILE_NAME
                + ") wurde nicht gefunden.\n"
                + "Soll diese Bibliothek jetzt aus dem Maven Central Repository heruntergeladen werden?";
        int result = JOptionPane.showConfirmDialog(
                null,
                message,
                "AppMap Agent herunterladen?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    private static boolean downloadAgentJar(File target) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url = new URL(APPMAP_DOWNLOAD_URL);
            in = url.openStream();

            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            out = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            System.out.println("AppMap agent downloaded to: " + target.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.out.println("Failed to download AppMap agent: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private static void relaunchWithAgent(String[] args, File agentJar) {
        String javaBin = System.getProperty("java.home")
                + File.separator + "bin"
                + File.separator + "java";
        String classPath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<String>();
        command.add(javaBin);

        // AppMap Agent
        command.add("-javaagent:" + agentJar.getAbsolutePath());

        // Marker gegen Relaunch-Schleife
        command.add("-D" + AGENT_LAUNCHED_FLAG + "=true");

        // Auto-Recording: Agent erzeugt AppMaps automatisch
        command.add("-Dappmap.recording.auto=true");

        // explizit unsere Config-Datei verwenden
        command.add("-Dappmap.config.file=" + APPMAP_CONFIG_FILE_NAME);

        command.add("-cp");
        command.add(classPath);
        command.add(Main.class.getName());
        if (args != null) {
            command.addAll(Arrays.asList(args));
        }

        System.out.println("Relaunch with AppMap agent: " + command);

        try {
            new ProcessBuilder(command).inheritIO().start();
        } catch (IOException e) {
            System.out.println("Failed to relaunch with AppMap agent: " + e.getMessage());
            return;
        }

        System.exit(0);
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignore) {
                // Ignore
            }
        }
    }

    private static void closeQuietly(FileOutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignore) {
                // Ignore
            }
        }
    }

    @Deprecated
    private static void convertLegacyTestFile() {
        TestRegistry reg = TestRegistry.getInstance();

        if (reg.wasLoadedFromLegacy()) {
            reg.save();
            System.out.println("tests.json auf neues Format migriert.");
        }
    }
}
