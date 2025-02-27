(function() {
    function initializeTooltip() {
        if (!document.body) return setTimeout(initializeTooltip, 50);
        var tooltip = document.createElement('div');
        tooltip.style.position = 'fixed';
        tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
        tooltip.style.color = 'white';
        tooltip.style.padding = '5px';
        tooltip.style.borderRadius = '3px';
        tooltip.style.fontSize = '12px';
        tooltip.style.zIndex = '9999';
        tooltip.style.pointerEvents = 'none';
        document.body.appendChild(tooltip);

        function getSelector(element) {
            if (!element) return null;
            if (element.id) return "#" + CSS.escape(element.id); // 🔹 Escape für sichere IDs
            if (element.className) return "." + element.className.split(" ").join(".");
            return element.tagName.toLowerCase();
        }

        document.addEventListener('mouseover', function(event) {
            var el = event.target;
            var selector = getSelector(el);
            tooltip.textContent = selector.replace(/\\3A /g, ":"); // 🔹 Für bessere Lesbarkeit
            tooltip.style.top = (event.clientY + 10) + 'px';
            tooltip.style.left = (event.clientX + 10) + 'px';
            tooltip.style.display = 'block';
        });

        document.addEventListener('mouseout', function() {
            tooltip.style.display = 'none';
        });

        document.addEventListener('click', function(event) {
            let el = event.target;
            let clickedSelector = getSelector(el);
            console.log("Clicked Selector:", clickedSelector);

            // 🔹 Alte Liste aus `localStorage` holen oder leere Liste erstellen
            let storedSelectors = JSON.parse(localStorage.getItem("clickedSelectors") || "[]");

            // 🔹 Neuen Selektor hinzufügen
            storedSelectors.push(clickedSelector);

            // 🔹 Liste zurück in `localStorage` speichern
            localStorage.setItem("clickedSelectors", JSON.stringify(storedSelectors));

            console.log("Gespeicherte Selektoren:", storedSelectors);
        });
    }

    document.addEventListener('DOMContentLoaded', initializeTooltip);
})
