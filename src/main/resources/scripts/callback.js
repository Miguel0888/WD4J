window.sendSelector = function(selector) {
    const WS_URL = "ws://localhost:8080";
    const MAX_RETRIES = 5; // Maximale Wiederholungen
    const RETRY_DELAY = 2000; // Wartezeit für Reconnects (2 Sekunden)
    let retryCount = 0;
    let messageQueue = []; // Warteschlange für nicht gesendete Nachrichten

    function connectWebSocket() {
        if (window.ws && window.ws.readyState <= 1) {
            console.log("🔄 WebSocket ist bereits verbunden oder verbindet gerade...");
            return;
        }

        console.log(`🔌 Verbinde WebSocket... (Versuch ${retryCount + 1}/${MAX_RETRIES})`);
        window.ws = new WebSocket(WS_URL);

        window.ws.onopen = () => {
            console.log("✅ WebSocket verbunden!");
            retryCount = 0; // Retry-Zähler zurücksetzen
            sendQueuedMessages(); // 🟢 Wartende Nachrichten sofort senden
        };

        window.ws.onmessage = (event) => console.log("🔹 Nachricht vom Server:", event.data);

        window.ws.onerror = (error) => {
            console.warn("⚠ WebSocket-Fehler:", error);
            window.ws.close();
        };

        window.ws.onclose = () => {
            console.warn("❌ WebSocket-Verbindung geschlossen.");
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                console.log(`🔄 Erneuter Verbindungsversuch in ${RETRY_DELAY / 1000} Sekunden...`);
                setTimeout(connectWebSocket, RETRY_DELAY);
            } else {
                console.error("🚨 Max. Anzahl an Wiederholungen erreicht. Kein erneuter Versuch.");
            }
        };
    }

    function sendQueuedMessages() {
        if (!window.ws || window.ws.readyState !== WebSocket.OPEN) return;
        while (messageQueue.length > 0) {
            let msg = messageQueue.shift();
            safeSend(msg);
        }
    }

    function safeSend(message) {
        if (!window.ws || window.ws.readyState !== WebSocket.OPEN) {
            console.warn("⚠ WebSocket nicht bereit. Nachricht wird gespeichert:", message);
            messageQueue.push(message);
            connectWebSocket();
            return;
        }

        try {
            console.log("📤 Sende Nachricht:", message);
            window.ws.send(message);
        } catch (error) {
            console.error("🚨 Fehler beim Senden:", error);
            messageQueue.push(message);
        }
    }

    // Verbindung aufbauen, falls noch nicht verbunden
    connectWebSocket();

    // Selektor senden oder zwischenspeichern, falls Verbindung noch nicht offen ist
    if (typeof selector === "string" && selector.trim() !== "") {
        safeSend(selector);
    }
}
