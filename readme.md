# WD4J - WebDriver for Java

WD4J ist eine Java-basierte Implementierung des WebDriver BiDi-Protokolls (BiDirectional), die sich auf moderne, native Java-Lösungen konzentriert. Das Ziel von WD4J ist es, die Vorteile des neuen WebDriver BiDi-Standards zu nutzen und gleichzeitig ein leichtgewichtiges und vollständig in Java geschriebenes Interface anzubieten – ohne den Legacy-Overhead von Selenium.

## Zielsetzung

Das WebDriver BiDi-Protokoll ist eine neue standardisierte Methode für die Kommunikation zwischen Web-Clients und Browsern. WD4J verfolgt das Ziel, einen nativen, browserübergreifenden Java-WebDriver bereitzustellen, der:

1. **Ohne native Executables arbeitet** – keine externen Browser-spezifischen Implementierungen.
2. **Leichtgewichtiger als Selenium ist** – durch Verzicht auf Abwärtskompatibilität und Legacy-Code.
3. **Eine ähnliche API wie Selenium bietet**, um bestehende Entwicklererfahrungen zu nutzen.

---

## Aktueller Stand

### Implementierte Features (Milestone 0)

- **Starten und Beenden von Browser-Instanzen**:
    - Unterstützung für Chrome, Edge, Firefox und Safari.
    - Optionale Konfiguration von Profilpfaden und Startoptionen wie Headless-Modus.

- **WebSocket-Verbindung**:
    - Kommunikation mit den Browsern über WebSocket gemäß BiDi-Protokoll.
    - Verbindung wird bei Beendigung korrekt geschlossen.

- **Grundlegendes WebDriver-Interface**:
    - `BiDiWebDriver` als zentrale Klasse für Steuerung.
    - Navigation zu URLs (`get`-Methode) und erste Unterstützung für `findElement`.

---

## Milestones

### **Milestone 1: Basis-API für WebDriver BiDi**
- Implementierung von grundlegenden WebDriver-BiDi-Funktionen:
    - Navigation (z. B. `get`, `getCurrentUrl`, `getTitle`).
    - Elementinteraktionen (z. B. `findElement`, `click`, `sendKeys`).
- Unterstützung für Browser-spezifische Optionen (z. B. `noRemote`, `disableGpu`).

### **Milestone 2: Erweiterte BiDi-Funktionalitäten**
- Netzwerkinterception und Logging:
    - Abfangen und Bearbeiten von Netzwerkrequests.
    - Zugriff auf Logs (z. B. Konsole, Netzwerkaktivität).
- Unterstützung für Events:
    - Abonnieren von Browser-Ereignissen (z. B. DOM-Änderungen, Netzwerkereignisse).

### **Milestone 3: Vollständiges WebDriver-BiDi-Interface**
- Vollständige Unterstützung aller BiDi-Features gemäß dem W3C-Standard.
- Abbildung des Selenium-WebDriver-Interfaces (ohne den Legacy-Teil).

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

## Beispielcode

Hier ist ein einfacher Anwendungsfall mit WD4J:

```java
import wd4j.impl.BiDiWebDriver;
import wd4j.impl.BrowserType;

public class Example {
    public static void main(String[] args) {
        BiDiWebDriver driver = new BiDiWebDriver(BrowserType.CHROME);
        
        // Navigiere zu einer URL
        driver.get("https://www.example.com");
        
        // Finde ein Element und klicke darauf
        WebElement element = driver.findElement(By.cssSelector("button#submit"));
        element.click();

        // Beende den Browser
        driver.close();
    }
}
```

---

## Contribution

Beiträge sind willkommen! Wenn du helfen möchtest:
- Schaue in den [Issue Tracker](https://github.com/Miguel0888/WD4J/issues), um offene Aufgaben zu finden.
- Reiche Pull Requests ein, um Funktionen oder Fehlerbehebungen vorzuschlagen.

------

## Siehe auch

- [WebDriver BiDi Spezifikation (W3C)](https://w3c.github.io/webdriver-bidi/): Offizielle Dokumentation und Beschreibung des WebDriver BiDi-Protokolls.
- [Session Management im WebDriver BiDi-Protokoll](https://w3c.github.io/webdriver-bidi/#session): Details zur Sitzungserstellung und -verwaltung.
- [Module des WebDriver BiDi-Protokolls](https://w3c.github.io/webdriver-bidi/#modules): Überblick über die einzelnen Module und deren Funktionen (z. B. `session`, `browsingContext`).
- [WebSocket-Kommunikation im BiDi-Protokoll](https://w3c.github.io/webdriver-bidi/#transport): Beschreibung der bidirektionalen Kommunikation zwischen Client und Browser.
- [Capabilities für WebSocket-URLs](https://developer.mozilla.org/en-US/docs/Web/WebDriver/Capabilities/webSocketUrl): Informationen zum Erlangen der WebSocket-URL mit WebDriver-Capabilities.

---

## Lizenz

Dieses Projekt steht unter der **MIT-Lizenz**.

