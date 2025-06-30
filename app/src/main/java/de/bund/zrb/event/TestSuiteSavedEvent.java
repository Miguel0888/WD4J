package de.bund.zrb.event;

public class TestSuiteSavedEvent implements ApplicationEvent<String> {
    private final String suiteName;

    public TestSuiteSavedEvent(String suiteName) {
        this.suiteName = suiteName;
    }

    @Override
    public String getPayload() {
        return suiteName;
    }
}
