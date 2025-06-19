package de.bund.zrb.webdriver.type.browsingContext;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import de.bund.zrb.webdriver.mapping.EnumWrapper;

import java.lang.reflect.Type;

@JsonAdapter(WDLocator.LocatorAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDLocator<T> {
    String getType();
    T getValue();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LocatorAdapter implements JsonDeserializer<WDLocator<?>>, JsonSerializer<WDLocator<?>> {
        @Override
        public WDLocator<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in Locator JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "accessibility":
                    return context.deserialize(jsonObject, AccessibilityLocator.class);
                case "context":
                    return context.deserialize(jsonObject, ContextLocator.class);
                case "css":
                    return context.deserialize(jsonObject, CssLocator.class);
                case "innerText":
                    return context.deserialize(jsonObject, InnerTextLocator.class);
                case "xpath":
                    return context.deserialize(jsonObject, XPathLocator.class);
                default:
                    throw new JsonParseException("Unknown Locator type: " + type);
            }
        }

        @Override
        public JsonElement serialize(WDLocator src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }
    }

   class AccessibilityLocator implements WDLocator<AccessibilityLocator.Value> {
       private final String type = "accessibility";
       private final Value value;

       public AccessibilityLocator(Value value) {
           this.value = value;
       }

       public String getType() {
           return type;
       }

       public Value getValue() {
           return value;
       }

       public static class Value {
           private final String name;
           private final String role;

           public Value(String name, String role) {
               this.name = name;
               this.role = role;
           }

           public String getName() {
               return name;
           }

           public String getRole() {
               return role;
           }
       }
   }

   class ContextLocator implements WDLocator<ContextLocator.Value> {
       private final String type = "context";
       private final Value value;

       public ContextLocator(Value value) {
           this.value = value;
       }

       @Override
       public String getType() {
           return type;
       }

       @Override
       public Value getValue() {
           return value;
       }

       public static class Value {
           private final String contextId;

           public Value(String contextId) {
               this.contextId = contextId;
           }

           public String getContextId() {
               return contextId;
           }
       }
   }

   class CssLocator implements WDLocator<String> {
       private final String type = "css";
       private final String value;

       public CssLocator(String value) {
           this.value = value;
       }


       @Override
       public String getType() {
           return type;
       }

       @Override
       public String getValue() {
           return value;
       }
   }

   class InnerTextLocator implements WDLocator<String> {
       private final String type = "innerText";
       private final String value;
       private final Boolean ignoreCase; // optional
       private final MatchType matchType; // optional
       private final Long maxDepth; // optional

       public InnerTextLocator(String value) {
           this.value = value;
           this.ignoreCase = null;
           this.matchType = null;
           this.maxDepth = null;
       }

       public InnerTextLocator(String value, boolean ignoreCase, MatchType matchType, Long maxDepth) {
           this.value = value;
           this.ignoreCase = ignoreCase;
           this.matchType = matchType;
           this.maxDepth = maxDepth;
       }

       @Override
       public String getType() {
           return type;
       }

       @Override
       public String getValue() {
           return value;
       }

       public Boolean isIgnoreCase() {
           return ignoreCase;
       }

       public MatchType getMatchType() {
           return matchType;
       }

       public Long getMaxDepth() {
           return maxDepth;
       }

       public enum MatchType implements EnumWrapper {
           FULL("full"),
           PARTIAL("partial");

           private final String value;

           MatchType(String value) {
               this.value = value;
           }

           @Override // confirmed
           public String value() {
               return value;
           }
       }
   }

   class XPathLocator implements WDLocator<String> {
       private final String type = "xpath";
       private final String value;

       public XPathLocator(String value) {
           this.value = value;
       }


       @Override
       public String getType() {
           return type;
       }

       @Override
       public String getValue() {
           return value;
       }
   }
}
