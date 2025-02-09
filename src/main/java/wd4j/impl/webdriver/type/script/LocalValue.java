package wd4j.impl.webdriver.type.script;

import java.util.List;
import java.util.Map;

public interface LocalValue<T> {
    String getType();
    T getValue();

    class ArrayLocalValue implements LocalValue<List<LocalValue<?>>> {
        private final String type = "array";
        private final List<LocalValue<?>> value;

        public ArrayLocalValue(List<LocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public List<LocalValue<?>> getValue() {
            return value;
        }
    }

    class DateLocalValue implements LocalValue<String> {
        private final String type = "date";
        private final String value;

        public DateLocalValue(String value) {
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

    class MapLocalValue implements LocalValue<Map<LocalValue<?>, LocalValue<?>>> {
        private final String type = "map";
        private final Map<LocalValue<?>, LocalValue<?>> value;

        public MapLocalValue(Map<LocalValue<?>, LocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Map<LocalValue<?>, LocalValue<?>> getValue() {
            return value;
        }
    }

    class ObjectLocalValue implements LocalValue<Map<LocalValue<?>, LocalValue<?>>> {
        private final String type = "object";
        private final Map<LocalValue<?>, LocalValue<?>> value;

        public ObjectLocalValue(Map<LocalValue<?>, LocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Map<LocalValue<?>, LocalValue<?>> getValue() {
            return value;
        }
    }

    class RegExpValue {
        private final String pattern;
        private final String flags; // Optional

        public RegExpValue(String pattern) {
            this(pattern, null);
        }

        public RegExpValue(String pattern, String flags) {
            this.pattern = pattern;
            this.flags = flags;
        }

        public String getPattern() {
            return pattern;
        }

        public String getFlags() {
            return flags;
        }
    }

    class RegExpLocalValue implements LocalValue<RegExpValue> {
        private final String type = "regexp";
        private final RegExpValue value;

        public RegExpLocalValue(RegExpValue value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public RegExpValue getValue() {
            return value;
        }
    }

    class SetLocalValue implements LocalValue<List<LocalValue<?>>> {
        private final String type = "set";
        private final List<LocalValue<?>> value;

        public SetLocalValue(List<LocalValue<?>> value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public List<LocalValue<?>> getValue() {
            return value;
        }
    }
}