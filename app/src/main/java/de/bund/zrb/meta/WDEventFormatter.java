package de.bund.zrb.meta;

import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDLogEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.browsingContext.WDInfo;
import de.bund.zrb.type.browsingContext.WDNavigationInfo;
import de.bund.zrb.type.log.WDLogEntry;
import de.bund.zrb.type.network.WDBaseParameters;
import de.bund.zrb.type.network.WDRequestData;
import de.bund.zrb.type.network.WDResponseData;
import de.bund.zrb.type.script.WDSource;
import de.bund.zrb.websocket.WDEvent;

/**
 * Format typed WebDriver BiDi events into concise one-line strings for the meta drawer.
 * Keep it robust: rely only on public getters; do not use reflection.
 */
public final class WDEventFormatter {

    private WDEventFormatter() {
        // Prevent instantiation
    }

    /** Entry point: format any typed event. */
    public static String format(WDEvent<?> event) {
        if (event == null) return "";

        // Network
        if (event instanceof WDNetworkEvent.BeforeRequestSent) {
            return formatBeforeRequestSent((WDNetworkEvent.BeforeRequestSent) event);
        }
        if (event instanceof WDNetworkEvent.ResponseStarted) {
            return formatResponseStarted((WDNetworkEvent.ResponseStarted) event);
        }
        if (event instanceof WDNetworkEvent.ResponseCompleted) {
            return formatResponseCompleted((WDNetworkEvent.ResponseCompleted) event);
        }
        if (event instanceof WDNetworkEvent.FetchError) {
            return formatFetchError((WDNetworkEvent.FetchError) event);
        }
        if (event instanceof WDNetworkEvent.AuthRequired) {
            return formatAuthRequired((WDNetworkEvent.AuthRequired) event);
        }

        // BrowsingContext
        if (event instanceof WDBrowsingContextEvent.Created) {
            return formatContextCreated((WDBrowsingContextEvent.Created) event);
        }
        if (event instanceof WDBrowsingContextEvent.Destroyed) {
            return formatContextDestroyed((WDBrowsingContextEvent.Destroyed) event);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationStarted) {
            return formatNavGeneric((WDBrowsingContextEvent.NavigationStarted) event);
        }
        if (event instanceof WDBrowsingContextEvent.FragmentNavigated) {
            return formatNavGeneric((WDBrowsingContextEvent.FragmentNavigated) event);
        }
        if (event instanceof WDBrowsingContextEvent.DomContentLoaded) {
            return formatNavGeneric((WDBrowsingContextEvent.DomContentLoaded) event);
        }
        if (event instanceof WDBrowsingContextEvent.Load) {
            return formatNavGeneric((WDBrowsingContextEvent.Load) event);
        }
        if (event instanceof WDBrowsingContextEvent.DownloadWillBegin) {
            return formatNavGeneric((WDBrowsingContextEvent.DownloadWillBegin) event);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationAborted) {
            return formatNavGeneric((WDBrowsingContextEvent.NavigationAborted) event);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationCommitted) {
            return formatNavGeneric((WDBrowsingContextEvent.NavigationCommitted) event);
        }
        if (event instanceof WDBrowsingContextEvent.NavigationFailed) {
            return formatNavGeneric((WDBrowsingContextEvent.NavigationFailed) event);
        }
        if (event instanceof WDBrowsingContextEvent.HistoryUpdated) {
            return formatHistoryUpdated((WDBrowsingContextEvent.HistoryUpdated) event);
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptOpened) {
            return formatUserPromptOpened((WDBrowsingContextEvent.UserPromptOpened) event);
        }
        if (event instanceof WDBrowsingContextEvent.UserPromptClosed) {
            return formatUserPromptClosed((WDBrowsingContextEvent.UserPromptClosed) event);
        }

        // Log
        if (event instanceof WDLogEvent.EntryAdded) {
            return formatLogEntryAdded((WDLogEvent.EntryAdded) event);
        }

        // Script
        if (event instanceof WDScriptEvent.MessageWD) {
            return formatScriptMessage((WDScriptEvent.MessageWD) event);
        }
        if (event instanceof WDScriptEvent.RealmCreated) {
            return "[script] " + safe(event.getMethod()) + " realmCreated";
        }
        if (event instanceof WDScriptEvent.RealmDestroyed) {
            return "[script] " + safe(event.getMethod()) + " realmDestroyed";
        }

        // Fallback
        return safe(event.getMethod());
    }

    // region ---------- Network formatters ----------

    private static String formatBeforeRequestSent(WDNetworkEvent.BeforeRequestSent e) {
        WDNetworkEvent.BeforeRequestSent.BeforeRequestSentParametersWD p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContextId() : null);
        String method = reqMethod(p);
        String url = reqUrl(p);
        return "[network] beforeRequestSent " + method + " " + url + ctxPart(ctx);
    }

    private static String formatResponseStarted(WDNetworkEvent.ResponseStarted e) {
        WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContextId() : null);
        String url = resUrl(p);
        String status = resStatus(p);
        return "[network] responseStarted " + status + " " + url + ctxPart(ctx);
    }

    private static String formatResponseCompleted(WDNetworkEvent.ResponseCompleted e) {
        WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContextId() : null);
        String url = resUrl(p);
        String status = resStatus(p);
        return "[network] responseCompleted " + status + " " + url + ctxPart(ctx);
    }

    private static String formatFetchError(WDNetworkEvent.FetchError e) {
        WDNetworkEvent.FetchError.FetchErrorParametersWD p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContextId() : null);
        String url = reqUrl(p);
        String err = p != null ? safe(p.getErrorText()) : "";
        return "[network] fetchError " + url + (err.isEmpty() ? "" : " — " + err) + ctxPart(ctx);
    }

    private static String formatAuthRequired(WDNetworkEvent.AuthRequired e) {
        WDNetworkEvent.AuthRequired.AuthRequiredParametersWD p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContextId() : null);
        String url = reqUrl(p);
        String status = resStatus(p);
        return "[network] authRequired " + (status.isEmpty() ? "" : status + " ") + url + ctxPart(ctx);
    }

    // endregion

    // region ---------- BrowsingContext formatters ----------

    private static String formatContextCreated(WDBrowsingContextEvent.Created e) {
        WDInfo p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContext() : null);
        return "[browsingContext] contextCreated" + ctxPart(ctx);
    }

    private static String formatContextDestroyed(WDBrowsingContextEvent.Destroyed e) {
        WDInfo p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContext() : null);
        return "[browsingContext] contextDestroyed" + ctxPart(ctx);
    }

    private static String formatNavGeneric(WDEvent<WDNavigationInfo> e) {
        WDNavigationInfo p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContext() : null);
        return "[browsingContext] " + safe(e.getMethod()) + ctxPart(ctx);
    }

    private static String formatHistoryUpdated(WDBrowsingContextEvent.HistoryUpdated e) {
        WDBrowsingContextEvent.HistoryUpdated.HistoryUpdatedParameters p = e.getParams();
        String ctx = contextAbbr(p != null ? p.getContext() : null);
        String url = p != null ? safe(p.getUrl()) : "";
        return "[browsingContext] historyUpdated " + url + ctxPart(ctx);
    }

    private static String formatUserPromptOpened(WDBrowsingContextEvent.UserPromptOpened e) {
        WDBrowsingContextEvent.UserPromptOpened.UserPromptOpenedParameters p = e.getParams();
        String ctx = idAbbr(p != null ? p.getContext() : null);
        String msg = p != null ? safe(p.getMessage()) : "";
        String type = p != null && p.getType() != null ? safe(p.getType().value()) : "";
        return "[browsingContext] userPromptOpened " + (type.isEmpty() ? "" : type + " ") +
                (msg.isEmpty() ? "" : "\"" + msg + "\" ") + ctxPart(ctx);
    }

    private static String formatUserPromptClosed(WDBrowsingContextEvent.UserPromptClosed e) {
        WDBrowsingContextEvent.UserPromptClosed.UserPromptClosedParameters p = e.getParams();
        String ctx = idAbbr(p != null ? p.getContext() : null);
        String type = p != null && p.getType() != null ? safe(p.getType().value()) : "";
        String res = p != null ? (p.isAccepted() ? "accepted" : "dismissed") : "";
        return "[browsingContext] userPromptClosed " + (type.isEmpty() ? "" : type + " ") + res + ctxPart(ctx);
    }

    // endregion

    // region ---------- Log formatters ----------

    private static String formatLogEntryAdded(WDLogEvent.EntryAdded e) {
        WDLogEntry entry = e.getParams();
        if (entry instanceof WDLogEntry.BaseWDLogEntry) {
            WDLogEntry.BaseWDLogEntry base = (WDLogEntry.BaseWDLogEntry) entry;
            String level = base.getLevel() != null ? safe(base.getLevel().value()) : "";
            String text = safe(base.getText());
            String ctx = contextAbbr(base.getSource() != null ? base.getSource().getContext() : null);
            return "[log] entryAdded " + (level.isEmpty() ? "" : level + " ") + text + ctxPart(ctx);
        }
        // Fallback for unexpected implementations
        return "[log] entryAdded";
    }

    // endregion

    // region ---------- Script formatters ----------

    private static String formatScriptMessage(WDScriptEvent.MessageWD e) {
        WDScriptEvent.MessageWD.MessageParameters p = e.getParams();
        WDSource src = p != null ? p.getSource() : null;
        String ctx = contextAbbr(src != null ? src.getContext() : null);
        // Keep message compact; do not serialize data payloads here
        return "[script] message" + ctxPart(ctx);
    }

    // endregion

    // region ---------- Small helpers ----------

    /** Build optional context suffix like " (ctx: abcd1234)". */
    private static String ctxPart(String ctxAbbr) {
        return (ctxAbbr == null || ctxAbbr.length() == 0) ? "" : " (ctx: " + ctxAbbr + ")";
    }

    /** Return short id (first 8 chars) for plain String ids. */
    private static String idAbbr(String id) {
        if (id == null) return "";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    /** Return short id for WDBrowsingContext via value(). */
    private static String contextAbbr(WDBrowsingContext ctx) {
        if (ctx == null) return "";
        String id = safe(ctx.value());
        return idAbbr(id);
    }

    /** Extract request URL, null-safe. */
    private static String reqUrl(WDBaseParameters p) {
        if (p == null) return "";
        WDRequestData r = p.getRequest();
        return r != null ? safe(r.getUrl()) : "";
    }

    /** Extract request method, null-safe. */
    private static String reqMethod(WDBaseParameters p) {
        if (p == null) return "";
        WDRequestData r = p.getRequest();
        return r != null ? safe(r.getMethod()) : "";
    }

    /** Extract response URL, null-safe. */
    private static String resUrl(WDBaseParameters p) {
        if (p == null) return "";
        WDResponseData r = extractResponse(p);
        return r != null ? safe(r.getUrl()) : "";
    }

    /** Extract status and statusText, compact form like "302 Found". */
    private static String resStatus(WDBaseParameters p) {
        WDResponseData r = extractResponse(p);
        if (r == null) return "";
        String code = String.valueOf(r.getStatus());
        String text = safe(r.getStatusText());
        return (text.isEmpty() ? code : code + " " + text);
    }

    /** Extract WDResponseData from any known network params. */
    private static WDResponseData extractResponse(WDBaseParameters p) {
        if (p == null) return null;
        if (p instanceof WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD) {
            return ((WDNetworkEvent.ResponseStarted.ResponseStartedParametersWD) p).getResponse();
        }
        if (p instanceof WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD) {
            return ((WDNetworkEvent.ResponseCompleted.ResponseCompletedParametersWD) p).getResponse();
        }
        if (p instanceof WDNetworkEvent.AuthRequired.AuthRequiredParametersWD) {
            return ((WDNetworkEvent.AuthRequired.AuthRequiredParametersWD) p).getResponse();
        }
        // Other network params either do not expose a response or we do not need it
        return null;
    }

    /** Return a non-null string. */
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // endregion
}
