package wd4j.impl.module;

import wd4j.impl.WebSocketImpl;
import wd4j.impl.generic.Module;

public class LogService implements Module {

    private final WebSocketImpl webSocketImpl;

    public LogService(WebSocketImpl webSocketImpl) {
        this.webSocketImpl = webSocketImpl;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handlers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Commands
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}