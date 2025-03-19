(() => {
    let isEnabled = false;
    let intervalId = null;

    function findElement(selector) {
        // XPath erkennen, falls Selektor mit "/" oder "(//" beginnt
        if (selector.startsWith("/") || selector.startsWith("(//")) {
            let result = document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
            return result.singleNodeValue;
        } else {
            return document.querySelector(selector);
        }
    }

    function animateTextarea(selector) {
        const textarea = findElement(selector);  // 🔄 Jetzt mit XPath-Unterstützung
        if (!textarea) {
            console.log(`❌ Element mit Selektor ${selector} nicht gefunden!`);
            return;
        }

        const symbols = ["😂", "🤣", "😜", "🤩", "💩", "🙃", "🦄", "🐱‍👤", "🚀", "🎉"];
        let index = 0;

        intervalId = setInterval(() => {
            textarea.value = symbols[index % symbols.length];
            index++;
        }, 300); // Alle 300ms wechseln
    }

    window.toggleAnimation = function(enable, selector) {
        isEnabled = enable;
        if (!enable) {
            if (intervalId) clearInterval(intervalId);
            console.log("🛑 Animation gestoppt.");
        } else {
            console.log("▶️ Animation gestartet.");
            animateTextarea(selector);
        }
    };
})
