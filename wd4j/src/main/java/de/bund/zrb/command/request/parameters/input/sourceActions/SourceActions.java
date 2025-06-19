package de.bund.zrb.command.request.parameters.input.sourceActions;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.support.mapping.EnumWrapper;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Interface für alle SourceActions-Typen.
 */
@JsonAdapter(SourceActions.SourceActionsAdapter.class)
public interface SourceActions {
    String getType();

    /**
     * GSON-Adapter für die automatische Serialisierung/Deserialisierung von SourceActions.
     */
    class SourceActionsAdapter implements JsonDeserializer<SourceActions>, JsonSerializer<SourceActions> {
        @Override
        public SourceActions deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in SourceActions JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "none":
                    return context.deserialize(jsonObject, NoneSourceActions.class);
                case "key":
                    return context.deserialize(jsonObject, KeySourceActions.class);
                case "pointer":
                    return context.deserialize(jsonObject, PointerSourceActions.class);
                case "wheel":
                    return context.deserialize(jsonObject, WheelSourceActions.class);
                default:
                    throw new JsonParseException("Unknown SourceActions type: " + type);
            }
        }

        @Override
        public JsonElement serialize(SourceActions src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

    /**
     * Repräsentiert eine "none"-Aktion.
     */
    class NoneSourceActions implements SourceActions {
        private final String type = "none";
        private final String id;
        private final List<PauseAction> actions; // aka. NoneSourceAction

        public NoneSourceActions(String id, List<PauseAction> actions) {
            this.id = id;
            this.actions = actions;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public List<PauseAction> getActions() {
            return actions;
        }
    }

    /**
     * Repräsentiert eine "key"-Aktion.
     */
    class KeySourceActions implements SourceActions {
        private final String type = "key";
        private final String id;
        private final List<KeySourceAction> actions;

        public KeySourceActions(String id, List<KeySourceAction> actions) {
            this.id = id;
            this.actions = actions;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public List<KeySourceAction> getActions() {
            return actions;
        }
    }

    /**
     * Repräsentiert eine "pointer"-Aktion.
     */
    class PointerSourceActions implements SourceActions {
        private final String type = "pointer";
        private final String id;
        private final List<PointerSourceAction> actions;
        private final PointerParameters parameters; // Optional

        public PointerSourceActions(String id, PointerParameters parameters, List<PointerSourceAction> actions) {
            this.id = id;
            this.parameters = parameters;
            this.actions = actions;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public PointerParameters getParameters() {
            return parameters;
        }

        public List<PointerSourceAction> getActions() {
            return actions;
        }

        public static class PointerParameters {
            private final PointerType type;

            public PointerParameters() {
                this.type = PointerType.MOUSE;
            }

            public PointerParameters(PointerType type) {
                this.type = type;
            }

            public PointerType getType() {
                return type;
            }

            public enum PointerType implements EnumWrapper {
                MOUSE("mouse"),
                PEN("pen"),
                TOUCH("touch");

                private final String value;

                PointerType(String value) {
                    this.value = value;
                }

                @Override // confirmed
                public String value() {
                    return value;
                }
            }
        }
    }

    /**
     * Repräsentiert eine "wheel"-Aktion.
     */
    class WheelSourceActions implements SourceActions {
        private final String type = "wheel";
        private final String id;
        private final List<WheelSourceAction> actions;

        public WheelSourceActions(String id, List<WheelSourceAction> actions) {
            this.id = id;
            this.actions = actions;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public List<WheelSourceAction> getActions() {
            return actions;
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


}



