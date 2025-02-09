package wd4j.impl.webdriver.type.script;

public interface PrimitiveProtocolValue<T> {
    String getType();

    class UndefinedValue implements PrimitiveProtocolValue<Void> {
        private final String type = "undefined";

        @Override
        public String getType() {
            return type;
        }
    }

    class NullValue implements PrimitiveProtocolValue<Void> {
        private final String type = "null";

        @Override
        public String getType() {
            return type;
        }
    }

    class StringValue implements PrimitiveProtocolValue<String> {
        private final String type = "string";
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class NumberValue implements PrimitiveProtocolValue<String> {
        private final String type = "number";
        private final String value; // Cannot use Number (as defined) because of special values

        public NumberValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    class BooleanValue implements PrimitiveProtocolValue<Boolean> {
        private final String type = "boolean";
        private final Boolean value;

        public BooleanValue(Boolean value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public Boolean getValue() {
            return value;
        }
    }

    class BigIntValue implements PrimitiveProtocolValue<String> {
        private final String type = "bigint";
        private final String value;

        public BigIntValue(String value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

    enum SpecialNumber {
        NAN("NaN"),
        NEGATIVE_ZERO("-0"),
        INFINITY("Infinity"),
        NEGATIVE_INFINITY("-Infinity");

        private final String value;

        SpecialNumber(String value) {
            this.value = value;
        }
    }
}
