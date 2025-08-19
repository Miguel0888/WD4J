package de.bund.zrb.websocket;

public enum WDEventNames {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event methods see: https://w3c.github.io/webdriver-bidi#modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // -> marks the Playwright mapping, otherwise see 'BiDiExtraHookInstaller' Class

    // 🔹 Browsing Context Events
    CONTEXT_CREATED( // -> onFrameAttached, onPopup
            "browsingContext.contextCreated",
            "Ein neuer Seitenbereich wurde erstellt (z. B. neues Tab, Fenster oder eingebettetes Frame)."
    ),
    CONTEXT_DESTROYED( // -> onClose
            "browsingContext.contextDestroyed",
            "Ein Seitenbereich wurde geschlossen (Tab/Fenster/Frame)."
    ),
    NAVIGATION_STARTED( // -> onFrameNavigated
            "browsingContext.navigationStarted",
            "Eine Navigation hat begonnen – die Seite lädt eine neue Adresse."
    ),
    FRAGMENT_NAVIGATED(
            "browsingContext.fragmentNavigated",
            "Nur der Teil hinter dem # in der Adresse hat sich geändert (kein vollständiges Neuladen)."
    ),
    HISTORY_UPDATED(
            "browsingContext.historyUpdated",
            "Die Browser-Historie der Seite wurde aktualisiert (z. B. per pushState/replaceState)."
    ),
    DOM_CONTENT_LOADED( // -> onDOMContentLoaded
            "browsingContext.domContentLoaded",
            "Das Grundgerüst der Seite ist geladen (HTML geparst; Skripte ggf. noch nicht fertig)."
    ),
    LOAD( // -> onLoad
            "browsingContext.load",
            "Die Seite ist vollständig geladen (inklusive Bilder, Stylesheets usw.)."
    ),
    DOWNLOAD_WILL_BEGIN( // -> onDownload
            "browsingContext.downloadWillBegin",
            "Ein Dateidownload wird gestartet."
    ),
    NAVIGATION_ABORTED(
            "browsingContext.navigationAborted",
            "Eine begonnene Navigation wurde abgebrochen (z. B. durch Nutzeraktion oder Umleitung)."
    ),
    NAVIGATION_COMMITTED(
            "browsingContext.navigationCommitted",
            "Die Navigation wurde bestätigt; der Browser beginnt, die neue Seite anzuzeigen."
    ),
    NAVIGATION_FAILED( // -> onCrash
            "browsingContext.navigationFailed",
            "Das Laden der Seite ist fehlgeschlagen (z. B. Netzwerkfehler)."
    ),
    USER_PROMPT_CLOSED(
            "browsingContext.userPromptClosed",
            "Ein Browser-Dialog (Alert/Bestätigen/Eingabe) wurde geschlossen."
    ),
    USER_PROMPT_OPENED( // -> onDialog
            "browsingContext.userPromptOpened",
            "Ein Browser-Dialog (Alert/Bestätigen/Eingabe) wurde geöffnet."
    ),

    // 🔹 Network Events
    AUTH_REQUIRED(
            "network.authRequired",
            "Die Seite verlangt Anmeldedaten (HTTP-Authentifizierung)."
    ),
    BEFORE_REQUEST_SENT( // -> onRequest
            "network.beforeRequestSent",
            "Eine Netzwerkanfrage wird gesendet."
    ),
    FETCH_ERROR( // -> onRequestFailed
            "network.fetchError",
            "Eine Netzwerkanfrage ist fehlgeschlagen (z. B. Verbindungsfehler oder CORS-Problem)."
    ),
    RESPONSE_COMPLETED( // -> onRequestFinished
            "network.responseCompleted",
            "Die Antwort einer Netzwerkanfrage wurde vollständig empfangen."
    ),
    RESPONSE_STARTED( // -> onResponse
            "network.responseStarted",
            "Die Antwort auf eine Netzwerkanfrage hat begonnen (Header eingetroffen)."
    ),

    // 🔹 Script Events
    MESSAGE( // -> non PlayWright official: BrowserImpl#onMessage
            "script.message",
            "Benutzerdefinierte Nachricht aus einem Skript-Kanal (nicht Playwright-standardisiert)."
    ),
    REALM_CREATED( // -> onWorker
            "script.realmCreated",
            "Eine neue JavaScript-Ausführungsumgebung wurde erstellt (z. B. ein Worker)."
    ),
    REALM_DESTROYED(
            "script.realmDestroyed",
            "Eine JavaScript-Ausführungsumgebung wurde beendet."
    ),

    // 🔹 Log Events
    ENTRY_ADDED( // -> onConsoleMessage
            "log.entryAdded",
            "Neuer Konsolen-Eintrag auf der Seite (console.log, Warnung oder Fehler)."
    ),

    // 🔹 Input Events
    FILE_DIALOG_OPENED( // -> onFileChooser
            "input.fileDialogOpened",
            "Der Dateiauswahldialog des Browsers wurde geöffnet."
    );

    // 🔹 Weitere Module (Session, Browser Storage, Input, WebExtension) haben aktuell laut W3C-Spec keine Events

    private final String name;
    private final String description;

    WDEventNames(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }

    // 🔹 Methode zur Suche eines Events anhand des Namens (für Dispatcher)
    public static WDEventNames fromName(String name) {
        for (WDEventNames event : WDEventNames.values()) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        return null; // Falls kein passendes Event gefunden wird
    }
}
