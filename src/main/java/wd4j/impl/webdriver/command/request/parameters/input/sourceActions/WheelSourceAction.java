package wd4j.impl.webdriver.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * Interface für alle WheelSourceAction-Typen.
 */
@JsonAdapter(WheelSourceAction.WheelSourceActionAdapter.class)
public interface WheelSourceAction {
    String getType();

    /**
     * GSON-Adapter für die automatische Serialisierung/Deserialisierung von WheelSourceActions.
     */
    class WheelSourceActionAdapter implements JsonDeserializer<WheelSourceAction>, JsonSerializer<WheelSourceAction> {
        @Override
        public WheelSourceAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in WheelSourceAction JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "pause":
                    return context.deserialize(jsonObject, PauseAction.class);
                case "scroll":
                    return context.deserialize(jsonObject, WheelScrollAction.class);
                default:
                    throw new JsonParseException("Unknown WheelSourceAction type: " + type);
            }
        }

        @Override
        public JsonElement serialize(WheelSourceAction src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    /**
     * Repräsentiert eine "scroll"-Aktion für das Wheel-Input.
     */
    class WheelScrollAction implements WheelSourceAction {
        private final String type = "scroll";
        private final int x;
        private final int y;
        private final int deltaX;
        private final int deltaY;
        private final Long duration; // Optional
        private final Origin origin; // Optional, Default: "viewport"

        public WheelScrollAction(int x, int y, int deltaX, int deltaY) {
            this(x, y, deltaX, deltaY, null, Origin.FixedOrigin.VIEWPORT);
        }

        public WheelScrollAction(int x, int y, int deltaX, int deltaY, long duration) {
            this(x, y, deltaX, deltaY, duration, Origin.FixedOrigin.VIEWPORT);
        }

        public WheelScrollAction(int x, int y, int deltaX, int deltaY, Origin origin) {
            this(x, y, deltaX, deltaY, null, origin);
        }

        public WheelScrollAction(int x, int y, int deltaX, int deltaY, Long duration, Origin origin) {
            this.x = x;
            this.y = y;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.duration = duration;
            this.origin = origin;
        }

        @Override
        public String getType() {
            return type;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getDeltaX() {
            return deltaX;
        }

        public int getDeltaY() {
            return deltaY;
        }

        public Long getDuration() {
            return duration;
        }

        public Origin getOrigin() {
            return origin;
        }
    }

}
