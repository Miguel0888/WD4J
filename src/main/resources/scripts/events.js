(() => {
    let tooltip
    let isTooltipEnabled = false

    function initializeTooltip() {
        if (!document.body) return setTimeout(initializeTooltip, 50)

        if (!tooltip) {
            tooltip = document.createElement('div')
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
            })
            document.body.appendChild(tooltip)
        }
    }

    function getStableSelector(element) {
        if (!element) return null

        if (element.id && !isGeneratedId(element.id)) {
            return `#${element.id}`
        }

        if (element.className && typeof element.className === 'string') {
            const classList = element.className.trim().split(/\s+/).filter(cls => !isGeneratedClass(cls))
            if (classList.length > 0) {
                return `.${classList.join('.')}`
            }
        }

        return getElementPath(element)
    }

    function isGeneratedId(id) {
        return /^([A-Za-z0-9]{8,})$/.test(id)
    }

    function isGeneratedClass(cls) {
        return /^ns-[a-z0-9\-]+$/.test(cls) || /^[A-Za-z0-9]{8,}$/.test(cls)
    }

    function getElementPath(element) {
        if (!element) return 'Unknown'
        let path = []
        while (element.parentElement) {
            let selector = element.tagName.toLowerCase()
            if (element.id && !isGeneratedId(element.id)) {
                selector += `#${element.id}`
                path.unshift(selector)
                break
            } else {
                let siblingIndex = 1
                let sibling = element
                while ((sibling = sibling.previousElementSibling) !== null) {
                    if (sibling.tagName === element.tagName) siblingIndex++
                }
                if (siblingIndex > 1) selector += `:nth-of-type(${siblingIndex})`
            }
            path.unshift(selector)
            element = element.parentElement
        }
        return path.join(' > ')
    }

    function updateTooltipPosition(x, y) {
        if (!tooltip) return
        tooltip.style.top = `${y + 10}px`
        tooltip.style.left = `${x + 10}px`
    }

    function onMouseOver(event) {
        if (!isTooltipEnabled) return
        let selector = getStableSelector(event.target)
        if (selector) {
            tooltip.textContent = selector
            tooltip.style.display = 'block'
            updateTooltipPosition(event.clientX, event.clientY)
        }
    }

    function onMouseOut() {
        if (!isTooltipEnabled) return
        tooltip.style.display = 'none'
    }

    function recordEvent(event) {
        const selector = getStableSelector(event.target)
        if (!selector) return

        let eventData = { selector }

        if (event.type === 'input' || event.type === 'change') {
            eventData.action = 'input'
            eventData.value = event.target.value
        } else if (event.type === 'keydown') {
            eventData.action = 'press'
            eventData.key = event.key
        } else {
            eventData.action = 'click'
        }

        // **Sendet immer Events, unabhängig davon, ob das Tooltip aktiv ist**
        if (typeof window.sendJsonDataAsArray === 'function') {
            window.sendJsonDataAsArray([eventData])
        }
    }

    function rebindEventListeners() {
        console.log('🔄 PrimeFaces AJAX-Update erkannt – Event-Listener werden neu gebunden')

        const elements = document.querySelectorAll('button, a, input, textarea, select, .ui-commandlink, .ui-button')
        elements.forEach(el => {
            el.removeEventListener('click', recordEvent)
            el.addEventListener('click', recordEvent)
            el.removeEventListener('input', recordEvent)
            el.addEventListener('input', recordEvent)
            el.removeEventListener('change', recordEvent)
            el.addEventListener('change', recordEvent)
            el.removeEventListener('keydown', recordEvent)
            el.addEventListener('keydown', recordEvent)
            el.removeEventListener('submit', recordEvent)
            el.addEventListener('submit', recordEvent)
        })
    }

    function watchPrimeFacesAjax() {
        if (window.PrimeFaces) {
            console.log('✅ PrimeFaces erkannt – AJAX-Events werden überwacht')
            PrimeFaces.ajax.Queue.add = function(cfg) {
                console.log('📡 PrimeFaces AJAX-Request gestartet:', cfg)
            }
            PrimeFaces.ajax.Queue.remove = function(cfg) {
                console.log('✅ PrimeFaces AJAX-Request abgeschlossen:', cfg)
                rebindEventListeners()
            }
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        initializeTooltip()
        watchPrimeFacesAjax()
        rebindEventListeners()
        startObserver(); // <-- Observer is started only when DOM is there
    })

    document.addEventListener('mouseover', onMouseOver)
    document.addEventListener('mouseout', onMouseOut)

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

                    // Falls Elemente hinzugefügt wurden, binde Events erneut
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

    // **Expose `toggleTooltip` global, damit du es aktivieren/deaktivieren kannst**
    window.toggleTooltip = function(enable) {
        isTooltipEnabled = enable
        if (!enable && tooltip) {
            tooltip.style.display = 'none'
        }
    }
})