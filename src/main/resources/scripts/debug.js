(() => {
    let isEnabled = false;
    let intervalId = null;

    function findElement(selector) {
        if (selector.startsWith("/") || selector.startsWith("(//")) {
            let result = document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
            return result.singleNodeValue;
        } else {
            return document.querySelector(selector);
        }
    }

    function animateTextarea(element) {
        const symbols = ["😂", "🤣", "😜", "🤩", "💩", "🙃", "🦄", "🐱‍👤", "🚀", "🎉"];
        let index = 0;
        intervalId = setInterval(() => {
            element.value = symbols[index % symbols.length];
            index++;
        }, 300);
    }

    function animateInput(element) {
        const story = [
            "📢 Hey!",
            "😃 Wie geht’s?",
            "🤔 Alles gut?",
            "🤣 Haha!",
            "👋 Tschüss!"
        ];
        let index = 0;

        intervalId = setInterval(() => {
            element.value = story[index % story.length]; // Setzt den Text aus der Story
            index++;
        }, 1000); // Ändert den Text jede Sekunde
    }

    function animateButtonText(element) {
        const story = [
            "🏗 Vorbereitung...",
            "🔥 Zündung...",
            "🚀 Abheben!",
            "🌕 Mond erreicht!",
            "🎉 Party im All!"
        ];
        let index = 0;
        intervalId = setInterval(() => {
            element.innerText = story[index % story.length];
            index++;
        }, 1000);
    }

    function testButton(element) {
        animateButtonText(element);
    }

    function testSelect(element) {
        let options = element.options;
        let index = 0;
        intervalId = setInterval(() => {
            element.selectedIndex = index % options.length;
            index++;
        }, 1000);
    }

    window.testSelector = function(enable, selector) {
        isEnabled = enable;
        const element = findElement(selector);

        if (!element) {
            console.log(`❌ Element mit Selektor ${selector} nicht gefunden!`);
            return;
        }

        if (!enable) {
            if (intervalId) clearInterval(intervalId);
            console.log("🛑 Animation gestoppt.");
            return;
        }

        console.log("▶️ Animation gestartet für", element.tagName.toLowerCase());

        switch (element.tagName.toLowerCase()) {
            case "textarea":
                animateTextarea(element);
                break;
            case "input":
                animateInput(element);
                break;
            case "button":
                testButton(element);
                break;
            case "select":
                testSelect(element);
                break;
            default:
                console.log("⚠️ Kein passendes Test-Szenario für", element.tagName);
        }
    };
})
