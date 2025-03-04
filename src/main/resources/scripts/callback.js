window.sendSelector = function(eventDataArray) {
    if (!Array.isArray(eventDataArray)) {
        console.error("🚨 sendSelector erwartet ein JSON-Array!");
        return;
    }

    const WS_URL = "ws://localhost:8080";
    const MAX_RETRIES = 5;
    const RETRY_DELAY = 2000;
    let retryCount = 0;
    let messageQueue = [];

    function connectWebSocket() {
        if (window.ws && window.ws.readyState <= 1) {
            console.log("🔄 WebSocket ist bereits verbunden oder verbindet gerade...");
            return;
        }

        console.log(`🔌 Verbinde WebSocket... (Versuch ${retryCount + 1}/${MAX_RETRIES})`);
        window.ws = new WebSocket(WS_URL);

        window.ws.onopen = () => {
            console.log("✅ WebSocket verbunden!");
            retryCount = 0;
            sendQueuedMessages();
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
        if (messageQueue.length === 0) return;

        try {
            console.log(`📤 Sende Nachrichten-Array (${messageQueue.length} Einträge)`);
            window.ws.send(JSON.stringify(messageQueue));
            messageQueue = []; // Warteschlange leeren nach erfolgreichem Senden
        } catch (error) {
            console.error("🚨 Fehler beim Senden des Arrays:", error);
        }
    }

    function safeSend(eventDataArray) {
        if (!window.ws || window.ws.readyState !== WebSocket.OPEN) {
            console.warn("⚠ WebSocket nicht bereit. Nachrichten werden gespeichert:", eventDataArray);
            messageQueue = messageQueue.concat(eventDataArray);
            connectWebSocket();
            return;
        }

        messageQueue = messageQueue.concat(eventDataArray);
        sendQueuedMessages();
    }

    connectWebSocket();

    safeSend(eventDataArray);
}