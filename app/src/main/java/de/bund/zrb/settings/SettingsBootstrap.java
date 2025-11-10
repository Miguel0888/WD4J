package de.bund.zrb.settings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SettingsBootstrap {

    // Define target directory in user home
    private static final String USER_SETTINGS_DIR_NAME = ".wd4j"; // adapt if needed
    private static final String SETTINGS_ZIP_NAME = "settings.zip";

    private SettingsBootstrap() {
        // Prevent instantiation
    }

    public static void ensureUserSettingsPresent() {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path targetDir = userHome.resolve(USER_SETTINGS_DIR_NAME);

        if (Files.exists(targetDir)) {
            return;
        }

        try {
            Path appDir = detectAppDirectory();
            Path zipPath = appDir.resolve(SETTINGS_ZIP_NAME);

            if (!Files.exists(zipPath)) {
                // Fail silently but log; do not block app start
                System.err.println("[SettingsBootstrap] settings.zip not found in: " + zipPath);
                return;
            }

            Files.createDirectories(targetDir);
            extractZip(zipPath, targetDir);
        } catch (Exception e) {
            // Never crash application because of bootstrap
            System.err.println("[SettingsBootstrap] Failed to initialize default settings: " + e.getMessage());
        }
    }

    private static Path detectAppDirectory() throws URISyntaxException {
        // Detect directory of the running JAR or classes
        URL location = SettingsBootstrap.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();

        if (location == null) {
            // Fallback to current working directory
            return Paths.get("").toAbsolutePath();
        }

        Path path = Paths.get(location.toURI());

        if (Files.isDirectory(path)) {
            // Running from IDE / classes folder
            return path.toAbsolutePath();
        }

        // Running from JAR: use parent directory of the jar
        Path parent = path.getParent();
        if (parent == null) {
            return Paths.get("").toAbsolutePath();
        }
        return parent.toAbsolutePath();
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        if (!Files.exists(zipFile)) {
            throw new FileNotFoundException("ZIP not found: " + zipFile);
        }

        byte[] buffer = new byte[8192];

        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile.toFile()))
        );

        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName()).normalize();

                // Prevent Zip Slip
                if (!outPath.startsWith(targetDir)) {
                    throw new IOException("Illegal ZIP entry path: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    OutputStream out = Files.newOutputStream(
                            outPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                    try {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    } finally {
                        out.close();
                    }
                }
            }
        } finally {
            zis.close();
        }
    }
}
