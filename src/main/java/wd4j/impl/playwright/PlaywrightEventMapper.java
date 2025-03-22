package wd4j.impl.playwright;

import com.google.gson.JsonObject;
import wd4j.impl.playwright.event.*;
import wd4j.impl.websocket.EventMapper;
import wd4j.impl.dto.event.*;
import wd4j.impl.websocket.WDEventNames;

public class PlaywrightEventMapper implements EventMapper {

    @Override
    public Object apply(String eventType, JsonObject json) {
        WDEventNames eventMapping = WDEventNames.fromName(eventType);
        if (eventMapping == null) {
            return null; // Falls das Event nicht bekannt ist, nichts zurückgeben
        }
        System.out.println("[DEBUG] Mapping event: " + eventType + " to: " + eventMapping);
        switch (eventMapping) {
            // 🔹 Browsing Context Events
            case CONTEXT_CREATED:
                return new PageImpl(new WDBrowsingContextEvent.Created(json));
            case CONTEXT_DESTROYED:
                return new PageImpl(new WDBrowsingContextEvent.Destroyed(json));

            case NAVIGATION_STARTED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationStarted(json));
            case NAVIGATION_COMMITTED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationCommitted(json)); // ToDo: Correct?
            case NAVIGATION_FAILED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationFailed(json));
            case NAVIGATION_ABORTED:
                return new FrameImpl(new WDBrowsingContextEvent.NavigationAborted(json));
            case FRAGMENT_NAVIGATED:
                return new FrameImpl(new WDBrowsingContextEvent.FragmentNavigated(json));
            case HISTORY_UPDATED:
                return new FrameImpl(new WDBrowsingContextEvent.HistoryUpdated(json));

            case DOM_CONTENT_LOADED:
                return new PageImpl(new WDBrowsingContextEvent.DomContentLoaded(json));
            case LOAD:
                return new PageImpl(new WDBrowsingContextEvent.Load(json));

            case DOWNLOAD_WILL_BEGIN:
                return new DownloadImpl(new WDBrowsingContextEvent.DownloadWillBegin(json));

            case USER_PROMPT_OPENED:
                return new DialogImpl(new WDBrowsingContextEvent.UserPromptOpened(json));
            case USER_PROMPT_CLOSED:
                return new DialogImpl(new WDBrowsingContextEvent.UserPromptClosed(json));

            // 🔹 Network Events
            case AUTH_REQUIRED: //
                return new RequestImpl(new WDNetworkEvent.AuthRequired(json)); // Besser als AuthChallenge // ToDo: Correct?

            case BEFORE_REQUEST_SENT:
                return new RequestImpl(new WDNetworkEvent.BeforeRequestSent(json));

            case FETCH_ERROR:
                return new ResponseImpl(new WDNetworkEvent.FetchError(json), null);

            case RESPONSE_STARTED:
                return new ResponseImpl(new WDNetworkEvent.ResponseStarted(json), null);
            case RESPONSE_COMPLETED:
                return new ResponseImpl(new WDNetworkEvent.ResponseCompleted(json), null);

            // 🔹 Script Events
            case REALM_CREATED:
                return new WorkerImpl(new WDScriptEvent.RealmCreated(json));
            case REALM_DESTROYED:
                return new WorkerImpl(new WDScriptEvent.RealmDestroyed(json));
            case MESSAGE://ToDo: This is not quite correct, since message is used for "Channels" (they have a ChannelID)
                return new WDScriptEvent.Message(json); // Might not be relevant for PlayWright!

            // 🔹 Log Events
            case ENTRY_ADDED:
                return new ConsoleMessageImpl(new WDLogEvent.EntryAdded(json));

            // 🔹 WebSocket Events
//            case WEBSOCKET_CLOSED:
//                return new WebSocketImpl(new WDNetworkEvent.WebSocketClosed(json));
//
//            case WEBSOCKET_FRAME_SENT:
//                return new WebSocketImpl.WebSocketFrameImpl(new WDNetworkEvent.WebSocketFrameSent(json));
//
//            case WEBSOCKET_FRAME_RECEIVED:
//                return new WebSocketImpl.WebSocketFrameImpl(new WDNetworkEvent.WebSocketFrameReceived(json));

            default:
                return null;
        }
    }
}
