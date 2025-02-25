package wd4j.impl.playwright.event;

import wd4j.api.ConsoleMessage;
import wd4j.api.JSHandle;
import wd4j.api.Page;
import wd4j.impl.manager.WDBrowsingContextManager;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.playwright.PageImpl;
import wd4j.impl.webdriver.command.request.WDBrowsingContextRequest;
import wd4j.impl.webdriver.event.WDLogEvent;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.log.WDLogEntry;
import wd4j.impl.webdriver.type.log.WDLogEntry.BaseWDLogEntry;
import wd4j.impl.webdriver.type.script.WDRemoteValue;
import wd4j.impl.webdriver.type.script.WDSource;
import wd4j.impl.webdriver.type.script.WDStackTrace;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConsoleMessageImpl implements ConsoleMessage {

    private final BaseWDLogEntry logEntry; // 🔹 Jetzt sicher gecastet zu `BaseWDLogEntry`
    private final PageImpl page;

    public ConsoleMessageImpl(WDLogEvent.EntryAdded event) {
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
            this.page = BrowserImpl.getBrowsers().stream()
                    .flatMap(browser -> browser.getPages().stream())
                    .filter(page -> page.getBrowsingContextId().equals(context.value()))
                    .findFirst()
                    .orElse(null);
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
