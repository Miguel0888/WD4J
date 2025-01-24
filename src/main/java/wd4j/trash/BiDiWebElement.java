// package wd4j.impl;

// import wd4j.api.WebElement;

// public class BiDiWebElement implements WebElement {
//     private final String elementId;

//     public BiDiWebElement(String response) {
//         // Extrahiere die Element-ID aus der BiDi-Antwort (vereinfacht)
//         this.elementId = parseElementId(response);
//     }

//     @Override
//     public void click() {
//         // JSON-Kommando für einen Klick auf das Element erstellen
//         String command = String.format(
//                 "{\"id\":5,\"method\":\"element.click\",\"params\":{\"element\":\"%s\"}}",
//                 elementId
//         );
//         WebSocketConnection connection = WebDriverContext.getConnection();
//         connection.send(command);
//     }

//     @Override
//     public String getText() {
//         // JSON-Kommando für das Abrufen des Textinhalts
//         String command = String.format(
//                 "{\"id\":6,\"method\":\"element.getText\",\"params\":{\"element\":\"%s\"}}",
//                 elementId
//         );
//         WebSocketConnection connection = WebDriverContext.getConnection();
//         connection.send(command);

//         // Beispiel: Antwort aus der Verbindung empfangen
//         try {
//             return connection.receive(); // Implementierung der Antwortverarbeitung
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             throw new RuntimeException("Fehler beim Abrufen des Texts", e);
//         }
//     }

//     private String parseElementId(String response) {
//         // Vereinfachte Extraktion der Element-ID aus der JSON-Antwort
//         return response; // Placeholder (JSON Parsing hier ergänzen)
//     }
// }
