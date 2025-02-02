package wd4j.impl.module.event;

import java.util.Map;

public class NetworkEvent {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Events (Classes)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public interface ResponseStarted {
        String requestId();
        String context();
        String navigation();
        String redirectCount();
        Map<String, Object> response();
    }

    public interface ResponseCompleted {
        String requestId();
        String context();
        String navigation();
        Map<String, Object> response();
    }
}