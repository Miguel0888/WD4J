(function() {
    document.addEventListener('DOMContentLoaded', function() {
        var ws = new WebSocket("ws://localhost:8080");

        function getSelector(element) {
            if (!element) return null;
            if (element.id) return "#" + CSS.escape(element.id);
            if (element.className) return "." + element.className.split(" ").join(".");
            return element.tagName.toLowerCase();
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
