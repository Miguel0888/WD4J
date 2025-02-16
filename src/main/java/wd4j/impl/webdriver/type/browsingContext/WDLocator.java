package wd4j.impl.webdriver.type.browsingContext;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import wd4j.impl.webdriver.mapping.EnumWrapper;

import java.lang.reflect.Type;

@JsonAdapter(WDLocator.LocatorAdapter.class) // 🔥 Automatische JSON-Konvertierung
public interface WDLocator<T> {
    String getType();
    T getValue();

    // 🔥 **INNERE KLASSE für JSON-Deserialisierung**
    class LocatorAdapter implements JsonDeserializer<WDLocator<?>> {
        @Override
        public WDLocator<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Missing 'type' field in Locator JSON");
            }

            String type = jsonObject.get("type").getAsString();

            switch (type) {
                case "accessibility":
                    return context.deserialize(jsonObject, AccessibilityWDLocator.class);
                case "context":
                    return context.deserialize(jsonObject, ContextWDLocator.class);
                case "css":
                    return context.deserialize(jsonObject, CssWDLocator.class);
                case "innerText":
                    return context.deserialize(jsonObject, InnerTextWDLocator.class);
                case "xpath":
                    return context.deserialize(jsonObject, XPathWDLocator.class);
                default:
                    throw new JsonParseException("Unknown Locator type: " + type);
            }
        }
    }

   class AccessibilityWDLocator implements WDLocator<AccessibilityWDLocator.Value> {
       private final String type = "accessibility";
       private final Value value;

       public AccessibilityWDLocator(Value value) {
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

   class ContextWDLocator implements WDLocator<ContextWDLocator.Value> {
       private final String type = "context";
       private final Value value;

       public ContextWDLocator(Value value) {
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

   class CssWDLocator implements WDLocator<String> {
       private final String type = "css";
       private final String value;

       public CssWDLocator(String value) {
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

   class InnerTextWDLocator implements WDLocator<String> {
       private final String type = "innerText";
       private final String value;
       private final Boolean ignoreCase; // optional
       private final MatchType matchType; // optional
       private final Character maxDepth; // optional

       public InnerTextWDLocator(String value) {
           this.value = value;
           this.ignoreCase = null;
           this.matchType = null;
           this.maxDepth = null;
       }

       public InnerTextWDLocator(String value, boolean ignoreCase, MatchType matchType, char maxDepth) {
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

       public Character getMaxDepth() {
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

   class XPathWDLocator implements WDLocator<String> {
       private final String type = "xpath";
       private final String value;

       public XPathWDLocator(String value) {
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
