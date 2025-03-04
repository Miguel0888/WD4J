(() => {
    const recordedEvents = [];

    function getStableSelector(element) {
        if (!element) return null;

        // 1️⃣ Falls das Element eine ID hat, aber die ID generisch aussieht, ignorieren
        if (element.id && !isGeneratedId(element.id)) {
            return `#${element.id}`;
        }

        // 2️⃣ Falls das Element eine spezifische Klasse hat, benutzen
        if (element.className && typeof element.className === "string") {
            const classList = element.className.trim().split(/\s+/).filter(cls => !isGeneratedClass(cls));
            if (classList.length > 0) {
                return `.${classList.join(".")}`;
            }
        }

        // 3️⃣ Falls kein guter Selektor gefunden wurde, den allgemeinen Tag mit Index nutzen
        return getElementPath(element);
    }

    function isGeneratedId(id) {
        return /^([A-Za-z0-9]{8,})$/.test(id); // IDs mit zufälliger Struktur ignorieren
    }

    function isGeneratedClass(cls) {
        return /^ns-[a-z0-9\-]+$/.test(cls) || /^[A-Za-z0-9]{8,}$/.test(cls); // Generierte Klassen vermeiden
    }

    function getElementPath(element) {
        if (!element) return "Unknown";
        let path = [];
        while (element.parentElement) {
            let selector = element.tagName.toLowerCase();
            if (element.id && !isGeneratedId(element.id)) {
                selector += `#${element.id}`;
                path.unshift(selector);
                break;
            } else {
                let siblingIndex = 1;
                let sibling = element;
                while ((sibling = sibling.previousElementSibling) !== null) {
                    if (sibling.tagName === element.tagName) siblingIndex++;
                }
                if (siblingIndex > 1) selector += `:nth-of-type(${siblingIndex})`;
            }
            path.unshift(selector);
            element = element.parentElement;
        }
        return path.join(" > ");
    }

    function recordEvent(event) {
        const selector = getStableSelector(event.target);
        if (!selector) return; // Keine unnötigen null-Werte

        let eventData = {
            selector: selector
        };

        if (event.type === "input" || event.type === "change") {
            eventData.action = "input";
            eventData.value = event.target.value;
        } else if (event.type === "keydown") {
            eventData.action = "press";
            eventData.key = event.key;
        } else {
            eventData.action = "click";
        }

        if (typeof window.sendSelector === "function") {
            window.sendSelector(eventData);
        }

        recordedEvents.push(eventData);
    }

    function rebindEventListeners() {
        console.log("🔄 PrimeFaces AJAX-Update erkannt – Event-Listener werden neu gebunden");

        const elements = document.querySelectorAll("button, a, input, textarea, select, .ui-commandlink, .ui-button");
        elements.forEach(el => {
            el.removeEventListener("click", recordEvent);
            el.addEventListener("click", recordEvent);
            el.removeEventListener("input", recordEvent);
            el.addEventListener("input", recordEvent);
            el.removeEventListener("change", recordEvent);
            el.addEventListener("change", recordEvent);
            el.removeEventListener("keydown", recordEvent);
            el.addEventListener("keydown", recordEvent);
            el.removeEventListener("submit", recordEvent);
            el.addEventListener("submit", recordEvent);
        });
    }

    function watchPrimeFacesAjax() {
        if (window.PrimeFaces) {
            console.log("✅ PrimeFaces erkannt – AJAX-Events werden überwacht");
            PrimeFaces.ajax.Queue.add = function(cfg) {
                console.log("📡 PrimeFaces AJAX-Request gestartet:", cfg);
            };
            PrimeFaces.ajax.Queue.remove = function(cfg) {
                console.log("✅ PrimeFaces AJAX-Request abgeschlossen:", cfg);
                rebindEventListeners();
            };
        }
    }

    // Initial Listener setzen
    document.addEventListener("DOMContentLoaded", () => {
        watchPrimeFacesAjax();
        rebindEventListeners();
    });

    // MutationObserver für DOM-Änderungen (z. B. PrimeFaces AJAX)
    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            if (mutation.addedNodes.length > 0) {
                rebindEventListeners();
            }
        });
    });

    observer.observe(document.body, { childList: true, subtree: true });

    window.getRecordedEvents = () => recordedEvents;
})