(function() {
    let tooltip;
    let isEnabled = false;

    function initializeTooltip() {
        if (!document.body) return setTimeout(initializeTooltip, 50);

        if (!tooltip) {
            tooltip = document.createElement('div');
            tooltip.style.position = 'fixed';
            tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
            tooltip.style.color = 'white';
            tooltip.style.padding = '5px';
            tooltip.style.borderRadius = '3px';
            tooltip.style.fontSize = '12px';
            tooltip.style.zIndex = '9999';
            tooltip.style.pointerEvents = 'none';
            tooltip.style.display = 'none';
            document.body.appendChild(tooltip);
        }

        function getSelector(element) {
            if (!element) return null;

            // Falls das Element ein klickbares Element ist, nimm es direkt
            if (element.matches("button, a, [onclick], [data-action]")) {
                return getFullSelector(element);
            }

            // Falls das Element innerhalb eines klickbaren Elements liegt, nimm den nächsten Elternknoten
            var clickableParent = element.closest("button, a, [onclick], [data-action]");
            return clickableParent ? getFullSelector(clickableParent) : getFullSelector(element);
        }

        function escapeCSSSelector(value) {
            return value.replace(/:/g, "\\3A ");
        }

        function getElementInfo(element) {
            if (!element) return "Unbekanntes Element";

            // ID hat höchste Priorität (mit Escaping!)
            if (element.id) return "#" + escapeCSSSelector(element.id);

            // Falls Klassen vorhanden sind, als Selektor zurückgeben
            if (element.className) return "." + element.className.split(/\s+/).join(".");

            // Standardmäßig das Tag-Element zurückgeben
            return element.tagName.toLowerCase();
        }

        function getFullSelector(element) {
            if (!element) return null;
            let selectorParts = [];

            while (element.parentElement) {
                let part = getElementInfo(element);
                let parent = element.parentElement;

                if (parent.children.length === 1 || Array.from(parent.children).indexOf(element) !== -1) {
                    selectorParts.unshift(part);
                } else {
                    selectorParts.unshift(" " + part);
                }

                element = parent;

                if (part.startsWith("#")) break;
            }

            return selectorParts.join(" > ");
        }

        function onMouseOver(event) {
            if (!isEnabled) return;
            var el = event.target;
            var selector = getSelector(el);
            tooltip.textContent = selector;
            tooltip.style.top = (event.clientY + 10) + 'px';
            tooltip.style.left = (event.clientX + 10) + 'px';
            tooltip.style.display = 'block';
        }

        function onMouseOut() {
            if (!isEnabled) return;
            tooltip.style.display = 'none';
        }

        function onClick(event) {
            if (!isEnabled) return;
            let clickedSelector = getSelector(event.target);
            if (typeof window.sendSelector === "function") {
                window.sendSelector(clickedSelector);
            } else {
                console.warn("⚠ Kein Callback definiert. Selektor:", clickedSelector);
            }
        }

        document.addEventListener('mouseover', onMouseOver);
        document.addEventListener('mouseout', onMouseOut);
        document.addEventListener('click', onClick);
    }

    window.toggleTooltip = function(enable) {
        isEnabled = enable;
        if (!isEnabled) {
            tooltip.style.display = 'none';
        }
    };

    document.addEventListener('DOMContentLoaded', initializeTooltip);
})
