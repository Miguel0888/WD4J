package de.bund.zrb.command.request;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import de.bund.zrb.webdriver.command.request.parameters.browser.SetClientWindowStateParameters;
import de.bund.zrb.webdriver.command.request.parameters.browser.SetClientWindowStateParameters.ClientWindowNamedState;
import de.bund.zrb.webdriver.command.request.parameters.browser.SetClientWindowStateParameters.ClientWindowRectState;
import de.bund.zrb.webdriver.command.request.parameters.browser.SetClientWindowStateParameters.ClientWindowNamedState.State;
import de.bund.zrb.webdriver.type.browser.WDClientWindow;
import de.bund.zrb.webdriver.mapping.GsonMapperFactory;

import static org.junit.jupiter.api.Assertions.*;

public class BrowserWDRequestTest {

    private final Gson gson = GsonMapperFactory.getGson();

    @Test
    public void testSerializeSetClientWindowStateNamed() {
        // ✅ Named State (fullscreen)
        SetClientWindowStateParameters request = new ClientWindowNamedState(
                new WDClientWindow("client-123"),
                State.FULLSCREEN
        );

        // Serialisieren
        String json = gson.toJson(request);

        // Erwartetes JSON prüfen (es darf KEIN "value" Feld geben!)
        assertTrue(json.contains("\"clientWindow\""));
        assertTrue(json.contains("\"state\":\"fullscreen\""));
        assertFalse(json.contains("\"value\"")); // Kein überflüssiges Feld!

        System.out.println("Named State JSON: " + json);
    }

    @Test
    public void testSerializeSetClientWindowStateRect() {
        // ✅ Rect State (1024x768, x=100, y=50)
        SetClientWindowStateParameters request = new ClientWindowRectState(
                new WDClientWindow("client-456"),
                1024, 768, 100, 50
        );

        // Serialisieren
        String json = gson.toJson(request);

        System.out.println("Rect State JSON: " + json);

        // Erwartetes JSON prüfen
        assertTrue(json.contains("\"clientWindow\""));
        assertTrue(json.contains("\"state\":\"normal\""));
        assertTrue(json.contains("\"width\":1024"));
        assertTrue(json.contains("\"height\":768"));
        assertTrue(json.contains("\"x\":100"));
        assertTrue(json.contains("\"y\":50"));
        assertFalse(json.contains("\"value\"")); // Kein überflüssiges Feld!


    }

    @Test
    public void testDeserializeSetClientWindowStateNamed() {
        String json = "{ \"clientWindow\": \"client-123\", \"state\": \"fullscreen\" }";

        SetClientWindowStateParameters request = gson.fromJson(json, ClientWindowNamedState.class);

        assertNotNull(request);
        assertNotNull(request.getClientWindow());
        assertEquals("client-123", request.getClientWindow().value());
        assertTrue(request instanceof ClientWindowNamedState);
        assertEquals(State.FULLSCREEN, ((ClientWindowNamedState) request).getState());

        System.out.println("Named State Deserialized: " + ((ClientWindowNamedState) request).getState());
    }

    @Test
    public void testDeserializeSetClientWindowStateRect() {
        String json = "{ \"clientWindow\": \"client-456\", \"state\": \"normal\", \"width\": 1024, \"height\": 768, \"x\": 100, \"y\": 50 }";

        SetClientWindowStateParameters request = gson.fromJson(json, ClientWindowRectState.class);

        assertNotNull(request);
        assertNotNull(request.getClientWindow());
        assertEquals("client-456", request.getClientWindow().value());
        assertTrue(request instanceof ClientWindowRectState);

        ClientWindowRectState rectState = (ClientWindowRectState) request;
        assertEquals(1024, rectState.getWidth());
        assertEquals(768, rectState.getHeight());
        assertEquals(100, rectState.getX());
        assertEquals(50, rectState.getY());
        assertEquals(ClientWindowRectState.State.NORMAL, rectState.getState());

        System.out.println("Rect State Deserialized: " + rectState.getState());
    }
}
