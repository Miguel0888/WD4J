package de.bund.zrb.support;

import com.google.gson.JsonObject;
import de.bund.zrb.api.EventMapper;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDLogEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;

/**
 * Mappt ein eingehendes BiDi-Event ausschließlich auf die passenden
 * WD-Event-DTOs. Jegliches Mapping in Playwright-Objekte passiert
 * nun in der Page/Frame-Schicht über Adapter.
 */
public class EventMapperImpl implements EventMapper {

    // Falls der alte Konstruktor beibehalten werden muss:
    // private final BrowserImpl browser;
    // public EventMapperImpl(BrowserImpl browser) { this.browser = browser; }

    public EventMapperImpl() {}

    @Override
    public Object apply(String eventType, JsonObject json) {
        WDEventNames eventMapping = WDEventNames.fromName(eventType);
        if (eventMapping == null) return null;

        System.out.println("[DEBUG] Mapping event: " + eventType + " to: " + eventMapping);

        switch (eventMapping) {
            // ── Browsing Context Events ────────────────────────────────────────────────
            case CONTEXT_CREATED:
                return new WDBrowsingContextEvent.Created(json);
            case CONTEXT_DESTROYED:
                return new WDBrowsingContextEvent.Destroyed(json);

            case NAVIGATION_STARTED:
                return new WDBrowsingContextEvent.NavigationStarted(json);
            case NAVIGATION_COMMITTED:
                return new WDBrowsingContextEvent.NavigationCommitted(json);
            case NAVIGATION_FAILED:
                return new WDBrowsingContextEvent.NavigationFailed(json);
            case NAVIGATION_ABORTED:
                return new WDBrowsingContextEvent.NavigationAborted(json);
            case FRAGMENT_NAVIGATED:
                return new WDBrowsingContextEvent.FragmentNavigated(json);
            case HISTORY_UPDATED:
                return new WDBrowsingContextEvent.HistoryUpdated(json);

            case DOM_CONTENT_LOADED:
                return new WDBrowsingContextEvent.DomContentLoaded(json);
            case LOAD:
                return new WDBrowsingContextEvent.Load(json);

            case DOWNLOAD_WILL_BEGIN:
                return new WDBrowsingContextEvent.DownloadWillBegin(json);

            case USER_PROMPT_OPENED:
                return new WDBrowsingContextEvent.UserPromptOpened(json);
            case USER_PROMPT_CLOSED:
                return new WDBrowsingContextEvent.UserPromptClosed(json);

            // ── Network Events ────────────────────────────────────────────────────────
            case AUTH_REQUIRED:
                return new WDNetworkEvent.AuthRequired(json);
            case BEFORE_REQUEST_SENT:
                return new WDNetworkEvent.BeforeRequestSent(json);
            case FETCH_ERROR:
                return new WDNetworkEvent.FetchError(json);
            case RESPONSE_STARTED:
                return new WDNetworkEvent.ResponseStarted(json);
            case RESPONSE_COMPLETED:
                return new WDNetworkEvent.ResponseCompleted(json);

            // ── Script Events ─────────────────────────────────────────────────────────
            case REALM_CREATED:
                return new WDScriptEvent.RealmCreated(json);
            case REALM_DESTROYED:
                return new WDScriptEvent.RealmDestroyed(json);
            case MESSAGE:
                return new WDScriptEvent.Message(json); // (Channels ggf. später gesondert)

            // ── Log Events ────────────────────────────────────────────────────────────
            case ENTRY_ADDED:
                return new WDLogEvent.EntryAdded(json);

            // ── WebSocket (Page-WebSocket) ────────────────────────────────────────────
            // Nicht durch BiDi abgedeckt – bei Bedarf JS-Instrumentation.
            default:
                return null;
        }
    }
}
