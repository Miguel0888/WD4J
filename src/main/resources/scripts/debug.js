(() => {
    let isEnabled = false;
    let intervalId = null;

    function animateTextarea(selector) {
        const textarea = document.querySelector(selector);
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
