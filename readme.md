# WD4J - WebDriver for Java

WD4J ist eine Java-basierte Implementierung des WebDriver BiDi-Protokolls (BiDirectional), die sich auf moderne, native Java-Lösungen konzentriert. Das Ziel von WD4J ist es, die Vorteile des neuen WebDriver BiDi-Standards zu nutzen und gleichzeitig ein leichtgewichtiges und vollständig in Java geschriebenes Interface anzubieten – ohne den Legacy-Overhead von Selenium.

## Zielsetzung

Das WebDriver BiDi-Protokoll ist eine neue standardisierte Methode für die Kommunikation zwischen Web-Clients und Browsern. WD4J verfolgt das Ziel, einen nativen, browserübergreifenden Java-WebDriver bereitzustellen, der:

1. **Ohne native Executables arbeitet** – keine externen Browser-spezifischen Implementierungen.
2. **Leichtgewichtiger als Selenium ist** – durch Verzicht auf Abwärtskompatibilität und Legacy-Code.
3. **Eine ähnliche API wie Selenium bietet**, um bestehende Entwicklererfahrungen zu nutzen.

---

## Aktueller Stand

Der aktuelle Stand beschreibt den Zustand kurz vor dem ersten produktiven Release (Stand: Oktober 2025).

### 1. Core-Engine (`wd4j`)
- **WebDriver-BiDi-Client in Java 8**  
  WD4J enthält eine eigenständige Implementierung des WebDriver-BiDi-Protokolls. Die Kommunikation erfolgt über WebSocket direkt mit dem Browser.

- **Sitzungs- und Kontext-Verwaltung**  
  Es gibt Manager-Klassen für Sessions, Browser-Kontexte (Tabs/Fenster), Eingaben (Maus/Tastatur), Netzwerk, Skriptausführung usw.  
  Jede Verantwortlichkeit ist klar getrennt (Single Responsibility):  
  z. B. `WDBrowserManager`, `WDSessionManager`, `WDInputManager`, `WDScriptManager`, …

- **Bidirektionales Messaging & Events**  
  Ereignisse (z. B. Navigation, DOM-Änderungen, User-Interaktionen) werden zentral entgegengenommen, typisiert und über einen Event-Dispatcher verteilt.  
  Dieser Dispatcher arbeitet nach dem Observer-Pattern (Listener-Registrierung pro Eventtyp bzw. pro Browsing Context).  
  → Das erlaubt es höheren Schichten, live auf Browserzustand zu reagieren (z. B. Recorder, UI).

- **Eingriffe in die Seite / Skriptausführung**  
  Es ist möglich, JavaScript gezielt in den aktiven Realm einer Page zu injizieren, z. B. um Overlays, Selektor-Tooltips oder DOM-Observer zu aktivieren.  
  Das wird für den integrierten Recorder genutzt.

- **Screenshots, Navigation, Aktivierung von Tabs**  
  Aktive Tabs können gewechselt, sichtbar gemacht, angesprochen und gescreenshottet werden.

- **Bekannter offener Punkt: Messaging-Overhead**  
  Das interne Messaging-System in WD4J wurde anfangs so aufgebaut, dass zwischen sogenannten "Frames" unterschieden wird. Dabei wurde das Playwright-Konzept "Frame" (HTML-IFrame innerhalb einer Seite) fälschlich als technischer Transport-Kanal interpretiert.  
  Ergebnis: unnötige Zwischenschichten und zusätzliche Verteilung pro "Frame".  
  Das ist kein Architektur-Blocker, aber eine Stelle für Performance-Tuning (weniger unnötige Umsortierung von Events, weniger Copying).  
  Geplant ist, diesen Layer zu vereinfachen – Events sollen direkt pro Browsing Context (Tab/Fenster) geroutet werden, nicht über ein künstliches "Frame"-Konstrukt.

### 2. Playwright-kompatible API (`playwright-java` + `playwright-adapter`)
- **API-Oberfläche (`playwright-java`)**  
  Das Modul `playwright-java` stellt die bekannten Interfaces aus Playwright bereit (`Playwright`, `Browser`, `BrowserContext`, `Page`, `Locator`, `BrowserType`, usw.).  
  Diese Interfaces sind unabhängig vom Rest und bilden den "Port" – also die Abstraktion, gegen die höherliegende Software programmiert.

- **Adapter/Implementierung (`playwright-adapter`)**  
  Das Modul `playwright-adapter` liefert die konkreten Implementierungen (`BrowserImpl`, `PageImpl`, `LocatorImpl`, `BrowserTypeImpl`, `PlaywrightImpl`, …).  
  Diese Implementierungen sprechen intern direkt mit `wd4j`.

  Konkret unterstützt sind u. a.:
  - Browser starten (`BrowserType.launch(...)`) inkl. Headless/Args.
  - Erstellen von BrowserContexts (isolation pro Nutzer / Testfall).
  - Erstellen und Wechseln von Tabs/Pages.
  - Navigation (`page.navigate("https://…")`).
  - Element-Lokalisierung über CSS/XPath-Selektoren (`page.locator("button#submit")`).
  - Interaktion (Klicks, Eingaben, Zurück/Vorwärts-Navigation).
  - Screenshots.

  Nicht vollständig bzw. noch eingeschränkt:
  - Komplexe `LaunchOptions` / `NewContextOptions` (z. B. alle Playwright-Flags, Netzwerk-Proxy-Optionen, Geolocation etc.).
  - APIRequest / Network-Intercept / Request-Weiterleitung.
  - Vollständige Event-API von Playwright (z. B. `page.on("console", ...)`) ist teilweise vorhanden, aber nicht überall 1:1 kompatibel.

  Wichtig:  
  Die Basisfunktionen zum Steuern des Browsers über eine Playwright-ähnliche API sind vorhanden und lauffähig.  
  Detailoptionen und Edge-Cases fehlen an manchen Stellen noch – aber die Architektur blockiert diese Erweiterungen nicht.

### 3. Desktop-App & Recorder (`app`)
- **GUI (Swing)**  
  Das Modul `app` stellt eine Desktop-Anwendung bereit. Diese Anwendung kann:
  - den Browser starten,
  - Benutzerkontexte verwalten (z. B. "User A", "User B" → jeweils eigener `BrowserContext`),
  - Tabs öffnen/schließen,
  - aktuelle Seite wechseln,
  - die aktuelle Seite inspizieren.

- **Aufnahme von Interaktionen (Recorder)**  
  WD4J injiziert JavaScript-Hooks in die aktuell geöffnete Seite, um Benutzeraktionen mitzuschneiden:
  - Klicks,
  - Tastatureingaben,
  - Navigation,
  - DOM-Änderungen.

  Diese Events werden über den WD4J-Event-Dispatcher an die Recorder-Logik weitergeleitet.  
  Daraus entstehen strukturierte Schritte, die in Richtung "Playwright-Script" übersetzbar sind.

- **Screenshots & Video**  
  Es gibt eine integrierte Video-/Bild-Aufnahme (Fenster-Capture), um Testschritte visuell zu dokumentieren.

- **Multi-User-Kontext**  
  Das UI erlaubt, mehrere isolierte BrowserContexts parallel zu pflegen und dynamisch umzuschalten.  
  Damit lassen sich z. B. Multi-User-Szenarien oder parallele Sessions nachstellen.

### 4. Plattform / Reifegrad
- **Zielplattform aktuell:** Windows.
- **Browser-Fokus aktuell:** Firefox über WebDriver BiDi.
  - Chromium / Edge / WebKit sind konzeptionell vorbereitet (Factory-Methoden `newChromiumInstance()`, `newEdgeInstance()`, … sind vorhanden).
  - Der Code zum tatsächlichen Starten, Parametrisieren und Verbinden dieser Browser-Typen ist angelegt, aber noch nicht in allen Fällen stabil produktiv getestet.
- **Java-Version:** Java 8 (keine neueren Sprachfeatures notwendig).

**Fazit:**  
Die grundlegende Funktionalität – Browser kontrollieren, Pages ansteuern, Elemente finden und interagieren, Events mitschneiden und über eine Desktop-App verwalten – ist implementiert.  
Offene Arbeiten betreffen hauptsächlich Feinschliff (Optionen, Edge-Cases, Performance im Messaging), nicht die Grundarchitektur. Es gibt aktuell keinen grundlegenden Designfehler, der die Weiterentwicklung behindern würde.


---

## Warum WD4J?

Selenium ist der De-facto-Standard für Web-Automatisierung, bringt jedoch Herausforderungen mit sich:
- **Legacy-Overhead**: Abwärtskompatibilität und alte Architekturen erschweren die Entwicklung.
- **Browser-spezifische Abhängigkeiten**: Externe Executables sind erforderlich, oft in anderen Sprachen implementiert.

WD4J bietet eine moderne, native Alternative:
- **Rein in Java geschrieben**: Kein zusätzlicher Setup-Aufwand für native Executables.
- **Fokus auf BiDi**: Unterstützung des neuesten WebDriver-Standards.
- **Leichtgewichtige API**: Ohne unnötige Komplexität oder Altlasten.

---

## Nutzung

### Voraussetzungen
- **Java 8+**
- Unterstützte Browser:
    - Google Chrome (Version 96+ mit BiDi-Unterstützung)
    - Microsoft Edge (Version 96+ mit BiDi-Unterstützung)
    - Mozilla Firefox (Version 91+ mit BiDi-Unterstützung)
    - Safari (noch experimentell)

### Installation
1. Klone das Repository:
   ```bash
   git clone https://github.com/Miguel0888/WD4J.git
   cd WD4J
   ```

2. Baue das Projekt:
   ```bash
   ./gradlew build
   ```

3. Füge die JAR-Datei zu deinem Projekt hinzu:
   ```bash
   ./build/libs/wd4j.jar
   ```

---

## Code-Beispiele

WD4J kann auf zwei Arten verwendet werden:

1. **High-Level über die Playwright-kompatible API**  
   → Ideal, wenn du Playwright kennst und einfach Browser steuern willst.

2. **Low-Level direkt über den WD4J-Core (`wd4j`)**  
   → Ideal, wenn du volle Kontrolle über BiDi brauchst oder WD4J als eigenständige Library (ohne Playwright-Schicht) nutzen möchtest.

---

### Variante A: Playwright-kompatible API (über `playwright-adapter`)

Dieses Beispiel zeigt, wie WD4J die Playwright-Java-Interfaces bereitstellt (`com.microsoft.playwright.*`) und wie der Adapter (`playwright-adapter`) unter der Haube den Browser startet, die WebSocket-Verbindung herstellt und die Objekte `Browser`, `BrowserContext`, `Page`, `Locator` usw. liefert.

```java
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Demonstrate how to automate a browser using the Playwright-like API implemented by WD4J.
 */
public class PlaywrightStyleExample {
    public static void main(String[] args) {
        // Create Playwright runtime (this is backed by WD4J, no Node.js required)
        Playwright playwright = Playwright.create();

        // Choose a browser type (e.g. Firefox)
        BrowserType browserType = playwright.firefox();

        // Launch the browser (LaunchOptions can set headless, args, etc.)
        Browser browser = browserType.launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false) // Run browser in headed mode for debugging
        );

        // Create an isolated BrowserContext (separate cookies/storage)
        BrowserContext context = browser.newContext(
            new Browser.NewContextOptions()
        );

        // Open a new Page (tab) inside that context
        Page page = context.newPage();

        // Navigate to a URL
        page.navigate("https://example.com");

        // Interact with the page
        page.locator("button#submit").click(); // Click the button with id="submit"

        // Take a screenshot (PNG bytes)
        byte[] screenshot = page.screenshot();

        // Clean up resources
        browser.close();      // Close the browser process
        playwright.close();   // Close the Playwright runtime
    }
}
```

Wichtig:
- Alle Imports kommen aus `com.microsoft.playwright.*` – also aus `playwright-java`.
- Die tatsächlichen Implementierungen (`BrowserTypeImpl`, `BrowserImpl`, `PageImpl`, …) liegen in `playwright-adapter` und nutzen intern den WD4J-Core.
- `BrowserType.launch(...)` startet wirklich einen echten Browser-Prozess (z. B. Firefox), liest dessen BiDi-WebSocket-Endpunkt aus, verbindet sich darüber und richtet Events, Recording-Hooks usw. ein.

---

### Variante B: Direkter Zugriff auf den WD4J-Core (`wd4j`)

Dieses Beispiel zeigt, wie man WD4J ohne den Playwright-Adapter verwendet.  
Hier arbeitest du direkt mit der BiDi-Engine (`WebDriver`, `WDBrowsingContextManager`, …).  
Das ist sinnvoll, wenn du sehr niedrigschwellig mit dem Browser reden willst oder eigene Tools um BiDi baust.

```java
import de.bund.zrb.WebDriver;
import de.bund.zrb.WDWebSocketImpl;
import de.bund.zrb.WDWebSocketManagerImpl;
import de.bund.zrb.command.response.WDBrowsingContextResult;

/**
 * Demonstrate how to talk directly to the WebDriver BiDi core (wd4j) without the Playwright adapter.
 */
public class CoreStyleExample {
    public static void main(String[] args) throws Exception {

        // 1. Start browser manually (e.g. Firefox with WebDriver BiDi enabled)
        //    and obtain its WebDriver BiDi WebSocket URL, for example:
        //    ws://127.0.0.1:9222/session
        //
        //    BrowserTypeImpl.launch(...) in the adapter does this automatically.
        //    Here we assume you already have the URL.
        String webSocketUrl = "ws://127.0.0.1:9222/session";

        // 2. Open WebSocket connection to the browser's BiDi endpoint
        WDWebSocketImpl socket = new WDWebSocketImpl(
            java.net.URI.create(webSocketUrl)
        );
        socket.connect(); // Establish the low-level WebSocket connection

        // 3. Wrap the socket with the WebSocketManager
        WDWebSocketManagerImpl webSocketManager = new WDWebSocketManagerImpl(socket);

        // 4. Create the WD4J core driver
        WebDriver driver = new WebDriver(webSocketManager);

        // 5. Create a new BiDi session for a given browser (e.g. "firefox")
        driver.connect("firefox"); // Register a new WebDriver BiDi session

        // 6. Create a new browsing context (tab / window)
        WDBrowsingContextResult.CreateResult createResult =
            driver.browsingContext().create();
        String contextId = createResult.getContext(); // Store context id

        // 7. Navigate to a page inside that context
        driver.browsingContext().navigate("https://example.com", contextId);

        // 8. Capture a screenshot from that context
        WDBrowsingContextResult.CaptureScreenshotResult screenshotResult =
            driver.browsingContext().captureScreenshot(contextId);
        String base64Png = screenshotResult.getData(); // PNG as Base64 string

        // 9. End the BiDi session and clean up
        driver.session().endSession(); // Close WebDriver BiDi session
        socket.close();                // Close WebSocket connection
    }
}
```

Wichtige Punkte zur Core-Variante:
- Du arbeitest direkt mit dem BiDi-Protokoll über `WebDriver`, `WDSessionManager`, `WDBrowsingContextManager`, usw.
- Du bekommst Kontext-IDs, kannst Tabs erzeugen, navigieren, Screenshots machen, Events abonnieren usw.
- Du bist näher am Standard. Das ist ideal, wenn du eigene Automations- oder Analyse-Tools baust und Playwright-Semantik nicht brauchst.
- Der Playwright-Adapter (`playwright-adapter`) macht intern ebenfalls genau das, plus Komfortschicht und API-Modellierung im Playwright-Stil.


---

## Entwicklerinformationen
### Automatische Proxy-Konfiguration per WPAD/PAC-Datei
Wenn unter Windows ein Setupskript mit URL für das Netzwerk hinterlegt ist, muss das Projekt wie folgt über die PowerShell gebaut werden:

```
./gradlew assemble --init-script proxy-init.gradle
```

Dadurch werden die nötigen Dependencies in den Gradle-Cache geladen. Anschließen kann das Projekt auch einfach wie gewohnt in IntelliJ gestartet und dedebugged werden. Dazu einfach die Play-Taste neben der main anklicken. Bei Änderungen der Dependencies muss der o.g. Befehl im Terminal allerdings immer wieder erneut ausgeführt werden. 

**Tipp:** Wer sich das wiederholte Ausführen im Terminal sparen möchte, kann die Datei `proxy-init.gradle` auch global unter `%USERPROFILE%\.gradle\init.gradle` ablegen. Damit wird die automatische Proxy-Konfiguration dauerhaft für alle Gradle-Projekte übernommen – unabhängig davon, wie sie gestartet werden. (Die Datei muss zwingend in init.gradle umbenannt werden, ansonsten funktioniert es nicht.)

### Proxy-Konfiguration für GIT-Versionsverwaltung
Da GIT analog zu Gradle die Proxy Konfiguration aus Windows nicht automatisch übernimmt, muss einmalig für dem ersten Push folgendes Script ausgeführt werden:
```
.\configure-git-proxy.ps1
```
Hierbei wird ein minimalistischer JavaScript Parser verwendet, um an die notwendigen Informationen aus der PAC-Datei zu gelangen.

### Proxy-Konfiguration für IDEA (funktioniert aber nicht für Gradle und GIT)
Im obigen Fall muss auch IDEA angepasst werden, damit die GIT-Versionsverwaltung wie gewohnt funktioniert. Das geht am einfachsten wie folgt:
Settings → Appearance & Behavior → System Settings → HTTP Proxy:
- steht auf Auto-detect proxy settings
- oder manuell mit den richtigen Daten (falls "auto" nicht ausreicht die Box für den Link anhaken)
Die URL für die halbautomatische Einstellung bekommt man über ein Klick auf den blauen Link zu den Systemeinstellugen, die URL dort einfach kopieren.

## Contribution

Beiträge sind willkommen! Wenn du helfen möchtest:
- Schaue in den [Issue Tracker](https://github.com/Miguel0888/WD4J/issues) oder die ToDos, um evtl. offene Aufgaben zu finden.
- Reiche Pull Requests ein, um Funktionen oder Fehlerbehebungen vorzuschlagen. (Via Fork)


------

## Siehe auch

- [WebDriver BiDi Spezifikation (W3C)](https://w3c.github.io/webdriver-bidi/): Offizielle Dokumentation und Beschreibung des WebDriver BiDi-Protokolls.
- [Session Management im WebDriver BiDi-Protokoll](https://w3c.github.io/webdriver-bidi/#session): Details zur Sitzungserstellung und -verwaltung.
- [Module des WebDriver BiDi-Protokolls](https://w3c.github.io/webdriver-bidi/#modules): Überblick über die einzelnen Module und deren Funktionen (z. B. `session`, `WDBrowsingContextRequest`).
- [WebSocket-Kommunikation im BiDi-Protokoll](https://w3c.github.io/webdriver-bidi/#transport): Beschreibung der bidirektionalen Kommunikation zwischen Client und Browser.
- [Capabilities für WebSocket-URLs](https://developer.mozilla.org/en-US/docs/Web/WebDriver/Capabilities/webSocketUrl): Informationen zum Erlangen der WebSocket-URL mit WebDriver-Capabilities.

---

## Lizenz

Dieses Projekt steht unter der **MIT-Lizenz**. Bitte beachten Sie, dass die verwendete PlayWright API ggf. unter einer anderen Lizenz steht. Dies bezieht sich lediglich auf die Interfaces (api package), die Implementierungs-Klassen (impl package) wurden hingegen vollkommen neu geschrieben!

# ToDos:
## Aufzeichnung aller für automatisierter Tests relevanten Events ermöglichen (ist die Liste vollständig?)

| Event-Kategorie        | WebDriver BiDi Event                          | Bedeutung                          |
|------------------------|----------------------------------------------|------------------------------------|
| Navigation            | `WDBrowsingContextRequest.navigate`                    | Navigiert zu einer URL            |
| Seitenwechsel        | `WDBrowsingContextRequest.domContentLoaded`            | DOM der Seite wurde geladen       |
| Seitenwechsel        | `WDBrowsingContextRequest.load`                         | Seite vollständig geladen         |
| Mausklicks           | `input.userInteraction (type=pointerDown)`     | Benutzer hat geklickt            |
| Mausklicks           | `input.userInteraction (type=pointerUp)`       | Klick losgelassen                 |
| Mausbewegung         | `input.userInteraction (type=pointerMove)`     | Mausbewegung über Element         |
| Tastatureingaben     | `input.userInteraction (type=keyDown)`         | Taste wurde gedrückt              |
| Tastatureingaben     | `input.userInteraction (type=keyUp)`           | Taste wurde losgelassen           |
| Formularinteraktion  | `input.userInteraction (type=input)`           | Eingabe in ein Formularfeld       |
| Formularinteraktion  | `input.userInteraction (type=change)`          | Wert einer Eingabe hat sich geändert |
| Scrollen            | `input.userInteraction (type=wheel)`           | Scroll-Event                      |
| Kontextmenü         | `input.userInteraction (type=contextMenu)`     | Rechtsklick erkannt               |
| Element-Fokus       | `input.userInteraction (type=focus)`           | Element wurde fokussiert          |
| Element-Blurring    | `input.userInteraction (type=blur)`            | Fokus wurde entfernt              |

## Mapping der WebDriver BiDi Events auf die PlayWright-API
| WebDriver BiDi Event                          | Playwright-Eventhandler                         | Playwright-Alternative                        |
|----------------------------------------------|-----------------------------------------------|----------------------------------------------|
| `WDBrowsingContextRequest.navigate`                    | `page.on("framenavigated", event -> {})`      | `page.on("domcontentloaded", event -> {})`  |
| `WDBrowsingContextRequest.domContentLoaded`            | `page.on("domcontentloaded", event -> {})`    | Automatisch in `goto()` enthalten           |
| `WDBrowsingContextRequest.load`                         | `page.on("load", event -> {})`                | `page.waitForLoadState("load")`             |
| `input.userInteraction (type=pointerDown)`     | `page.on("mousedown", event -> {})`           | `page.on("click", event -> {})`             |
| `input.userInteraction (type=pointerUp)`       | `page.on("mouseup", event -> {})`             | `page.on("click", event -> {})`             |
| `input.userInteraction (type=pointerMove)`     | ❌ (Kein direkter Eventhandler)               | `page.mouse().move(x, y)` nur für Simulation |
| `input.userInteraction (type=keyDown)`         | `page.keyboard().on("keydown", event -> {})`  | `page.on("keydown", event -> {})`           |
| `input.userInteraction (type=keyUp)`           | `page.keyboard().on("keyup", event -> {})`    | `page.on("keyup", event -> {})`             |
| `input.userInteraction (type=input)`           | `page.on("input", event -> {})`               | `page.fill(selector, value)`                |
| `input.userInteraction (type=change)`          | `page.on("change", event -> {})`              | `page.on("input", event -> {})`             |
| `input.userInteraction (type=wheel)`           | ❌ (Kein direkter Eventhandler)               | `page.mouse().wheel(dx, dy)` für Simulation |
| `input.userInteraction (type=contextMenu)`     | `page.on("contextmenu", event -> {})`         | `page.mouse().click(x, y, button="right")`  |
| `input.userInteraction (type=focus)`           | `page.on("focus", event -> {})`               | `page.WDLocator(selector).focus()`            |
| `input.userInteraction (type=blur)`            | `page.on("blur", event -> {})`                | `page.WDLocator(selector).blur()`             |


## Implementierung des Event-Recordings

Der Recorder der App-Schicht verwendet seine eigene von PlayWright abweichende Logik, sendet die Events aber per WebDriver BiDi Messages an die Anwendung zurück.

---

## Videoaufzeichnung

WD4J bietet flexible Videoaufzeichnungsmöglichkeiten mit automatischer Backend-Auswahl.

### Architektur

Die Videoaufzeichnung basiert auf dem Strategy-Pattern mit zwei Backends:

1. **LibVLC (bevorzugt)** - Nutzt eine lokal installierte VLC-Installation
2. **FFmpeg/JavaCV (Fallback)** - Lädt benötigte Bibliotheken bei Bedarf nach

### Voraussetzungen

**Wichtig:** 64-bit Matching zwischen Java, VLC und Betriebssystem erforderlich!

#### Für LibVLC (empfohlen)
- 64-bit VLC-Installation (keine Downloads nötig)
- Windows: VLC unter `C:\Program Files\VideoLAN\VLC`
- macOS: VLC.app unter `/Applications/VLC.app`
- Linux: libvlc.so in Standard-Bibliothekspfaden (`/usr/lib`, `/usr/local/lib`)

#### Für FFmpeg/JavaCV (Fallback)
- Automatischer Download der Bibliotheken beim ersten Start (~50-100 MB)
- Oder manuelle Auswahl der JAR-Dateien

### Verwendung

```java
// 1. Recorder erstellen (automatische Backend-Auswahl)
MediaRecorder recorder = MediaRuntimeBootstrap.createRecorder();

// 2. Aufnahmeprofil konfigurieren
RecordingProfile profile = RecordingProfile.builder()
    .source("screen://")  // screen://, dshow://, v4l2://, avfoundation://
    .outputFile(Paths.get("C:/Reports/video.mkv"))
    .width(1920)
    .height(1080)
    .fps(30)
    .videoCodec("h264")  // optional: h264, mjpeg, etc.
    .build();

// 3. Aufnahme starten
recorder.start(profile);

// 4. Aufnahme stoppen
recorder.stop();
```

### Quellen-Strings

| Platform | Source String | Beschreibung |
|----------|---------------|--------------|
| Windows  | `screen://`   | DirectShow Screen-Capture |
| macOS    | `screen://`   | AVFoundation Screen-Capture |
| Linux    | `screen://`   | Video4Linux2 / X11 Screen-Capture |

### Backend-Selektion

```java
// Bevorzugtes Backend prüfen
String backend = MediaRuntimeBootstrap.getPreferredBackend();
// Rückgabe: "LibVLC" oder "FFmpeg"

// LibVLC-Verfügbarkeit prüfen
boolean vlcAvailable = MediaRuntimeBootstrap.isLibVlcAvailable();

// Mit interaktiver Bibliothekswahl (bei Bedarf)
MediaRecorder recorder = MediaRuntimeBootstrap.createRecorderInteractive();
```

### Risiken und Hinweise

- **32/64-bit Mismatch:** Java, VLC und OS müssen alle 64-bit sein
- **VLC_PLUGIN_PATH:** Wird automatisch gesetzt, kann aber manuell überschrieben werden
- **VLC-Versionen:** Getestet mit VLC 3.0+
- **Lizenz:** VLC wird dynamisch gebunden (keine statische Verlinkung erforderlich)

### Nicht-Ziele

- Mehrfachaufnahmen parallel (nicht unterstützt)
- Automatischer VLC-Download (muss manuell installiert werden)

---

Überarbeitet von Codex
