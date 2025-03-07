((sendMessage) => {
    // Speichert die übergebene BiDi-Channel-Funktion für spätere Nutzung
    window.sendBiDiMessage = sendMessage;

    window.addEventListener("focus", () => {
        window.sendBiDiMessage({
            type: "focus",
            visibility: document.visibilityState,
            url: window.location.href
        });
    });

    window.addEventListener("blur", () => {
        window.sendBiDiMessage({
            type: "blur",
            visibility: document.visibilityState,
            url: window.location.href
        });
    });

})(arguments[0])
