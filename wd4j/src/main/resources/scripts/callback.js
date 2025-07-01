function(sendMessage) {
    console.log("✅ callback.js gestartet");
    console.log("✅ BiDi sendMessage:", sendMessage);

    if (typeof sendMessage !== "function") {
        console.error("🚨 WebDriver BiDi: Kein gültiger Message-Channel übergeben!");
        return;
    }

    window.sendBiDiMessage = sendMessage;

    window.sendJsonDataAsArray = function(eventDataArray) {
        if (!Array.isArray(eventDataArray)) {
            console.error("🚨 sendJsonDataAsArray erwartet ein JSON-Array!");
            return;
        }

        try {
            console.log(`📤 Sende ${eventDataArray.length} Events über BiDi`);
            window.sendBiDiMessage({
                type: "recording-event",
                events: eventDataArray
            });
        } catch (error) {
            console.error("🚨 Fehler beim Senden über BiDi:", error);
        }
    };

    console.log("✅ callback.js ist bereit – BiDi Channel für sendJsonDataAsArray gebunden!");
}
