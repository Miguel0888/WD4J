package wd4j.impl.module.type;

public class SessionUserPromptHandler {
    private final String contextId;
    private final String handlerType;

    public SessionUserPromptHandler(String contextId, String handlerType) {
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        if (handlerType == null || handlerType.isEmpty()) {
            throw new IllegalArgumentException("Handler type must not be null or empty.");
        }
        this.contextId = contextId;
        this.handlerType = handlerType;
    }

    public String getContextId() {
        return contextId;
    }

    public String getHandlerType() {
        return handlerType;
    }
}