(function() {
    // Nachricht über WebDriver BiDi-Kanal senden
    function sendBiDiMessage(type, detail) {
        window.__webdriver_bidi_channel__.postMessage({
            event: type,
            detail: detail
        });
    }

    // Drag-and-Drop simulieren
    function simulateDragAndDrop(sourceSelector, targetSelector) {
        const sourceElement = document.querySelector(sourceSelector);
        const targetElement = document.querySelector(targetSelector);

        if (!sourceElement || !targetElement) {
            console.error("DragAndDrop failed: Elements not found.");
            return;
        }

        function createEvent(type, options = {}) {
            return new DragEvent(type, {
                bubbles: true,
                cancelable: true,
                dataTransfer: new DataTransfer(),
                ...options
            });
        }

        sourceElement.dispatchEvent(createEvent("dragstart"));
        targetElement.dispatchEvent(createEvent("dragenter"));
        targetElement.dispatchEvent(createEvent("dragover"));
        targetElement.dispatchEvent(createEvent("drop"));
        sourceElement.dispatchEvent(createEvent("dragend"));

        sendBiDiMessage("dragAndDropPerformed", {
            source: sourceSelector,
            target: targetSelector
        });
    }

    // Nutzereingaben als Message-Event weiterleiten
    ["dragstart", "dragenter", "dragover", "drop", "dragend"].forEach(eventType => {
        document.addEventListener(eventType, event => {
            sendBiDiMessage(eventType, {
                target: event.target.tagName,
                pageX: event.pageX,
                pageY: event.pageY
            });
        });
    });

    // Globale Funktion für Java-Befehlsschnittstelle bereitstellen
    window.simulateDragAndDrop = simulateDragAndDrop;
})
