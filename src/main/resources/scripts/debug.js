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

    function testTextarea(element) {
        const symbols = ["😂", "🤣", "😜", "🤩", "💩", "🙃", "🦄", "🐱‍👤", "🚀", "🎉"];
        let index = 0;
        intervalId = setInterval(() => {
            element.value = symbols[index % symbols.length];
            index++;
        }, 300);
    }

    function animateInput(element) {
        const story = ["📢 Hey!", "😃 Wie geht’s?", "🤔 Alles gut?", "🤣 Haha!", "👋 Tschüss!"];
        let index = 0;
        intervalId = setInterval(() => {
            element.value = story[index % story.length];
            index++;
        }, 1000);
    }

    function testButtonText(element) {
        const story = ["🏗 Vorbereitung...", "🔥 Zündung...", "🚀 Abheben!", "🌕 Mond erreicht!", "🎉 Party im All!"];
        let index = 0;
        intervalId = setInterval(() => {
            element.innerText = story[index % story.length];
            index++;
        }, 1000);
    }

    function testSelect(element) {
        let options = element.options;
        let index = 0;
        intervalId = setInterval(() => {
            element.selectedIndex = index % options.length;
            index++;
        }, 1000);
    }

    function testFileInput(element) {
        const filenames = ["📂 report.pdf", "📸 selfie.png", "📁 project.zip", "🎵 song.mp3"];
        let index = 0;
        intervalId = setInterval(() => {
            element.value = filenames[index % filenames.length]; // Zeigt Fake-Dateinamen
            index++;
        }, 1500);
    }

    function testImage(element) {
        const images = [
            "https://via.placeholder.com/100?text=😂",
            "https://via.placeholder.com/100?text=🚀",
            "https://via.placeholder.com/100?text=🤩",
            "https://via.placeholder.com/100?text=🔥"
        ];
        let index = 0;
        intervalId = setInterval(() => {
            element.src = images[index % images.length];
            index++;
        }, 1200);
    }

    function testLink(element) {
        intervalId = setInterval(() => {
            element.style.color = element.style.color === "red" ? "blue" : "red";
        }, 700);
    }

    function testCheckbox(element) {
        intervalId = setInterval(() => {
            element.checked = !element.checked;
        }, 1000);
    }

    function testRadio(element) {
        let radios = document.querySelectorAll(`input[type="radio"][name="${element.name}"]`);
        let index = 0;
        intervalId = setInterval(() => {
            radios.forEach(r => (r.checked = false));
            radios[index % radios.length].checked = true;
            index++;
        }, 1200);
    }

    function testProgress(element) {
        let value = 0;
        intervalId = setInterval(() => {
            value = (value + 10) % 100;
            element.value = value;
        }, 800);
    }

    function testTextElement(element) {
        const texts = ["✨ Magic!", "🌟 Wow!", "🎨 Kunst!", "💡 Idee!"];
        let index = 0;
        intervalId = setInterval(() => {
            element.innerText = texts[index % texts.length];
            index++;
        }, 1200);
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
                testTextarea(element);
                break;
            case "input":
                if (element.type === "file") testFileInput(element);
                else if (element.type === "checkbox") testCheckbox(element);
                else if (element.type === "radio") testRadio(element);
                else animateInput(element);
                break;
            case "button":
                testButtonText(element);
                break;
            case "select":
                testSelect(element);
                break;
            case "a":
                testLink(element);
                break;
            case "img":
                testImage(element);
                break;
            case "progress":
            case "meter":
                testProgress(element);
                break;
            case "div":
            case "span":
            case "p":
                testTextElement(element);
                break;
            default:
                console.log("⚠️ Kein passendes Test-Szenario für", element.tagName);
        }
    };
})
