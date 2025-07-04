package de.bund.zrb.event;

import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.Page;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.log.WDLogEntry;
import de.bund.zrb.type.log.WDLogEntry.BaseWDLogEntry;
import de.bund.zrb.type.script.WDSource;
import de.bund.zrb.type.script.WDStackTrace;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConsoleMessageImpl implements ConsoleMessage {

    private final BaseWDLogEntry logEntry; // 🔹 Jetzt sicher gecastet zu `BaseWDLogEntry`
    private final PageImpl page;
    private final BrowserImpl browser;

    public ConsoleMessageImpl(BrowserImpl browser, WDLogEvent.EntryAdded event) {
        this.browser = browser;
        WDLogEntry params = event.getParams();

        // 🔹 Sicherstellen, dass `params` eine Instanz von `BaseWDLogEntry` ist
        if (!(params instanceof BaseWDLogEntry)) {
            throw new IllegalArgumentException("Unexpected WDLogEntry type: " + params.getClass().getSimpleName());
        }

        this.logEntry = (BaseWDLogEntry) params; // ✅ Jetzt können wir `getSource()`, `getText()`, etc. aufrufen

        // 🔹 Browsing Context ID aus `WDSource` extrahieren, um die Page zu identifizieren
        WDSource source = logEntry.getSource();
        if (source != null && source.getContext() != null) {
            WDBrowsingContext context = source.getContext();
            this.page = browser.getPage(context);
        } else {
            this.page = null; // Keine gültige Page gefunden
        }
    }

    @Override
    public List<JSHandle> args() {
        // 🔹 `args` gibt es nur in `ConsoleWDLogEntry`, nicht in `JavascriptWDLogEntry`
        if (logEntry instanceof WDLogEntry.ConsoleWDLogEntry) {
            WDLogEntry.ConsoleWDLogEntry consoleEntry = (WDLogEntry.ConsoleWDLogEntry) logEntry;
            if (consoleEntry.getArgs() == null) {
                return Collections.emptyList(); // Keine Argumente vorhanden
            }
            return consoleEntry.getArgs().stream().map(arg -> (JSHandle) arg).collect(Collectors.toList());
        }
        return Collections.emptyList(); // Falls kein `ConsoleWDLogEntry`, gibt es keine `args`
    }

    @Override
    public String location() {
        WDStackTrace stackTrace = logEntry.getStackTrace();
        if (stackTrace != null && !stackTrace.getFrames().isEmpty()) {
            WDStackTrace.StackFrame frame = stackTrace.getFrames().get(0); // Erster relevanter Stack-Frame
            return frame.getUrl() + " (" + frame.getLineNumber() + ":" + frame.getColumnNumber() + ")";
        }
        WDSource source = logEntry.getSource();
        return source != null ? source.toString() : "Unknown Source";
    }

    @Override
    public Page page() {
        return page; // Jetzt wird die tatsächliche `Page` zurückgegeben!
    }

    @Override
    public String text() {
        return logEntry.getText();
    }

    @Override
    public String type() {
        return logEntry.getLevel() != null ? logEntry.getLevel().value() : "info"; // Standard-Wert: "info"
    }
}
