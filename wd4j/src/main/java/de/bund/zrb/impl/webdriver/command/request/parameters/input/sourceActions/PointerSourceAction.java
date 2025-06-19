package de.bund.zrb.impl.webdriver.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Interface für alle PointerSourceActions-Typen.
 */
@JsonAdapter(PointerSourceAction.PointerSourceActionAdapter.class)
public interface PointerSourceAction {
    String getType();

    /**
     * GSON-Adapter für die automatische Serialisierung/Deserialisierung von PointerSourceActions.
     */
    class PointerSourceActionAdapter implements JsonDeserializer<PointerSourceAction>, JsonSerializer<PointerSourceAction> {
        @Override
        public PointerSourceAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in PointerSourceAction JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "pause":
                    return context.deserialize(jsonObject, PauseAction.class);
                case "pointerDown":
                    return context.deserialize(jsonObject, PointerDownAction.class);
                case "pointerUp":
                    return context.deserialize(jsonObject, PointerUpAction.class);
                case "pointerMove":
                    return context.deserialize(jsonObject, PointerMoveAction.class);
                default:
                    throw new JsonParseException("Unknown PointerSourceAction type: " + type);
            }
        }

        @Override
        public JsonElement serialize(PointerSourceAction src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    /**
     * Repräsentiert eine "pointerDown"-Aktion.
     */
    class PointerDownAction extends PointerCommonProperties implements PointerSourceAction {
        private final String type = "pointerDown";
        private final int button;

        public PointerDownAction(int button) {
            super();
            this.button = button;
        }

        public PointerDownAction(int button, PointerCommonProperties commonProperties) {
            super(
                    commonProperties.getWidth(),
                    commonProperties.getHeight(),
                    commonProperties.getPressure(),
                    commonProperties.getTangentialPressure(),
                    commonProperties.getTwist(),
                    commonProperties.getAltitudeAngle(),
                    commonProperties.getAzimuthAngle()
            );
            this.button = button;
        }

        @Override
        public String getType() {
            return type;
        }

        public int getButton() {
            return button;
        }
    }

    /**
     * Repräsentiert eine "pointerUp"-Aktion.
     */
    class PointerUpAction implements PointerSourceAction {
        private final String type = "pointerUp";
        private final int button;

        public PointerUpAction(int button) {
            this.button = button;
        }

        @Override
        public String getType() {
            return type;
        }

        public int getButton() {
            return button;
        }
    }

    /**
     * Repräsentiert eine "pointerMove"-Aktion.
     */
    class PointerMoveAction extends PointerCommonProperties implements PointerSourceAction {
        private final String type = "pointerMove"; // ToDo: This is weird.
        private final double x;
        private final double y;
        private final Long duration; // Optional
        private final Origin origin; // Optional

        public PointerMoveAction(double x, double y) {
            super();
            this.x = x;
            this.y = y;
            this.duration = null;
            this.origin = null;
        }

        public PointerMoveAction(double x, double y, PointerCommonProperties commonProperties) {
            super(
                    commonProperties.getWidth(),
                    commonProperties.getHeight(),
                    commonProperties.getPressure(),
                    commonProperties.getTangentialPressure(),
                    commonProperties.getTwist(),
                    commonProperties.getAltitudeAngle(),
                    commonProperties.getAzimuthAngle()
            ); // Superklasse mit Werten initialisieren

            this.x = x;
            this.y = y;
            this.duration = null;
            this.origin = null;
        }

        public PointerMoveAction(double x, double y, long duration, Origin origin, PointerCommonProperties commonProperties) {
            super(
                    commonProperties.getWidth(),
                    commonProperties.getHeight(),
                    commonProperties.getPressure(),
                    commonProperties.getTangentialPressure(),
                    commonProperties.getTwist(),
                    commonProperties.getAltitudeAngle(),
                    commonProperties.getAzimuthAngle()
            ); // Superklasse mit Werten initialisieren

            this.x = x;
            this.y = y;
            this.duration = duration;
            this.origin = origin;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public Long getDuration() {
            return duration;
        }

        public Origin getOrigin() {
            return origin;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    class PointerCommonProperties implements Serializable {

        private static final long serialVersionUID = 1L;

        private int width = 1;
        private int height = 1;
        private float pressure = 0.0f;
        private float tangentialPressure = 0.0f;
        private int twist = 0;
        private float altitudeAngle = 0.0f; // 0 .. Math.PI / 2
        private float azimuthAngle = 0.0f; // 0 .. 2 * Math.PI

        public PointerCommonProperties() {}

        public PointerCommonProperties(int width, int height, float pressure, float tangentialPressure, int twist, float altitudeAngle, float azimuthAngle) {
            this.setWidth(width);
            this.setHeight(height);
            this.setPressure(pressure);
            this.setTangentialPressure(tangentialPressure);
            this.setTwist(twist);
            this.setAltitudeAngle(altitudeAngle);
            this.setAzimuthAngle(azimuthAngle);
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            if (width < 1) {
                throw new IllegalArgumentException("Width must be >= 1");
            }
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            if (height < 1) {
                throw new IllegalArgumentException("Height must be >= 1");
            }
            this.height = height;
        }

        public float getPressure() {
            return pressure;
        }

        public void setPressure(float pressure) {
            if (pressure < 0.0f || pressure > 1.0f) {
                throw new IllegalArgumentException("Pressure must be between 0.0 and 1.0");
            }
            this.pressure = pressure;
        }

        public float getTangentialPressure() {
            return tangentialPressure;
        }

        public void setTangentialPressure(float tangentialPressure) {
            if (tangentialPressure < 0.0f || tangentialPressure > 1.0f) {
                throw new IllegalArgumentException("Tangential pressure must be between 0.0 and 1.0");
            }
            this.tangentialPressure = tangentialPressure;
        }

        public int getTwist() {
            return twist;
        }

        public void setTwist(int twist) {
            if (twist < 0 || twist > 359) {
                throw new IllegalArgumentException("Twist must be between 0 and 359");
            }
            this.twist = twist;
        }

        public float getAltitudeAngle() {
            return altitudeAngle;
        }

        public void setAltitudeAngle(float altitudeAngle) {
            if (altitudeAngle < 0.0f || altitudeAngle > (float) (Math.PI / 2)) {
                throw new IllegalArgumentException("Altitude angle must be between 0 and PI/2");
            }
            this.altitudeAngle = altitudeAngle;
        }

        public float getAzimuthAngle() {
            return azimuthAngle;
        }

        public void setAzimuthAngle(float azimuthAngle) {
            if (azimuthAngle < 0.0f || azimuthAngle > (float) (2 * Math.PI)) {
                throw new IllegalArgumentException("Azimuth angle must be between 0 and 2*PI");
            }
            this.azimuthAngle = azimuthAngle;
        }

        @Override
        public String toString() {
            return "PointerCommonPropertiesDTO{" +
                    "width=" + width +
                    ", height=" + height +
                    ", pressure=" + pressure +
                    ", tangentialPressure=" + tangentialPressure +
                    ", twist=" + twist +
                    ", altitudeAngle=" + altitudeAngle +
                    ", azimuthAngle=" + azimuthAngle +
                    '}';
        }
    }
}
