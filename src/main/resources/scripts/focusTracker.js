function(sendMessage) {
    console.warn("✅ this:", this); // Sollte `window` sein
    console.warn("✅ sendMessage:", sendMessage); // Sollte die BiDi-Nachrichtenfunktion sein

    if (typeof sendMessage !== "function") {
        console.error("🚨 WebDriver BiDi: Kein gültiger Message-Channel übergeben!");
        return;
    }

    // Speichert die übergebene BiDi-Channel-Funktion für spätere Nutzung
    window.sendBiDiMessage = sendMessage;

    // Event: Tab bekommt Fokus
    window.addEventListener("focus", () => {
        console.log("📢 Fenster hat Fokus!");
        window.sendBiDiMessage({
            type: "focus",
            visibility: document.visibilityState,
            url: window.location.href
        });
    });

    // Event: Tab verliert Fokus
    window.addEventListener("blur", () => {
        console.log("📢 Fenster hat Fokus verloren!");
        window.sendBiDiMessage({
            type: "blur",
            visibility: document.visibilityState,
            url: window.location.href
        });
    });

    console.log("✅ Fokus-Tracker-Skript erfolgreich geladen!");
}
