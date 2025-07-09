function(sendMessage) {
    console.warn("✅ this:", this);
    console.warn("✅ sendMessage:", sendMessage);

    if (typeof sendMessage !== "function") {
        console.error("🚨 WebDriver BiDi: Kein gültiger Message-Channel übergeben!");
        return;
    }

    window.addEventListener("focus", () => {
        console.log("📢 Fenster hat Fokus!");
        let message = {
            type: "focus",
            visibility: document.visibilityState,
            url: window.location.href
        };
        console.log("🚀 Sende Nachricht über BiDi:", message);  // Debugging-Log
        sendMessage(message);
    });

    window.addEventListener("blur", () => {
        console.log("📢 Fenster hat Fokus verloren!");
        let message = {
            type: "blur",
            visibility: document.visibilityState,
            url: window.location.href
        };
        console.log("🚀 Sende Nachricht über BiDi:", message);  // Debugging-Log
        sendMessage(message);
    });

    console.log("✅ Fokus-Tracker-Skript erfolgreich geladen!");
}
