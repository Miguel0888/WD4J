(function() {
    document.addEventListener('DOMContentLoaded', function() {
        var ws = new WebSocket("ws://localhost:8080");

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

                // Prüfen, ob das Element direkt im Elternteil liegt → dann `>` verwenden
                if (parent.children.length === 1 || Array.from(parent.children).indexOf(element) !== -1) {
                    selectorParts.unshift(part);
                } else {
                    selectorParts.unshift(" " + part);
                }

                element = parent;

                // Falls ein eindeutiger Selektor erreicht wurde (eine ID), abbrechen
                if (part.startsWith("#")) break;
            }

            return selectorParts.join(" > ");
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

        ws.onopen = function() {
            console.log("✅ WebSocket verbunden!");
        };

        ws.onmessage = function(event) {
            console.log("🔹 Nachricht vom Server:", event.data);
        };

        document.addEventListener('click', function(event) {
            let clickedSelector = getSelector(event.target);
            console.log("📌 Geklickt:", clickedSelector);
            ws.send(clickedSelector);
        });
    });
})
