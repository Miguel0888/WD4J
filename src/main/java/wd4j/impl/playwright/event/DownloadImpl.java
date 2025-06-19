package wd4j.impl.playwright.event;

import com.microsoft.Download;
import com.microsoft.Page;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.webdriver.event.WDBrowsingContextEvent;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.browsingContext.WDNavigationInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DownloadImpl implements Download {

    private final String contextId;
    private final String navigationId;
    private final long timestamp;
    private final String url;
    private final String filename;
    private Path filePath;
    private boolean isDeleted = false;
    private boolean isCanceled = false;

    public DownloadImpl(WDBrowsingContextEvent.DownloadWillBegin downloadWillBegin) {
        if (downloadWillBegin == null) {
            throw new IllegalArgumentException("DownloadWillBegin Event darf nicht null sein.");
        }

        WDNavigationInfo navigationInfo = downloadWillBegin.getParams();
        if (navigationInfo == null) {
            throw new IllegalArgumentException("WDNavigationInfo darf nicht null sein.");
        }

        // 🛠 Jeder Wert wird explizit zugeordnet
        this.contextId = navigationInfo.getContext().value();
        this.navigationId = navigationInfo.getNavigation().value();
        this.timestamp = navigationInfo.getTimestamp();
        this.url = navigationInfo.getUrl();
        this.filename = extractFilename(url);
    }

    private String extractFilename(String url) {
        return Optional.ofNullable(url)
                .map(u -> u.substring(u.lastIndexOf('/') + 1))
                .orElse("unknown-file");
    }

    @Override
    public void cancel() {
        if (!isCompleted()) {
            isCanceled = true;
            System.out.println("❌ Download abgebrochen: " + filename);
        }
    }

    @Override
    public InputStream createReadStream() {
        if (!isCompleted()) {
            throw new IllegalStateException("Download noch nicht abgeschlossen oder fehlgeschlagen.");
        }
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalStateException("Download-Datei nicht gefunden: " + filePath);
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Öffnen des InputStreams für: " + filePath, e);
        }
    }

    @Override
    public void delete() {
        if (!isCompleted()) {
            throw new IllegalStateException("Download noch nicht abgeschlossen.");
        }
        if (filePath != null && Files.exists(filePath)) {
            try {
                Files.delete(filePath);
                isDeleted = true;
                System.out.println("🗑 Datei gelöscht: " + filePath);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Löschen der Datei: " + filePath, e);
            }
        } else {
            System.out.println("⚠ Datei nicht gefunden oder bereits gelöscht: " + filePath);
        }
    }

    @Override
    public String failure() {
        if (isCanceled) {
            return "canceled";
        }
        if (isDeleted) {
            return "deleted";
        }
        return filePath == null ? "Download fehlgeschlagen oder nicht abgeschlossen" : "";
    }

    @Override
    public Page page() {
        return BrowserImpl.getPage(new WDBrowsingContext(contextId));
    }

    @Override
    public Path path() {
        if (!isCompleted()) {
            throw new IllegalStateException("Download noch nicht abgeschlossen.");
        }
        if (filePath == null) {
            throw new IllegalStateException("Download fehlgeschlagen oder Datei nicht verfügbar.");
        }
        return filePath;
    }

    @Override
    public void saveAs(Path targetPath) {
        if (!isCompleted()) {
            throw new IllegalStateException("Download noch nicht abgeschlossen.");
        }
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalStateException("Download-Datei existiert nicht: " + filePath);
        }
        try {
            Files.copy(filePath, targetPath);
            System.out.println("💾 Datei gespeichert als: " + targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Speichern der Datei nach: " + targetPath, e);
        }
    }

    @Override
    public String suggestedFilename() {
        return filename;
    }

    @Override
    public String url() {
        return url;
    }

    public String getContextId() {
        return contextId;
    }

    public String getNavigationId() {
        return navigationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    private boolean isCompleted() {
        return filePath != null || isCanceled || isDeleted;
    }
}
