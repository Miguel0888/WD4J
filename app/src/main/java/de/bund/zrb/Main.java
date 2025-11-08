package de.bund.zrb;

import com.appland.appmap.record.Recorder;
import de.bund.zrb.service.RecorderEventBridge;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static final Recorder RECORDER = Recorder.getInstance();

    public static final String RECORD_FLAG = "-d";
    public static final String ALT_RECORD_FLAG = "--debug";
    public static final String RECORD_NAME_PREFIX = "zrb-session-";

    public static final String AGENT_LAUNCHED_FLAG = "appmap.launcher.active";

    // Use fixed versioned file name to avoid surprises
    public static final String APPMAP_JAR_FILE_NAME = "appmap-agent-1.28.0.jar";

    // Download URL for this exact version (no dependency hell)
    public static final String APPMAP_DOWNLOAD_URL =
            "https://repo1.maven.org/maven2/com/appland/appmap-agent/1.28.0/appmap-agent-1.28.0.jar";

    public static void main(String[] args) {
        boolean recordingRequested = hasArgument(args, RECORD_FLAG) || hasArgument(args, ALT_RECORD_FLAG);

        if (recordingRequested && !isLauncherActive()) {
            File existingAgent = findExistingAgentJar();

            if (existingAgent != null && existingAgent.isFile()) {
                System.out.println("Using existing AppMap agent: " + existingAgent.getAbsolutePath());
                relaunchWithAgent(args, existingAgent);
                return;
            }

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

            // At this point: no agent → run without recording
            recordingRequested = false;
        }

        // Normal startup path (with or without recording)
        RecorderEventBridge.install();
        SettingsService.initAdapter();

        if (recordingRequested) {
            startRecording();
            installShutdownHook();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                convertLegacyTestFile();
                new MainWindow().initUI();
            }
        });
    }

    private static void startRecording() {
        try {
            if (RECORDER.hasActiveSession()) {
                System.out.println("AppMap recording already active, skip start.");
                return;
            }

            String recordingName = RECORD_NAME_PREFIX + System.currentTimeMillis();

            Recorder.Metadata metadata = new Recorder.Metadata("zrb", "process");
            metadata.scenarioName = recordingName;

            RECORDER.start(metadata);
            System.out.println("AppMap recording started: " + recordingName);
        } catch (Throwable t) {
            System.out.println("AppMap recording not started: " + t.getMessage());
        }
    }

    private static void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (RECORDER.hasActiveSession()) {
                        RECORDER.stop();
                        System.out.println("AppMap recording stopped on shutdown.");
                    }
                } catch (Throwable t) {
                    System.out.println("Failed to stop AppMap recording on shutdown: " + t.getMessage());
                }
            }
        }));
    }

    private static boolean hasArgument(String[] args, String flag) {
        if (args == null || flag == null) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (flag.equals(args[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLauncherActive() {
        return Boolean.getBoolean(AGENT_LAUNCHED_FLAG);
    }

    /**
     * Check known locations for an existing agent JAR with fixed versioned name.
     */
    private static File findExistingAgentJar() {
        // 1) Working directory
        String baseDir = System.getProperty("user.dir");
        File inWorkDir = new File(baseDir, APPMAP_JAR_FILE_NAME);
        if (inWorkDir.exists() && inWorkDir.isFile()) {
            return inWorkDir;
        }

        // 2) User home .appmap/lib/java
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

        // Last resort: temp file in system temp folder
        File temp = new File(System.getProperty("java.io.tmpdir"),
                APPMAP_JAR_FILE_NAME);
        return temp;
    }

    private static boolean canWriteFile(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    return false;
                }
            }
            if (!parent.canWrite()) {
                return false;
            }
            // quick probe
            File probe = new File(parent, file.getName() + ".tmp_probe");
            if (probe.exists()) {
                return probe.delete();
            }
            boolean created = probe.createNewFile();
            if (created) {
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
        command.add("-javaagent:" + agentJar.getAbsolutePath());
        command.add("-D" + AGENT_LAUNCHED_FLAG + "=true");
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
