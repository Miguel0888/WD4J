(() => {
    let tooltip;
    let isTooltipEnabled = false;

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

    function recordEvent(event) {
        let target = event.target;

        // Prüfen, ob innerhalb eines Buttons, Links, Inputs oder Dropdowns geklickt wurde
        let interactiveElement = target.closest('button, a, input, select, textarea, [role="button"], [role="menuitem"], tr.ui-selectonemenu-item, [role="navigation"] a');

        if (!interactiveElement) {
            interactiveElement = target; // Falls kein übergeordnetes interaktives Element gefunden wird, das ursprüngliche Element nutzen
        }

        const selector = getStableSelector(interactiveElement);
        if (!selector) return;

        let eventData = {
            selector,
            action: event.type === 'input' || event.type === 'change' ? 'input' :
                event.type === 'keydown' ? 'press' : 'click'
        };

        // Falls ein Input-Event, speichere den Wert
        if (eventData.action === 'input') {
            eventData.value = interactiveElement.value;
        } else if (eventData.action === 'press') {
            eventData.key = event.key;
        }

        // Falls das Element ein Button oder ein Paginierungs-Link ist, speichere wichtige Daten
        if (interactiveElement.tagName === 'BUTTON' || interactiveElement.getAttribute('role') === 'button' || interactiveElement.matches('.ui-paginator a')) {
            eventData.buttonText = interactiveElement.textContent.trim() || null;
            eventData.ariaLabel = interactiveElement.getAttribute('aria-label') || null;
        }

        // Falls ein Paginierungs-Element angeklickt wurde, speichere weitere Infos
        if (interactiveElement.matches('.ui-paginator a')) {
            eventData.pagination = interactiveElement.closest('[role="navigation"]')?.getAttribute('aria-label') || "Unbekannte Paginierung";
            eventData.pageNumber = interactiveElement.textContent.trim() || null; // Die sichtbare Seitenzahl
        }

        // Falls das Element ein Menü-Item oder ein Dropdown-Eintrag ist
        let menuItem = interactiveElement.closest('.ui-menuitem, tr.ui-selectonemenu-item');
        if (menuItem) {
            eventData.menuText = Array.from(menuItem.querySelectorAll('td'))
                .map(td => td.textContent.trim())
                .filter(text => text.length > 0)
                .join(' | ');
            eventData.ariaLabel = menuItem.getAttribute('aria-label') || null;
        }

        // Zusätzliche Metadaten für besseres Wiederfinden
        eventData.xpath = getElementXPath(interactiveElement);
        eventData.elementId = interactiveElement.id || null;
        eventData.classes = interactiveElement.className || null;

        if (typeof window.sendJsonDataAsArray === 'function') {
            window.sendJsonDataAsArray([eventData]);
        }
    }


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

    function startObserver() {
        const observer = new MutationObserver(mutations => {
            let recordedMutations = [];

            mutations.forEach(mutation => {
                if (mutation.type === "childList") {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === 1) { // Element-Node
                            const selector = getStableSelector(node);
                            if (selector) {
                                recordedMutations.push({ action: "added", selector });
                            }
                        }
                    });

                    mutation.removedNodes.forEach(node => {
                        if (node.nodeType === 1) { // Element-Node
                            const selector = getStableSelector(node);
                            if (selector) {
                                recordedMutations.push({ action: "removed", selector });
                            }
                        }
                    });

                    if (mutation.addedNodes.length > 0) {
                        rebindEventListeners();
                    }
                }

                if (mutation.type === "attributes") {
                    const selector = getStableSelector(mutation.target);
                    if (selector) {
                        recordedMutations.push({
                            action: "attributeChanged",
                            selector,
                            attribute: mutation.attributeName,
                            newValue: mutation.target.getAttribute(mutation.attributeName),
                        });
                    }
                }

                if (mutation.type === "characterData") {
                    const selector = getStableSelector(mutation.target.parentElement);
                    if (selector) {
                        recordedMutations.push({
                            action: "textChanged",
                            selector,
                            newValue: mutation.target.nodeValue,
                        });
                    }
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
                characterData: true
            });
            console.log("🔍 MutationObserver gestartet und überwacht DOM-Änderungen.");
        } catch (e) {
            console.error("🚨 MutationObserver konnte nicht gestartet werden:", e);
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

    function getElementText(element) {
        if (!element) return null;

        // Falls es sich um eine `<tr>`-Zeile handelt, alle `<td>`-Zellen auslesen
        if (element.tagName.toLowerCase() === "tr") {
            let cells = Array.from(element.querySelectorAll("td"));
            let text = cells.map(td => td.textContent.trim()).join(" | ");
            if (text) return text;
        }

        // Normaler Textinhalt für andere Elemente
        let text = element.textContent.trim();
        if (text) return text;

        // Falls kein Text gefunden wurde, prüfe auf verschachtelte `span` oder `td`
        let subElement = element.querySelector("span, td");
        if (subElement) return subElement.textContent.trim();

        // Falls immer noch nichts gefunden wurde, prüfe auf `aria-labelledby`
        let ariaLabelledBy = element.getAttribute("aria-labelledby");
        if (ariaLabelledBy) {
            let labelledElement = document.getElementById(ariaLabelledBy);
            if (labelledElement) return labelledElement.textContent.trim();
        }

        return "Unbekannt";
    }


    window.toggleTooltip = function(enable) {
        isTooltipEnabled = enable;
        if (!enable && tooltip) {
            tooltip.style.display = 'none';
        }
    };
})