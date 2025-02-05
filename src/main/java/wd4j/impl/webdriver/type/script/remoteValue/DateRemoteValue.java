package wd4j.impl.webdriver.type.script.remoteValue;

import wd4j.impl.webdriver.type.script.RemoteValue;

public class DateRemoteValue extends RemoteValue {
    private final String dateValue;

    public DateRemoteValue(String handle, String internalId, String dateValue) {
        super("date", handle, internalId);
        this.dateValue = dateValue;
    }

    public String getDateValue() {
        return dateValue;
    }

    @Override
    public String toString() {
        return "DateRemoteValue{" +
                "type='" + getType() + '\'' +
                ", handle='" + getHandle() + '\'' +
                ", internalId='" + getInternalId() + '\'' +
                ", dateValue='" + dateValue + '\'' +
                '}';
    }
}
