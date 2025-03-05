(() => {
    let tooltip;
    let isTooltipEnabled = false;
    let isDomObserverEnabled = false;

    function initializeTooltip() {
        if (!document.body) return setTimeout(initializeTooltip, 50);

        if (!tooltip) {
            tooltip = document.createElement('div');
            Object.assign(tooltip.style, {
                position: 'fixed',
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                color: 'white',
                padding: '5px',
                borderRadius: '3px',
                fontSize: '12px',
                zIndex: '9999',
                pointerEvents: 'none',
                display: 'none'
            });
            document.body.appendChild(tooltip);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    function getStableSelector(element) {
        if (!element) return null;

        if (element.id && !isGeneratedId(element.id)) {
            return `#${element.id}`;
        }

        if (element.className && typeof element.className === 'string') {
            const classList = element.className.trim().split(/\s+/).filter(cls => !isGeneratedClass(cls));
            if (classList.length > 0) {
                return `.${classList.join('.')}`;
            }
        }

        return getElementPath(element);
    }

    function isGeneratedId(id) {
        return /^([A-Za-z0-9]{8,})$/.test(id);
    }

    function isGeneratedClass(cls) {
        return /^ns-[a-z0-9\-]+$/.test(cls) || /^[A-Za-z0-9]{8,}$/.test(cls);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    const createEventDTO = () => ({
        selector: null,       // CSS-Selektor
        action: null,         // Aktion (click, input, press etc.)
        value: null,          // Falls ein Input-Event -> Eingabewert
        key: null,            // Falls eine Taste gedrückt wurde
        extractedText: null,  // Extrahierter Text (z. B. aus Menüs oder Buttons)
        extractedColumns: null, // JSON-Array für Spalten in Tabellen
        inputName: null,      // Falls ein Formularfeld geklickt wurde -> Name des Inputs
        buttonText: null,     // Falls ein Button geklickt wurde -> Button-Text
        pagination: null,     // Falls ein Paginierungs-Link geklickt wurde -> Paginierungsbereich
        elementId: null,      // Falls vorhanden -> ID des Elements
        classes: null,        // Falls vorhanden -> Klassen des Elements
        xpath: null,          // XPath des Elements
        aria: null,           // JSON-Objekt mit allen `aria-*`-Attributen
        attributes: null,     // JSON-Objekt mit relevanten Attributen (type, maxlength, data-*, etc.)
        test: null            // JSON-Objekt mit allen `test-*` Attributen
    });

    function recordEvent(event) {
        let target = event.target;
        let interactiveElement = findInteractiveElement(target);

        if (!interactiveElement) {
            interactiveElement = target;
        }

        const selector = getStableSelector(interactiveElement);
        if (!selector) return;

        let eventData = createEventDTO();
        eventData.selector = selector;
        eventData.action = determineAction(event);

        if (eventData.action === 'input') {
            eventData.value = interactiveElement.value || null;
        } else if (eventData.action === 'press') {
            eventData.key = event.key || null;
        }

        extractButtonOrMenuInfo(interactiveElement, eventData);
        extractTableData(interactiveElement, eventData);
        extractFormFieldInfo(interactiveElement, eventData);
        extractNavigationInfo(interactiveElement, eventData);
        extractAriaAttributes(interactiveElement, eventData);
        extractOtherAttributes(interactiveElement, eventData);
        extractTestAttributes(interactiveElement, eventData);

        eventData.xpath = getElementXPath(interactiveElement);
        eventData.elementId = interactiveElement.id || null;
        eventData.classes = interactiveElement.className || null;

        sendSanitizedData(eventData);

        // Tooltip anzeigen, falls aktiviert
        showTooltip(eventData, event);
    }

    /** 🔹 Entfernt alle `null`-Werte und sendet die Daten */
    function sendSanitizedData(eventData) {
        try {
            let sanitizedData = Object.fromEntries(
                Object.entries(eventData).filter(([_, value]) => value !== null)
            );

            if (typeof window.sendJsonDataAsArray === 'function') {
                window.sendJsonDataAsArray([sanitizedData]);
            }
        } catch (error) {
            console.error("Fehler in sendSanitizedData:", error);
            return null; // Alternativ: leeres Array `[]` oder ein Standardwert zurückgeben
        }
    }

    /** 🔹 Findet interaktive Elemente wie Buttons, Links, Inputs oder Menüpunkte */
    function findInteractiveElement(target) {
        return target.closest('button, a, input, select, textarea, [role="button"], [role="menuitem"], tr, td, li, [role="navigation"] a');
    }

    /** 🔹 Bestimmt die Aktion basierend auf dem Event-Typ */
    function determineAction(event) {
        if (event.type === 'input' || event.type === 'change') return 'input';
        if (event.type === 'keydown') return 'press';
        return 'click';
    }

    /** 🔹 Extrahiert Button- oder Menüinformationen */
    function extractButtonOrMenuInfo(element, eventData) {
        if (element.tagName === 'BUTTON' || element.getAttribute('role') === 'button' || element.matches('[role="navigation"] a')) {
            eventData.buttonText = element.textContent.trim() || null;
        }

        let menuItem = element.closest('[role="menuitem"], [role="tab"], li');
        if (menuItem) {
            let linkInside = menuItem.querySelector('a');
            eventData.extractedText = (linkInside ? linkInside.textContent.trim() : menuItem.textContent.trim()) || null;
        }
    }

    /** 🔹 Extrahiert Tabellendaten, falls innerhalb einer Tabelle */
    function extractTableData(element, eventData) {
        let tableRow = element.closest('tr');
        if (tableRow) {
            let columnData = Array.from(tableRow.querySelectorAll('td'))
                .map(td => td.textContent.trim())
                .filter(text => text.length > 0);

            eventData.extractedColumns = columnData.length > 0 ? columnData : null;
        }
    }

    /** 🔹 Extrahiert Formularfeld-Informationen */
    function extractFormFieldInfo(element, eventData) {
        if (element.tagName === 'INPUT' || element.tagName === 'SELECT' || element.tagName === 'TEXTAREA') {
            eventData.inputName = element.name || null;
        }
    }

    /** 🔹 Extrahiert Informationen zur Navigation (Paginierung) */
    function extractNavigationInfo(element, eventData) {
        let navigation = element.closest('[role="navigation"]');
        if (navigation) {
            eventData.pagination = navigation.getAttribute('aria-label') || "Unbekannte Navigation";
        }
    }

    /** 🔹 Extrahiert alle `aria-*` Attribute als JSON */
    function extractAriaAttributes(element, eventData) {
        let ariaAttributes = {};
        Array.from(element.attributes).forEach(attr => {
            if (attr.name.startsWith("aria-")) {
                ariaAttributes[attr.name] = attr.value;
            }
        });
        eventData.aria = Object.keys(ariaAttributes).length > 0 ? ariaAttributes : null;
    }

    /** 🔹 Extrahiert andere relevante Attribute */
    function extractOtherAttributes(element, eventData) {
        let attributeList = ["type", "maxlength", "autocomplete"];
        let attributes = {};
        attributeList.forEach(attr => {
            let value = element.getAttribute(attr);
            if (value !== null) {
                attributes[attr] = value;
            }
        });

        // Auch `data-*` Attribute speichern
        Array.from(element.attributes).forEach(attr => {
            if (attr.name.startsWith("data-")) {
                attributes[attr.name] = attr.value;
            }
        });

        eventData.attributes = Object.keys(attributes).length > 0 ? attributes : null;
    }

    /** 🔹 Extrahiert alle `test-*` Attribute für automatisierte Tests */
    function extractTestAttributes(element, eventData) {
        let testAttributes = {};
        Array.from(element.attributes).forEach(attr => {
            if (attr.name.startsWith("test-")) {
                testAttributes[attr.name] = attr.value;
            }
        });

        eventData.test = Object.keys(testAttributes).length > 0 ? testAttributes : null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    function rebindEventListeners() {
        console.log('🔄 PrimeFaces AJAX-Update erkannt – Event-Listener werden neu gebunden');

        const elements = document.querySelectorAll('button, a, input, textarea, select, .ui-commandlink, .ui-button');
        elements.forEach(el => {
            el.removeEventListener('click', recordEvent);
            el.addEventListener('click', recordEvent);
            el.removeEventListener('input', recordEvent);
            el.addEventListener('input', recordEvent);
            el.removeEventListener('change', recordEvent);
            el.addEventListener('change', recordEvent);
            el.removeEventListener('keydown', recordEvent);
            el.addEventListener('keydown', recordEvent);
            el.removeEventListener('submit', recordEvent);
            el.addEventListener('submit', recordEvent);
        });
    }

    function watchPrimeFacesAjax() {
        if (window.PrimeFaces) {
            console.log('✅ PrimeFaces erkannt – AJAX-Events werden überwacht');
            PrimeFaces.ajax.Queue.add = function(cfg) {
                console.log('📡 PrimeFaces AJAX-Request gestartet:', cfg);
            };
            PrimeFaces.ajax.Queue.remove = function(cfg) {
                console.log('✅ PrimeFaces AJAX-Request abgeschlossen:', cfg);
                rebindEventListeners();
            };
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        initializeTooltip();
        watchPrimeFacesAjax();
        rebindEventListeners();
        startObserver();

        // Capture-Phase-Listener für Clicks und Tastaturanschläge
        document.body.addEventListener('click', recordEvent, true);
        document.body.addEventListener('keydown', recordEvent, true);
    });

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////// MutationObserver ///////////////////////////////////////////

    const createMutationDTO = () => ({
        action: null,       // Typ der Mutation (added, removed, attributeChanged, textChanged)
        selector: null,     // CSS-Selektor des betroffenen Elements
        attribute: null,    // Falls `attributeChanged` -> Name des geänderten Attributs
        oldValue: null,     // Falls `attributeChanged` -> Alter Wert des Attributs
        newValue: null,     // Falls `attributeChanged` -> Neuer Wert des Attributs
        extractedText: null,// Falls `textChanged` -> Neuer Text-Inhalt
        elementId: null,    // Falls vorhanden -> ID des Elements
        classes: null,      // Falls vorhanden -> Klassen des Elements
        attributes: null,   // Falls vorhanden -> JSON-Objekt mit weiteren relevanten Attributen
        aria: null,         // Falls vorhanden -> JSON-Objekt mit `aria-*` Attributen
        test: null          // Falls vorhanden -> JSON-Objekt mit `test-*` Attributen
    });

    function startObserver() {
        const observer = new MutationObserver(mutations => {
            if (!isDomObserverEnabled) return; // ❗ DOM-Events unterdrücken, wenn deaktiviert
            let recordedMutations = [];

            mutations.forEach(mutation => {
                let mutationData = createMutationDTO();

                if (mutation.type === "childList") {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === 1) { // Element-Node
                            mutationData.action = "added";
                            mutationData.selector = getStableSelector(node);
                            mutationData.elementId = node.id || null;
                            mutationData.classes = node.className || null;

                            extractAriaAttributes(node, mutationData);
                            extractTestAttributes(node, mutationData);
                            extractOtherAttributes(node, mutationData);

                            recordedMutations.push(sanitizeMutationData(mutationData));
                        }
                    });

                    mutation.removedNodes.forEach(node => {
                        if (node.nodeType === 1) {
                            mutationData.action = "removed";
                            mutationData.selector = getStableSelector(node);
                            recordedMutations.push(sanitizeMutationData(mutationData));
                        }
                    });

                    if (mutation.addedNodes.length > 0) {
                        rebindEventListeners();
                    }
                }

                if (mutation.type === "attributes") {
                    mutationData.action = "attributeChanged";
                    mutationData.selector = getStableSelector(mutation.target);
                    mutationData.attribute = mutation.attributeName;
                    mutationData.newValue = mutation.target.getAttribute(mutation.attributeName);
                    mutationData.oldValue = mutation.oldValue || null;
                    recordedMutations.push(sanitizeMutationData(mutationData));
                }

                if (mutation.type === "characterData") {
                    mutationData.action = "textChanged";
                    mutationData.selector = getStableSelector(mutation.target.parentElement);
                    mutationData.extractedText = mutation.target.nodeValue;
                    recordedMutations.push(sanitizeMutationData(mutationData));
                }
            });

            if (recordedMutations.length > 0 && typeof window.sendJsonDataAsArray === "function") {
                window.sendJsonDataAsArray(recordedMutations);
            }
        });

        try {
            observer.observe(document.body, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeOldValue: true,
                characterData: true
            });
            console.log("🔍 MutationObserver mit einheitlichem DTO gestartet.");
        } catch (e) {
            console.error("🚨 MutationObserver konnte nicht gestartet werden:", e);
        }
    }

    function sanitizeMutationData(mutationData) {
        try {
            return Object.fromEntries(
                Object.entries(mutationData).filter(([_, value]) => value !== null)
            );
        } catch (error) {
            console.error("Fehler in sanitizeMutationData:", error);
            return null; // Alternativ: leeres Array `[]` oder ein Standardwert zurückgeben
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    function getElementPath(element) {
        if (!element) return 'Unknown';
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
        return path.join(' > ');
    }

    function getElementXPath(element) {
        if (!element || element.nodeType !== 1) return '';
        if (element.id) return `//*[@id='${element.id}']`;
        let path = [];
        while (element.nodeType === 1) {
            let index = 1;
            let sibling = element;
            while ((sibling = sibling.previousElementSibling) !== null) {
                if (sibling.nodeType === 1 && sibling.tagName === element.tagName) index++;
            }
            path.unshift(`${element.tagName.toLowerCase()}[${index}]`);
            element = element.parentNode;
        }
        return '/' + path.join('/');
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    function showTooltip(eventData, event) {
        if (!isTooltipEnabled || !eventData) return;

        if (!tooltip) initializeTooltip();

        // JSON hübsch formatieren
        const formattedData = JSON.stringify(eventData, null, 2)
            .replace(/\n/g, "<br>")
            .replace(/\s/g, "&nbsp;");

        // Tooltip-Inhalt setzen
        tooltip.innerHTML = `<strong>Selektor:</strong> ${eventData.selector}<br><pre>${formattedData}</pre>`;

        // Tooltip-Position setzen
        tooltip.style.left = `${event.pageX + 10}px`;
        tooltip.style.top = `${event.pageY + 10}px`;
        tooltip.style.display = 'block';
    }

    /////////////////////////////////////////// Toggles ///////////////////////////////////////////

    window.toggleTooltip = function(enable) {
        isTooltipEnabled = enable;
        if (!enable && tooltip) {
            tooltip.style.display = 'none';
        }
    };

    window.toggleDomObserver = function(enable) {
        isDomObserverEnabled = enable;
    };
})