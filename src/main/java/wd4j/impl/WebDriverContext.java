package wd4j.impl;

public class WebDriverContext {
    private static WebSocketConnection connection;

    // Getter für die aktuelle Verbindung
    public static WebSocketConnection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("WebSocketConnection wurde noch nicht initialisiert.");
        }
        return connection;
    }

    // Setter für die Verbindung (z. B. bei der Initialisierung des WebDrivers)
    public static void setConnection(WebSocketConnection newConnection) {
        if (connection != null) {
            throw new IllegalStateException("WebSocketConnection wurde bereits gesetzt.");
        }
        connection = newConnection;
    }

    // Verbindung schließen und Kontext zurücksetzen
    public static void clearConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
