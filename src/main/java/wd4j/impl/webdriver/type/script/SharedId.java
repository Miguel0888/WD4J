package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class SharedId implements StringWrapper {
    public final String value;

    public SharedId(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
