package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public interface Locator<T> {
   String getType();
   T getValue();

   class AccessibilityLocator implements Locator<AccessibilityLocator.Value> {
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

   class ContextLocator implements Locator<ContextLocator.Value> {
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

   class CssLocator implements Locator<String> {
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

   class InnerTextLocator implements Locator<String> {
       private final String type = "innerText";
       private final String value;
       private final Boolean ignoreCase; // optional
       private final MatchType matchType; // optional
       private final Character maxDepth; // optional

       public InnerTextLocator(String value) {
           this.value = value;
           this.ignoreCase = null;
           this.matchType = null;
           this.maxDepth = null;
       }

       public InnerTextLocator(String value, boolean ignoreCase, MatchType matchType, char maxDepth) {
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

   class XPathLocator implements Locator<String> {
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
