let selector = localStorage.getItem("lastClickedSelector");
if (selector) {
    let element = document.querySelector(selector);
    if (element) {
        console.log("Element gefunden:", element);
        element.click(); // 🔹 Automatisierter Test: Klicken auf das gespeicherte Element!
    } else {
        console.error("Element nicht gefunden für Selektor:", selector);
    }
}
