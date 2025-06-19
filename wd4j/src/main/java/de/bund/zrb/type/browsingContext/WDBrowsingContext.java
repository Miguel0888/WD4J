package de.bund.zrb.type.browsingContext;

import de.bund.zrb.api.markerInterfaces.WDType;
import de.bund.zrb.support.mapping.StringWrapper;

import java.util.Objects;

public class WDBrowsingContext implements WDType<WDBrowsingContext>, StringWrapper {
    private final String value;

    public WDBrowsingContext(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WDBrowsingContext context = (WDBrowsingContext) o;
        return Objects.equals(value, context.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
