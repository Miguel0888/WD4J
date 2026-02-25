# wd4j2cdp – WebDriver BiDi ↔ Chrome DevTools Protocol Adapter

Dieses Modul stellt einen Adapter bereit, der WebDriver-BiDi-Kommandos über das
Chrome DevTools Protocol (CDP) tunnelt. Dazu wird der von Google bereitgestellte
**chromium-bidi Mapper** (`mapperTab.js`) in einem versteckten Chrome-Tab
ausgeführt, der eingehende BiDi-Nachrichten in CDP-Aufrufe übersetzt.

## Projektstruktur

```
wd4j2cdp/
├── src/main/java/de/bund/zrb/chrome/
│   ├── ChromeBidiWebSocketImpl.java   ← WDWebSocket-Implementierung für Chrome
│   ├── ChromeBidiTest.java            ← Standalone-Integrationstest
│   └── cdp/
│       ├── CdpConnection.java         ← Low-Level CDP WebSocket-Verbindung
│       └── CdpMapperSetup.java        ← Mapper-Tab Setup (inject + init)
└── src/main/resources/chrome-bidi/
    └── mapperTab.js                   ← Gebündelter chromium-bidi Mapper
```

## Gradle-Einbindung

```groovy
// settings.gradle
include 'wd4j2cdp'

// build.gradle eines konsumierenden Moduls
dependencies {
    implementation project(':wd4j2cdp')
}
```

`wd4j2cdp` hängt von `:wd4j` ab (für die BiDi-Interfaces `WDWebSocket`,
`WebSocketFrame` etc.), aber **nicht** umgekehrt – `wd4j` kennt dieses Modul nicht.

## Lizenz

### Eigener Code (Java-Quellen)

Der Java-Code in diesem Modul unterliegt derselben Lizenz wie das Gesamtprojekt
MainframeMate.

### mapperTab.js – Apache License 2.0

Die Datei `src/main/resources/chrome-bidi/mapperTab.js` stammt aus dem Projekt
[GoogleChromeLabs/chromium-bidi](https://github.com/nickhudkins/nickhudkins-chromium-bidi)
und steht unter der **Apache License, Version 2.0**:

> Copyright Google LLC
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>     https://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.

