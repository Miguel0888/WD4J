package de.bund.zrb.event;

import de.bund.zrb.model.TestAction;

/** Event: Eine TestAction wurde im Baum (in-place) geändert und UI-Komponenten sollen sich aktualisieren. */
public class TestActionUpdatedEvent implements ApplicationEvent<TestAction> {
    private final TestAction payload;
    public TestActionUpdatedEvent(TestAction payload) { this.payload = payload; }
    @Override
    public TestAction getPayload() { return payload; }
}
