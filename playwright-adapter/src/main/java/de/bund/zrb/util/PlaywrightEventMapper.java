package de.bund.zrb.util;

import com.google.gson.JsonObject;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.PageImpl;
import de.bund.zrb.event.*;
import de.bund.zrb.api.EventMapper;
import de.bund.zrb.websocket.WDEventNames;

public class PlaywrightEventMapper implements EventMapper {

    private final BrowserImpl browser;

    public PlaywrightEventMapper(BrowserImpl browser) {
        this.browser = browser;
    }

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
                return new PageImpl(browser, new WDBrowsingContextEvent.Created(json));
            case CONTEXT_DESTROYED:
                return new PageImpl(browser, new WDBrowsingContextEvent.Destroyed(json));

            case NAVIGATION_STARTED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.NavigationStarted(json));
            case NAVIGATION_COMMITTED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.NavigationCommitted(json)); // ToDo: Correct?
            case NAVIGATION_FAILED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.NavigationFailed(json));
            case NAVIGATION_ABORTED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.NavigationAborted(json));
            case FRAGMENT_NAVIGATED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.FragmentNavigated(json));
            case HISTORY_UPDATED:
                return new FrameImpl(browser, new WDBrowsingContextEvent.HistoryUpdated(json));

            case DOM_CONTENT_LOADED:
                return new PageImpl(browser, new WDBrowsingContextEvent.DomContentLoaded(json));
            case LOAD:
                return new PageImpl(browser, new WDBrowsingContextEvent.Load(json));

            case DOWNLOAD_WILL_BEGIN:
                return new DownloadImpl(browser, new WDBrowsingContextEvent.DownloadWillBegin(json));

            case USER_PROMPT_OPENED:
                return new DialogImpl(browser, new WDBrowsingContextEvent.UserPromptOpened(json));
            case USER_PROMPT_CLOSED:
                return new DialogImpl(browser, new WDBrowsingContextEvent.UserPromptClosed(json));

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
                return new ConsoleMessageImpl(browser, new WDLogEvent.EntryAdded(json));

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
