package de.bund.zrb;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import de.bund.zrb.command.request.parameters.browsingContext.CaptureScreenshotParameters;
import de.bund.zrb.command.request.parameters.browsingContext.CreateType;
import de.bund.zrb.event.*;
import de.bund.zrb.ext.WDPageExtension;
import de.bund.zrb.ext.WDPageExtensionSupport;
import de.bund.zrb.support.ScreenshotPreprocessor;
import de.bund.zrb.type.browsingContext.WDNavigationInfo;
import de.bund.zrb.type.script.*;
import de.bund.zrb.support.PlaywrightResponse;
import de.bund.zrb.support.ScriptHelper;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.type.browser.WDClientWindow;
import de.bund.zrb.type.browsingContext.WDInfo;
import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.util.AdapterLocatorFactory;
import de.bund.zrb.util.LocatorType;
import de.bund.zrb.util.WebDriverUtil;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.support.JsonToPlaywrightMapper;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PageImpl implements Page, WDPageExtension {
    private final WDBrowsingContext browsingContext; // aka. browsing context or navigable in WebDriver BiDi
    private final WDUserContext userContextId; // aka. simply as contextId in CDP - default is "default"
    private boolean isClosed;
    private String url;

    private final BrowserImpl browser;
    private final WebDriver webDriver;

    private final Mouse mouse;
    private final Keyboard keyboard;

    // ToDo: Not supported yet, just for testing: Firefox does not remember the id, only accepts event + contextId
    private WDSubscription consoleMessageSubscription;

    private List<WDScriptResult.AddPreloadScriptResult> addPreloadScriptResults = new ArrayList<>();

    private final WDPageExtensionSupport extension = new WDPageExtensionSupport(this);

    // --- Adapter-Mapping je Eventtyp (extern -> interner Adapter) ---
    private final java.util.Map<java.util.function.Consumer<Response>,      java.util.function.Consumer<Object>> respAdapters        = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Request>,       java.util.function.Consumer<Object>> reqBeforeAdapters   = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Request>,       java.util.function.Consumer<Object>> reqFailedAdapters   = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Request>,       java.util.function.Consumer<Object>> reqFinishedAdapters = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.Map<java.util.function.Consumer<ConsoleMessage>, java.util.function.Consumer<Object>> consoleAdapters    = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Dialog>,         java.util.function.Consumer<Object>> dialogAdapters     = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Page>,           java.util.function.Consumer<Object>> domLoadedAdapters  = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Page>,           java.util.function.Consumer<Object>> loadAdapters       = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Page>,           java.util.function.Consumer<Object>> closeAdapters      = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.Map<java.util.function.Consumer<Worker>,         java.util.function.Consumer<Object>> workerAdapters     = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<Download>,       java.util.function.Consumer<Object>> downloadAdapters   = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.function.Consumer<FileChooser>,       java.util.function.Consumer<Object>> fileChooserAdapters   = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<Consumer<Page>,  Consumer<Object>> crashAdapters         = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Consumer<Page>,  Consumer<Object>> popupAdapters         = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<Consumer<Frame>, Consumer<Object>> frameAttachedAdapters = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Consumer<Frame>, Consumer<Object>> frameDetachedAdapters = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Consumer<Frame>, Consumer<Object>> frameNavigatedAdapters= new java.util.concurrent.ConcurrentHashMap<>();

    // Optional:
    private final Map<Consumer<WebSocket>, Consumer<Object>> webSocketAdapters = new java.util.concurrent.ConcurrentHashMap<>();

    // --- Request/Response correlation map (keyed by BiDi requestId) ---
    private final java.util.Map<String, RequestImpl> requestsById = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public WDPageExtensionSupport wdExt() {
        return extension;
    }

    /**
     * Constructor for a new page.
     * @param browser
     */
    public PageImpl(BrowserImpl browser) {
        this(browser, (WDUserContext) null);
    }

    /**
     * Constructor for a new page.
     * @param browser
     * @param userContext
     */
    public PageImpl(BrowserImpl browser, WDUserContext userContext) {
        this.browser = browser;
        this.webDriver = browser.getWebDriver();

        this.isClosed = false;
        this.url = "about:blank"; // Standard-Startseite

        this.userContextId = userContext;

        // Erzeuge BrowsingContext MIT userContext
        this.browsingContext = new WDBrowsingContext(
                browser.getWebDriver().browsingContext()
                        .create(CreateType.TAB, null, false, userContext)
                        .getContext()
        );

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    /**
     * Constructor for a new page with a given pageId.
     * @param browser
     * @param userContext
     * @param browsingContext
     */
    public PageImpl(BrowserImpl browser,  WDUserContext userContext, WDBrowsingContext browsingContext) {
        this.browser = browser;
        this.webDriver = browser.getWebDriver();

        this.isClosed = false;
        this.url = "about:blank"; // Standard-Startseite

        this.userContextId = userContext;
        this.browsingContext = browsingContext;

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    public PageImpl(BrowserImpl browser, WDBrowsingContextEvent.Load load) {
        WDBrowsingContext context = load.getParams().getContext();
        PageImpl existingPage = browser.getPage(context);

        this.browser = browser;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        // 🔹 Falls keine existierende Seite vorhanden ist, eine neue Instanz initialisieren
        this.browsingContext = context;
        this.isClosed = false;
        this.url = load.getParams().getUrl();

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    public PageImpl(BrowserImpl browser, WDBrowsingContextEvent.DomContentLoaded domContentLoaded) {
        WDBrowsingContext context = domContentLoaded.getParams().getContext();
        PageImpl existingPage = browser.getPage(context);

        // 🔹 Übernahme der bestehenden Browser-Instanz und Session, falls vorhanden
        this.browser = browser;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        // 🔹 Falls keine existierende Seite vorhanden ist, eine neue Instanz initialisieren
        this.browsingContext = context;
        this.isClosed = false;
        this.url = domContentLoaded.getParams().getUrl();

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    public PageImpl(BrowserImpl browser, WDBrowsingContextEvent.Destroyed destroyed) {
        WDBrowsingContext context = destroyed.getParams().getContext();
        PageImpl existingPage = browser.getPage(context);

        // 🔹 Falls die Page existiert, markieren wir sie als geschlossen
        this.browser = browser;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        this.browsingContext = context;
        this.isClosed = true;  // Diese Page gilt als "destroyed"
        this.url = (existingPage != null) ? existingPage.url() : null;

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    public PageImpl(BrowserImpl browser, WDBrowsingContextEvent.Created created) {
        WDBrowsingContext context = created.getParams().getContext();
        PageImpl existingPage = browser.getPage(context);

        // 🔹 Falls eine existierende Page vorhanden ist, übernehmen wir ihre fehlenden Werte
        this.browser = browser;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : created.getParams().getUserContext();

        this.browsingContext = context;
        this.isClosed = false;
        this.url = created.getParams().getUrl();

        // Erzeuge der Mouse und Keyboard Input Handler FÜR DEN NEU ERSTELLTEN BrowsingContext:
        this.mouse = new de.bund.zrb.event.MouseImpl(browser.getInputManager(), getBrowsingContextId());
        this.keyboard = new de.bund.zrb.event.KeyboardImpl(browser.getInputManager(), getBrowsingContextId());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Standard Features, directly supported by WebDriver
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(java.util.function.Consumer<Page> handler) {
        if (handler == null || closeAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.Destroyed wd = as(ev, WDBrowsingContextEvent.Destroyed.class);
            if (wd != null) handler.accept(new PageImpl(browser, wd));
        };

        closeAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.CONTEXT_DESTROYED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offClose(java.util.function.Consumer<Page> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = closeAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_DESTROYED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onConsoleMessage(java.util.function.Consumer<ConsoleMessage> handler) {
        if (handler == null || consoleAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDLogEvent.EntryAdded wd = as(ev, WDLogEvent.EntryAdded.class);
            if (wd != null) handler.accept(new ConsoleMessageImpl(browser, wd));
        };

        consoleAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.ENTRY_ADDED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offConsoleMessage(java.util.function.Consumer<ConsoleMessage> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = consoleAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.ENTRY_ADDED.getName(), getBrowsingContextId(), adapter);
        }
    }

    // ToDo: Not supported yet
//    @Override
//    public void offConsoleMessage(Consumer<ConsoleMessage> handler) {
//        if (consoleMessageSubscription != null) {
//            session.removeEventListener(consoleMessageSubscription, handler);
//            consoleMessageSubscription = null;
//        }
//    }

    @Override
    public void onCrash(Consumer<Page> handler) {
        if (handler == null || crashAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.NavigationFailed wd = as(ev, WDBrowsingContextEvent.NavigationFailed.class);
            if (wd != null) {
                // Playwrights onCrash liefert die Page – hier genügt die aktuelle Instanz.
                handler.accept(this);
            }
        };

        crashAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.NAVIGATION_FAILED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offCrash(Consumer<Page> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = crashAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.NAVIGATION_FAILED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onDialog(java.util.function.Consumer<Dialog> handler) {
        if (handler == null || dialogAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.UserPromptOpened wd = as(ev, WDBrowsingContextEvent.UserPromptOpened.class);
            if (wd != null) handler.accept(new DialogImpl(browser, wd));
        };

        dialogAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.USER_PROMPT_OPENED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offDialog(java.util.function.Consumer<Dialog> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = dialogAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.USER_PROMPT_OPENED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onDOMContentLoaded(java.util.function.Consumer<Page> handler) {
        if (handler == null || domLoadedAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.DomContentLoaded wd = as(ev, WDBrowsingContextEvent.DomContentLoaded.class);
            if (wd != null) handler.accept(new PageImpl(browser, wd));
        };

        domLoadedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.DOM_CONTENT_LOADED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offDOMContentLoaded(java.util.function.Consumer<Page> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = domLoadedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.DOM_CONTENT_LOADED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onLoad(java.util.function.Consumer<Page> handler) {
        if (handler == null || loadAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.Load wd = as(ev, WDBrowsingContextEvent.Load.class);
            if (wd != null) handler.accept(new PageImpl(browser, wd));
        };

        loadAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.LOAD.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offLoad(java.util.function.Consumer<Page> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = loadAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.LOAD.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onRequest(java.util.function.Consumer<Request> handler) {
        if (handler == null || reqBeforeAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDNetworkEvent.BeforeRequestSent wd = as(ev, WDNetworkEvent.BeforeRequestSent.class);
            if (wd != null) {
                String requestId = extractRequestId(wd.getParams());
                if (requestId != null) {
                    // Create RequestImpl once and cache it by requestId
                    RequestImpl req = new RequestImpl(wd);
                    requestsById.put(requestId, req);
                    handler.accept(req);
                } else {
                    // Fallback: create without caching if no requestId
                    handler.accept(new RequestImpl(wd));
                }
            }
        };

        reqBeforeAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.BEFORE_REQUEST_SENT.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offRequest(java.util.function.Consumer<Request> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = reqBeforeAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.BEFORE_REQUEST_SENT.getName(), getBrowsingContextId(), adapter);
        }
    }


    @Override
    public void onRequestFailed(java.util.function.Consumer<Request> handler) {
        if (handler == null || reqFailedAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDNetworkEvent.FetchError wd = as(ev, WDNetworkEvent.FetchError.class);
            if (wd != null) {
                String requestId = extractRequestId(wd.getParams());
                RequestImpl req = requestsById.get(requestId);
                
                if (req != null) {
                    // Reuse existing RequestImpl and enrich with error data
                    req.enrichWithError(wd);
                    handler.accept(req);
                    // Clean up - request lifecycle is complete
                    requestsById.remove(requestId);
                } else {
                    // Fallback: create new RequestImpl if not found
                    handler.accept(new RequestImpl(wd));
                }
            }
        };

        reqFailedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.FETCH_ERROR.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offRequestFailed(java.util.function.Consumer<Request> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = reqFailedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.FETCH_ERROR.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onRequestFinished(java.util.function.Consumer<Request> handler) {
        if (handler == null || reqFinishedAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDNetworkEvent.ResponseCompleted wd = as(ev, WDNetworkEvent.ResponseCompleted.class);
            if (wd != null) {
                String requestId = extractRequestId(wd.getParams());
                RequestImpl req = requestsById.get(requestId);
                
                if (req != null) {
                    // Reuse existing RequestImpl and enrich with response data
                    req.enrichWithResponse(wd);
                    handler.accept(req);
                    // Clean up - request lifecycle is complete
                    requestsById.remove(requestId);
                } else {
                    // Fallback: create new RequestImpl if not found
                    handler.accept(new RequestImpl(wd));
                }
            }
        };

        reqFinishedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.RESPONSE_COMPLETED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offRequestFinished(java.util.function.Consumer<Request> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = reqFinishedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.RESPONSE_COMPLETED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onResponse(java.util.function.Consumer<Response> handler) {
        if (handler == null || respAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDNetworkEvent.ResponseStarted wd = as(ev, WDNetworkEvent.ResponseStarted.class);
            if (wd != null) {
                String requestId = extractRequestId(wd.getParams());
                RequestImpl req = requestsById.get(requestId);
                // Pass the Request reference to ResponseImpl (may be null if not yet seen)
                handler.accept(new ResponseImpl(wd, null, req));
            }
        };

        respAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.RESPONSE_STARTED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offResponse(java.util.function.Consumer<Response> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = respAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.RESPONSE_STARTED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onWebSocket(Consumer<WebSocket> handler) {
        throw new UnsupportedOperationException(
                "Page.onWebSocket ist derzeit nicht unterstützt (WebDriver BiDi bietet keine Page-WebSocket-Events).");
    }

    @Override
    public void offWebSocket(Consumer<WebSocket> handler) {
        throw new UnsupportedOperationException(
                "Page.offWebSocket ist derzeit nicht unterstützt (WebDriver BiDi bietet keine Page-WebSocket-Events).");
    }

    @Override
    public void onWorker(java.util.function.Consumer<Worker> handler) {
        if (handler == null || workerAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDScriptEvent.RealmCreated wd = as(ev, WDScriptEvent.RealmCreated.class);
            if (wd != null) handler.accept(new WorkerImpl(wd));
        };

        workerAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.REALM_CREATED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offWorker(java.util.function.Consumer<Worker> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = workerAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.REALM_CREATED.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onDownload(java.util.function.Consumer<Download> handler) {
        if (handler == null || downloadAdapters.containsKey(handler)) return;

        java.util.function.Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.DownloadWillBegin wd = as(ev, WDBrowsingContextEvent.DownloadWillBegin.class);
            if (wd != null) handler.accept(new DownloadImpl(browser, wd));
        };

        downloadAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.DOWNLOAD_WILL_BEGIN.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offDownload(java.util.function.Consumer<Download> handler) {
        if (handler == null) return;
        java.util.function.Consumer<Object> adapter = downloadAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.DOWNLOAD_WILL_BEGIN.getName(), getBrowsingContextId(), adapter);
        }
    }

    @Override
    public void onFileChooser(Consumer<FileChooser> handler) {
        if (handler == null || fileChooserAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            // Falls du eine konkrete WD-Eventklasse hast, nutze sie hier statt der direkten Mapper-Konvertierung:
            // WDBrowsingContextEvent.FileDialogOpened wd = as(ev, WDBrowsingContextEvent.FileDialogOpened.class);
            // if (wd != null) handler.accept(new FileChooserImpl(browser, wd));
            if (ev instanceof JsonObject) {
                FileChooser chooser = JsonToPlaywrightMapper.mapToInterface((JsonObject) ev, FileChooser.class);
                if (chooser != null) handler.accept(chooser);
            }
        };

        fileChooserAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.FILE_DIALOG_OPENED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offFileChooser(Consumer<FileChooser> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = fileChooserAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.FILE_DIALOG_OPENED.getName(), getBrowsingContextId(), adapter);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Advanced Features, not directly supported by WebDriver
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onFrameAttached(Consumer<Frame> handler) {
        if (handler == null || frameAttachedAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.Created created = as(ev, WDBrowsingContextEvent.Created.class);
            if (created != null) {
                WDInfo info = created.getParams();
                if (info.getParent() != null) {
                    Frame frame = new FrameImpl(
                            browser, this,
                            info.getUserContext(),
                            info.getClientWindow(),
                            info.getUrl(),
                            info.getChildren()
                    );
                    handler.accept(frame);
                }
            }
        };

        frameAttachedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), null, null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offFrameAttached(Consumer<Frame> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = frameAttachedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_CREATED.getName(), null, adapter);
        }
    }

    @Override
    public void onFrameDetached(Consumer<Frame> handler) {
        if (handler == null || frameDetachedAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.Destroyed destroyed = as(ev, WDBrowsingContextEvent.Destroyed.class);
            if (destroyed != null) {
                WDInfo info = destroyed.getParams();
                if (info.getParent() != null) {
                    Frame frame = new FrameImpl(
                            browser, this,
                            info.getUserContext(),
                            info.getClientWindow(),
                            info.getUrl(),
                            info.getChildren()
                    );
                    handler.accept(frame);
                }
            }
        };

        frameDetachedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.CONTEXT_DESTROYED.getName(), null, null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offFrameDetached(Consumer<Frame> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = frameDetachedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_DESTROYED.getName(), null, adapter);
        }
    }

    @Override
    public void onFrameNavigated(Consumer<Frame> handler) {
        if (handler == null || frameNavigatedAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            WDBrowsingContextEvent.NavigationStarted started = as(ev, WDBrowsingContextEvent.NavigationStarted.class);
            if (started != null) {
                WDNavigationInfo navInfo = started.getParams();
                WDBrowsingContextResult.GetTreeResult tree =
                        webDriver.browsingContext().getTree(navInfo.getContext(), 1L);

                for (WDInfo info : tree.getContexts()) {
                    if (info.getParent() != null) {
                        Frame frame = new FrameImpl(
                                browser, this,
                                info.getUserContext(),
                                info.getClientWindow(),
                                info.getUrl(),
                                info.getChildren()
                        );
                        handler.accept(frame);
                    }
                }
            }
        };

        frameNavigatedAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.NAVIGATION_STARTED.getName(), null, null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offFrameNavigated(Consumer<Frame> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = frameNavigatedAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.NAVIGATION_STARTED.getName(), null, adapter);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<Consumer<String>> pageErrorListeners = new ArrayList<>();

    @Override
    public void onPageError(Consumer<String> handler) {
        pageErrorListeners.add(handler);
    }

    @Override
    public void offPageError(Consumer<String> handler) {
        pageErrorListeners.remove(handler);
    }

    /**
     * Kann immer benutzt werden, wenn eine exception auf der Seite auftritt, z.B. bei einem evaluate-Script-Befehl.
     *
     * Solche Fehler führen nicht zu einem Java Fehler, da sie als Type "exception" und nicht "error" vom Browser
     * zurückgegeben werden. Außerdem sieht Playwright für den Page Content z.B. auch keine Java Exceptions vor und
     * liefert bestenfalls NULL zurück.
     *
     * @param errorMessage
     */
    private void notifyPageErrorListeners(String errorMessage) {
        for (Consumer<String> listener : pageErrorListeners) {
            listener.accept(errorMessage);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPopup(Consumer<Page> handler) {
        if (handler == null || popupAdapters.containsKey(handler)) return;

        Consumer<Object> adapter = ev -> {
            if (ev instanceof JsonObject) {
                Page popup = JsonToPlaywrightMapper.mapToInterface((JsonObject) ev, Page.class);
                if (popup != null) handler.accept(popup);
            }
        };

        popupAdapters.put(handler, adapter);
        WDSubscriptionRequest req = new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), getBrowsingContextId(), null);
        webDriver.addEventListener(req, adapter);
    }

    @Override
    public void offPopup(Consumer<Page> handler) {
        if (handler == null) return;
        Consumer<Object> adapter = popupAdapters.remove(handler);
        if (adapter != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_CREATED.getName(), getBrowsingContextId(), adapter);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Clock clock() {
        return null;
    }

    @Override
    public void addInitScript(String script) {
        addPreloadScriptResults.add(((BrowserImpl) browser).getScriptManager().addPreloadScript(
                script, getBrowsingContextId()
        ));
    }

    @Override
    public void addInitScript(Path scriptPath) {
        String scriptData = ScriptHelper.loadScript(scriptPath.toString());
        addPreloadScriptResults.add(((BrowserImpl) browser).getScriptManager().addPreloadScript(
                scriptData, getBrowsingContextId()
        ));
    }

    public void removeInitScripts() {
        for (WDScriptResult.AddPreloadScriptResult result : addPreloadScriptResults) {
            ((BrowserImpl) browser).getScriptManager().removePreloadScript(result.getScript().value());
        }
        addPreloadScriptResults.clear();
    }


    @Override
    public ElementHandle addScriptTag(AddScriptTagOptions options) {
        return null;
    }

    @Override
    public ElementHandle addStyleTag(AddStyleTagOptions options) {
        return null;
    }

    @Override
    public void bringToFront() {

    }

    @Override
    public void check(String selector, Page.CheckOptions pageOptions) {
        Locator.CheckOptions locatorOptions = new Locator.CheckOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }

        locator(selector, null).check(locatorOptions);
    }


    @Override
    public void click(String selector, Page.ClickOptions pageOptions) {
        // Konvertiere Page.ClickOptions → Locator.ClickOptions
        Locator.ClickOptions locatorOptions = new Locator.ClickOptions();
        if (pageOptions != null) {
            locatorOptions.setButton(pageOptions.button);
            locatorOptions.setClickCount(pageOptions.clickCount);
            locatorOptions.setDelay(pageOptions.delay);
            locatorOptions.setModifiers(pageOptions.modifiers);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }

        locator(selector, null).click(locatorOptions);
    }



    @Override
    public void close() {
        isClosed = true;
        browser.getWebDriver().browsingContext().close(getBrowsingContextId());
//        browser.getPages().remove(pageId);
    }

    @Override
    public void close(CloseOptions options) {
        throw new UnsupportedOperationException("Reasons and before Unload Handlers are not supported by WebDriver BiDi");
    }

    @Override
    public String content() {
        if (isClosed) {
            throw new PlaywrightException("Page is closed");
        }

        // Ziel: BrowsingContext für das `evaluate()`-Command setzen
        WDTarget.ContextTarget contextTarget = new WDTarget.ContextTarget(new WDBrowsingContext(getBrowsingContextId()));

        // WebDriver BiDi Evaluate-Command ausführen
        WDEvaluateResult evaluate = webDriver.script().evaluate(
                "return document.documentElement.outerHTML;", contextTarget, true);

        try {
            // ✅ Wenn erfolgreich, den String aus `PrimitiveProtocolValue.StringValue` extrahieren
            return ((WDEvaluateResult.WDEvaluateResultSuccess) evaluate).getResult().asString();
        } catch (ClassCastException e) {
            // ❌ Fehler aufgetreten -> wahrscheinlich WDEvaluateResultError
            WDEvaluateResult.WDEvaluateResultError error = (WDEvaluateResult.WDEvaluateResultError) evaluate;
            notifyPageErrorListeners(error.getExceptionDetails().getText());

            // 🔥 Fehlerbehandlung: Null zurückgeben, falls der HTML-Code nicht ausgelesen werden konnte
            return null;
        }
    }

    @Override
    public BrowserContext context() {
        return browser.getUserContextImpls().stream()
                .filter(uc -> uc.getUserContext().equals(this.userContextId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No UserContextImpl found for Page with contextId=" + this.userContextId.value()
                ));
    }

    @Override
    public void dblclick(String selector, DblclickOptions pageOptions) {
        Locator.DblclickOptions locatorOptions = new Locator.DblclickOptions();
        if (pageOptions != null) {
            locatorOptions.setButton(pageOptions.button);
            locatorOptions.setDelay(pageOptions.delay);
            locatorOptions.setModifiers(pageOptions.modifiers);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(selector, null).dblclick(locatorOptions);
    }

    @Override
    public void dispatchEvent(String selector, String type, Object eventInit, DispatchEventOptions pageOptions) {
        Locator.DispatchEventOptions locatorOptions = new Locator.DispatchEventOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        locator(selector, null).dispatchEvent(type, eventInit, locatorOptions);
    }

    @Override
    public void dragAndDrop(String source, String target, DragAndDropOptions pageOptions) {
        Locator.DragToOptions locatorOptions = new Locator.DragToOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(source, null).dragTo(locator(target, null), locatorOptions);
    }

    @Override
    public void emulateMedia(EmulateMediaOptions options) {
        // ToDo: Wird oft direkt am BrowserContext gemacht!
        throw new UnsupportedOperationException("emulateMedia is not yet supported for this driver");
    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg, EvalOnSelectorOptions pageOptions) {
        Locator locator = locator(selector, null);
        if (pageOptions != null && Boolean.TRUE.equals(pageOptions.strict)) {
            locator = locator.first();
        }
        return locator.evaluate(expression, arg);
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        return locator(selector, null).evaluateAll(expression, arg);
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        return evaluateHandle(expression, arg).jsonValue();
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        WDEvaluateResult result;
        WDTarget target = new WDTarget.ContextTarget(browsingContext); // oder RealmTarget

        if (WebDriverUtil.isFunctionExpression(expression)) {
            // Verwende callFunction wenn Argumente vorhanden sind
            List<WDLocalValue> args = arg != null
                    ? Collections.singletonList(WDLocalValue.fromObject(arg))
                    : Collections.emptyList();

            result = webDriver.script().callFunction(
                    expression,
                    true, // awaitPromise
                    target,
                    args,
                    null, // thisObject
                    WDResultOwnership.ROOT,
                    null // serializationOptions
            );
        } else {
            // Normales evaluate ohne Argumente
            result = webDriver.script().evaluate(
                    expression,
                    target,
                    true,
                    WDResultOwnership.ROOT,
                    null // sandbox
            );
        }

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue rv = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();

            return new JSHandleImpl(webDriver, rv, target);
        }

        // Handle JS-Exception vom Zielkontext
        if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            WDEvaluateResult.WDEvaluateResultError err = (WDEvaluateResult.WDEvaluateResultError) result;
            notifyPageErrorListeners(err.getExceptionDetails().getText());
            throw new PlaywrightException("Evaluation failed: " + err.getExceptionDetails().getText());
        }

        throw new PlaywrightException("evaluateHandle failed: unsupported result type "
                + result.getClass().getSimpleName());
    }

    @Override
    public void exposeBinding(String name, BindingCallback callback, ExposeBindingOptions options) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void exposeFunction(String name, FunctionCallback callback) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void fill(String selector, String value, Page.FillOptions pageOptions) {
        Locator.FillOptions locatorOptions = new Locator.FillOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setTimeout(pageOptions.timeout);
        }

        locator(selector, null).fill(value, locatorOptions);
    }

    @Override
    public void focus(String selector, Page.FocusOptions pageOptions) {
        Locator.FocusOptions locatorOptions = new Locator.FocusOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        locator(selector, null).focus(locatorOptions);
    }

    @Override
    public Frame frame(String name) {
        return null;
    }

    @Override
    public Frame frameByUrl(String url) {
        return null;
    }

    @Override
    public Frame frameByUrl(Pattern url) {
        return null;
    }

    @Override
    public Frame frameByUrl(Predicate<String> url) {
        return null;
    }

    @Override
    public FrameLocator frameLocator(String selector) {
        return null;
    }

    @Override
    public List<Frame> frames() {
        List<Frame> frames = new ArrayList<>();
        WDBrowsingContextResult.GetTreeResult tree = webDriver.browsingContext().getTree(browsingContext, Long.MAX_VALUE);
        Collection<WDInfo> infos = tree.getContexts();
        for(WDInfo info : infos) {
            WDBrowsingContext context = info.getContext();
            WDClientWindow clientWindow = info.getClientWindow();
            WDBrowsingContext originalOpener = info.getOriginalOpener();
            String url1 = info.getUrl();
            WDUserContext userContext = info.getUserContext();
            WDBrowsingContext parent = info.getParent();
            Collection<WDInfo> children = info.getChildren();

            if(parent != null) { // ToDo: Check if this is correct (all children frames only?)
                if(browsingContext.equals(parent)) {
                    frames.add(new FrameImpl(browser, this, userContext, clientWindow, url1, children));
                }
                else {
                    throw new PlaywrightException("Parent context does not correspond to the sub frame's context.");
                }
            }
        }
        return frames;
    }

    @Override
    public String getAttribute(String selector, String name, GetAttributeOptions options) {
        return "";
    }

    @Override
    public Locator getByText(String text, GetByTextOptions options) {
        return new LocatorImpl(webDriver, this, LocatorType.TEXT, text);
    }

    @Override
    public Locator getByLabel(String text, GetByLabelOptions options) {
        return new LocatorImpl(webDriver, this, LocatorType.LABEL, text);
    }

    @Override
    public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) {
        return new LocatorImpl(webDriver, this, LocatorType.PLACEHOLDER, text);
    }

    @Override
    public Locator getByTitle(String text, GetByTitleOptions options) {
        return new LocatorImpl(webDriver, this, LocatorType.TITLE, text);
    }

    @Override
    public Locator getByAltText(String text, GetByAltTextOptions options) {
        return new LocatorImpl(webDriver, this, LocatorType.ALTTEXT, text);
    }

    @Override
    public Locator getByRole(AriaRole role, GetByRoleOptions options) {
        StringBuilder token = new StringBuilder(role.name().toLowerCase());
        if (options != null) {
            if (options.name != null)     token.append(";name=").append(options.name);
            if (options.checked != null)  token.append(";checked=").append(options.checked);
            if (options.selected != null) token.append(";selected=").append(options.selected);
            if (options.expanded != null) token.append(";expanded=").append(options.expanded);
            if (options.includeHidden != null && options.includeHidden) token.append(";includeHidden=true");
            if (options.level != null)    token.append(";level=").append(options.level);
            if (options.pressed != null)  token.append(";pressed=").append(options.pressed);
        }
        return new LocatorImpl(webDriver, this, LocatorType.ROLE, token.toString());
    }

    @Override
    public Locator getByTestId(String testId) {
        // exakter Match auf das Standard-Attribut data-testid
        String css = "[data-testid='" + cssEsc(testId) + "']";
        return new LocatorImpl(webDriver, this, de.bund.zrb.util.LocatorType.CSS, css);
    }

    @Override
    public Locator getByRole(AriaRole role) {
        // ohne Options-Objekt: nur Rolle
        return getByRole(role, null);
    }

    @Override
    public Locator getByAltText(Pattern text, GetByAltTextOptions options) {
        String lit = xpLit(text.pattern());
        String xp = (options != null && Boolean.TRUE.equals(options.exact))
                ? "//*[@alt and normalize-space(@alt)=" + lit + "]"
                : "//*[@alt and contains(normalize-space(@alt)," + lit + ")]";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByLabel(Pattern text, GetByLabelOptions options) {
        String lit  = xpLit(text.pattern());
        String cond = (options != null && Boolean.TRUE.equals(options.exact))
                ? "normalize-space(string(.))=" + lit
                : "contains(normalize-space(string(.))," + lit + ")";
        String xp = "//*[@id=//label[" + cond + "]/@for]"
                + " | //label[" + cond + "]//input"
                + " | //label[" + cond + "]//textarea"
                + " | //label[" + cond + "]//select";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByPlaceholder(Pattern text, GetByPlaceholderOptions options) {
        String lit = xpLit(text.pattern());
        String xp = (options != null && Boolean.TRUE.equals(options.exact))
                ? "//*[@placeholder and normalize-space(@placeholder)=" + lit + "]"
                : "//*[@placeholder and contains(normalize-space(@placeholder)," + lit + ")]";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByTestId(Pattern testId) {
        String xp = "//*[@data-testid and contains(@data-testid," + xpLit(testId.pattern()) + ")]";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByText(Pattern text, GetByTextOptions options) {
        String lit = xpLit(text.pattern());
        String xp = (options != null && Boolean.TRUE.equals(options.exact))
                ? "//*[normalize-space(string(.))=" + lit + "]"
                : "//*[contains(normalize-space(string(.))," + lit + ")]";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Locator getByTitle(Pattern text, GetByTitleOptions options) {
        String lit = xpLit(text.pattern());
        String xp = (options != null && Boolean.TRUE.equals(options.exact))
                ? "//*[@title and normalize-space(@title)=" + lit + "]"
                : "//*[@title and contains(normalize-space(@title)," + lit + ")]";
        return new LocatorImpl(webDriver, this, LocatorType.XPATH, xp);
    }

    @Override
    public Response goBack(GoBackOptions options) {
        browser.getWebDriver().browsingContext().traverseHistory(getBrowsingContextId(), -1);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public Response goForward(GoForwardOptions options) {
        browser.getWebDriver().browsingContext().traverseHistory(getBrowsingContextId(), 1);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public void requestGC() {

    }

    @Override
    public Response navigate(String url, NavigateOptions options) {
        if (isClosed) {
            throw new PlaywrightException("Page is closed");
        }
        if (getBrowsingContextId() == null || getBrowsingContextId().isEmpty()) {
            throw new PlaywrightException("Cannot navigate: contextId is null or empty!");
        }
        this.url = url;

        // WebDriver BiDi Befehl senden
        WDBrowsingContextResult.NavigateResult navigate = browser.getWebDriver().browsingContext().navigate(url, getBrowsingContextId());

        return new PlaywrightResponse<WDBrowsingContextResult.NavigateResult>(navigate);
    }

    @Override
    public void hover(String selector, Page.HoverOptions pageOptions) {
        Locator.HoverOptions locatorOptions = new Locator.HoverOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setModifiers(pageOptions.modifiers);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(selector, null).hover(locatorOptions);
    }

    @Override
    public String innerHTML(String selector, Page.InnerHTMLOptions pageOptions) {
        Locator.InnerHTMLOptions locatorOptions = new Locator.InnerHTMLOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).innerHTML(locatorOptions);
    }

    @Override
    public String innerText(String selector, Page.InnerTextOptions pageOptions) {
        Locator.InnerTextOptions locatorOptions = new Locator.InnerTextOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).innerText(locatorOptions);
    }

    @Override
    public String inputValue(String selector, Page.InputValueOptions pageOptions) {
        Locator.InputValueOptions locatorOptions = new Locator.InputValueOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).inputValue(locatorOptions);
    }

    @Override
    public boolean isChecked(String selector, Page.IsCheckedOptions pageOptions) {
        Locator.IsCheckedOptions locatorOptions = new Locator.IsCheckedOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isChecked(locatorOptions);
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }

    @Override
    public boolean isDisabled(String selector, Page.IsDisabledOptions pageOptions) {
        Locator.IsDisabledOptions locatorOptions = new Locator.IsDisabledOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isDisabled(locatorOptions);
    }

    @Override
    public boolean isEditable(String selector, Page.IsEditableOptions pageOptions) {
        Locator.IsEditableOptions locatorOptions = new Locator.IsEditableOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isEditable(locatorOptions);
    }

    @Override
    public boolean isEnabled(String selector, Page.IsEnabledOptions pageOptions) {
        Locator.IsEnabledOptions locatorOptions = new Locator.IsEnabledOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isEnabled(locatorOptions);
    }

    @Override
    public boolean isHidden(String selector, Page.IsHiddenOptions pageOptions) {
        Locator.IsHiddenOptions locatorOptions = new Locator.IsHiddenOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isHidden(locatorOptions);
    }

    @Override
    public boolean isVisible(String selector, Page.IsVisibleOptions pageOptions) {
        Locator.IsVisibleOptions locatorOptions = new Locator.IsVisibleOptions();
        if (pageOptions != null) {
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        return locator(selector).isVisible(locatorOptions);
    }

    @Override
    public Keyboard keyboard() {
        return null;
    }

    @Override
    public Locator locator(String selector, LocatorOptions options) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        LocatorType type = AdapterLocatorFactory.inferType(selector);
        String value = AdapterLocatorFactory.stripKnownPrefix(selector);
        return new LocatorImpl(webDriver, this, type, value);
    }

    @Override
    public Frame mainFrame() {
        return null;
    }

    @Override
    public Mouse mouse() {
        return null;
    }

    @Override
    public void onceDialog(Consumer<Dialog> handler) {

    }

    @Override
    public Page opener() {
        return null;
    }

    @Override
    public void pause() {

    }

    @Override
    public byte[] pdf(PdfOptions options) {
        return new byte[0];
    }

    @Override
    public void press(String selector, String key, Page.PressOptions pageOptions) {
        Locator.PressOptions locatorOptions = new Locator.PressOptions();
        if (pageOptions != null) {
            locatorOptions.setDelay(pageOptions.delay);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setTimeout(pageOptions.timeout);
        }

        locator(selector, null).press(key, locatorOptions);
    }

    @Override
    public ElementHandle querySelector(String selector, QuerySelectorOptions options) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }

        // Einheitlich typisieren
        LocatorType type = AdapterLocatorFactory.inferType(selector);
        String value     = AdapterLocatorFactory.stripKnownPrefix(selector);
        WDLocator<?> locator = AdapterLocatorFactory.create(type, value); // ⬅️ ruft jetzt auch den Sanitizer für CSS

        WDBrowsingContextResult.LocateNodesResult nodes =
                browser.getWebDriver().browsingContext().locateNodes(getBrowsingContextId(), locator);

        if (nodes.getNodes().isEmpty()) return null;

        WDRemoteValue.NodeRemoteValue node = nodes.getNodes().get(0);
        WDTarget.ContextTarget target = new WDTarget.ContextTarget(new WDBrowsingContext(getBrowsingContextId()));
        return new ElementHandleImpl(webDriver, node, target);
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }

        LocatorType type = AdapterLocatorFactory.inferType(selector);
        String value = AdapterLocatorFactory.stripKnownPrefix(selector);
        WDLocator<?> wdLocator = AdapterLocatorFactory.create(type, value);

        WDBrowsingContextResult.LocateNodesResult nodes =
                browser.getWebDriver().browsingContext()
                        .locateNodes(getBrowsingContextId(), wdLocator, Integer.MAX_VALUE);

        List<ElementHandle> out = new ArrayList<>();
        for (WDRemoteValue.NodeRemoteValue n : nodes.getNodes()) {
            out.add(new ElementHandleImpl(
                    webDriver,
                    n,
                    new WDTarget.ContextTarget(getBrowsingContext())
            ));
        }
        return out;
    }

    @Override
    public void addLocatorHandler(Locator locator, Consumer<Locator> handler, AddLocatorHandlerOptions options) {

    }

    @Override
    public void removeLocatorHandler(Locator locator) {

    }

    @Override
    public Response reload(ReloadOptions options) {
        browser.getWebDriver().browsingContext().reload(getBrowsingContextId());

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public APIRequestContext request() {
        return null;
    }

    @Override
    public void route(String url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void route(Pattern url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void route(Predicate<String> url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void routeFromHAR(Path har, RouteFromHAROptions options) {

    }

    @Override
    public void routeWebSocket(String url, Consumer<WebSocketRoute> handler) {

    }

    @Override
    public void routeWebSocket(Pattern url, Consumer<WebSocketRoute> handler) {

    }

    @Override
    public void routeWebSocket(Predicate<String> url, Consumer<WebSocketRoute> handler) {

    }

    /**
     * Takes a screenshot of the current browsing context.
     * <p>
     * Diese Implementierung unterstützt die Playwright-Screenshot-Parameter, soweit sie mit dem
     * BiDi-Standard kompatibel sind:
     * <ul>
     *   <li>{@code fullPage}: mapped to {@code Origin} (viewport or document)</li>
     *   <li>{@code type} and {@code quality}: mapped to {@code ImageFormat}</li>
     *   <li>{@code clip}: mapped to {@code BoxClipRectangle}</li>
     *   <li>{@code path}: wenn gesetzt, wird das Bild lokal gespeichert</li>
     * </ul>
     *
     * <p>
     * <strong>Extras:</strong>
     * <ul>
     *   <li>{@code animations = DISABLED}: stoppt CSS-Animationen & Transitionen vor dem Screenshot</li>
     *   <li>{@code caret = HIDE}: blendet den Text-Cursor aus</li>
     *   <li>{@code mask + maskColor}: überlagert bestimmte Elemente mit farbigen Overlays</li>
     * </ul>
     *
     * <p>
     * Diese Features werden durch JavaScript-Injection umgesetzt. Nach dem Screenshot musst du
     * {@link #restorePreprocessingStyles()} aufrufen, um die Seite wieder in den Ursprungszustand zu bringen.
     *
     * @param options die Screenshot-Optionen (können {@code null} sein)
     * @return das Screenshot-Bild als Byte-Array (PNG oder JPEG, je nach {@code type})
     * @throws PlaywrightException wenn das Speichern fehlschlägt oder der BiDi-Befehl scheitert
     */
    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        if(options != null) {
            waitForActionabilityBeforeScreenshot(options.timeout != null ? options.timeout : 3000);
        } else  {
            waitForActionabilityBeforeScreenshot(3000);
        }
        // ---------------------------------------------------------------
        // Playwright-like Preprocessing (optional)
        // ---------------------------------------------------------------
        if (options != null) {
            ScreenshotPreprocessor.Evaluator evaluator = script -> this.evaluate(script, null);

            if (options.animations == ScreenshotAnimations.DISABLED) {
                ScreenshotPreprocessor.disableAnimations(evaluator);
            }

            if (options.caret == ScreenshotCaret.HIDE) {
                ScreenshotPreprocessor.hideCaret(evaluator);
            }

            if (options.mask != null && !options.mask.isEmpty()) {
                ScreenshotPreprocessor.applyMask(options.mask, options.maskColor, evaluator);
            }
        }

        // ---------------------------------------------------------------
        // Standard Screenshot BiDi
        // ---------------------------------------------------------------
        WDBrowsingContext context = new WDBrowsingContext(getBrowsingContextId());

        // Origin bestimmen: fullPage = document, sonst viewport
        CaptureScreenshotParameters.Origin origin = CaptureScreenshotParameters.Origin.VIEWPORT;
        if (options != null && Boolean.TRUE.equals(options.fullPage)) {
            origin = CaptureScreenshotParameters.Origin.DOCUMENT;
        }

        // ImageFormat bestimmen: type + optional quality
        CaptureScreenshotParameters.ImageFormat imageFormat = null;
        if (options != null && options.type != null) {
            String type = options.type.name().toLowerCase();
            if ("jpeg".equals(type) && options.quality != null) {
                float quality = options.quality.floatValue() / 100.0f; // Playwright: 0..1
                imageFormat = new CaptureScreenshotParameters.ImageFormat(type, quality);
            } else {
                imageFormat = new CaptureScreenshotParameters.ImageFormat(type);
            }
        }

        // Clip bestimmen, falls gesetzt
        CaptureScreenshotParameters.ClipRectangle clip = null;
        if (options != null && options.clip != null) {
            clip = new CaptureScreenshotParameters.ClipRectangle.BoxClipRectangle(
                    (int) options.clip.x,
                    (int) options.clip.y,
                    (int) options.clip.width,
                    (int) options.clip.height
            );
        }

        // Screenshot ausführen
        WDBrowsingContextResult.CaptureScreenshotResult result =
                browser.getWebDriver().browsingContext().captureScreenshot(context, origin, imageFormat, clip);

        byte[] imageBytes = Base64.getDecoder().decode(result.getData());

        // Speichere auf Disk, falls Pfad gesetzt
        if (options != null && options.path != null) {
            try {
                java.nio.file.Files.write(options.path, imageBytes);
            } catch (java.io.IOException e) {
                throw new PlaywrightException("Failed to write screenshot to: " + options.path, e);
            }
        }

        return imageBytes;
    }

    /**
     * Entfernt ALLE Preprocessing-Styles & Masken, die durch Screenshot-Optionen gesetzt wurden.
     */
    public void restorePreprocessingStyles() {
        ScreenshotPreprocessor.restore(script -> this.evaluate(script, null));
    }

    @Override
    public List<String> selectOption(String selector, String values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, String[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public void setChecked(String selector, boolean checked, Page.SetCheckedOptions pageOptions) {
        Locator.SetCheckedOptions locatorOptions = new Locator.SetCheckedOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(selector, null).setChecked(checked, locatorOptions);
    }

    @Override
    public void setContent(String html, SetContentOptions options) {

    }

    @Override
    public void setDefaultNavigationTimeout(double timeout) {

    }

    @Override
    public void setDefaultTimeout(double timeout) {

    }

    @Override
    public void setExtraHTTPHeaders(Map<String, String> headers) {

    }

    @Override
    public void setInputFiles(String selector, Path files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, Path[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setViewportSize(int width, int height) {

    }

    @Override
    public void tap(String selector, Page.TapOptions pageOptions) {
        Locator.TapOptions locatorOptions = new Locator.TapOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setModifiers(pageOptions.modifiers);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(selector, null).tap(locatorOptions);
    }

    @Override
    public String textContent(String selector, TextContentOptions options) {
        return "";
    }

    @Override
    public String title() {
        return "Dummy Title"; // Placeholder implementation
    }

    @Override
    public Touchscreen touchscreen() {
        return null;
    }

    @Override
    public void type(String selector, String text, Page.TypeOptions pageOptions) {
        Locator.TypeOptions locatorOptions = new Locator.TypeOptions();
        if (pageOptions != null) {
            locatorOptions.setDelay(pageOptions.delay);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setTimeout(pageOptions.timeout);
        }
        locator(selector, null).type(text, locatorOptions);
    }

    @Override
    public void uncheck(String selector, Page.UncheckOptions pageOptions) {
        Locator.UncheckOptions locatorOptions = new Locator.UncheckOptions();
        if (pageOptions != null) {
            locatorOptions.setForce(pageOptions.force);
            locatorOptions.setNoWaitAfter(pageOptions.noWaitAfter);
            locatorOptions.setPosition(pageOptions.position);
            locatorOptions.setTimeout(pageOptions.timeout);
            locatorOptions.setTrial(pageOptions.trial);
        }
        locator(selector, null).uncheck(locatorOptions);
    }

    @Override
    public void unrouteAll() {

    }

    @Override
    public void unroute(String url, Consumer<Route> handler) {

    }

    @Override
    public void unroute(Pattern url, Consumer<Route> handler) {

    }

    @Override
    public void unroute(Predicate<String> url, Consumer<Route> handler) {

    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Video video() {
        return null;
    }

    @Override
    public ViewportSize viewportSize() {
        return null;
    }

    @Override
    public Page waitForClose(WaitForCloseOptions options, Runnable callback) {
        return null;
    }

    @Override
    public ConsoleMessage waitForConsoleMessage(WaitForConsoleMessageOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Download waitForDownload(WaitForDownloadOptions options, Runnable callback) {
        final Download[] result = new Download[1];

        Consumer<Download> listener = download -> result[0] = download;

        this.onDownload(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offDownload(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for download");
        }

        return result[0];
    }

    @Override
    public FileChooser waitForFileChooser(WaitForFileChooserOptions options, Runnable callback) {
        final FileChooser[] result = new FileChooser[1];

        Consumer<FileChooser> listener = fileChooser -> result[0] = fileChooser;

        this.onFileChooser(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offFileChooser(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for file chooser");
        }

        return result[0];
    }

    @Override
    public JSHandle waitForFunction(String expression, Object arg, WaitForFunctionOptions options) {
        long timeout = options != null &&  options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            Object result = this.evaluate(expression, arg);
            if (result instanceof Boolean && (Boolean) result) {
                return evaluateHandle(expression, arg);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        throw new PlaywrightException("Timeout exceeded while waiting for function");
    }

    @Override
    public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {

    }

    @Override
    public Response waitForNavigation(WaitForNavigationOptions options, Runnable callback) {
        final Response[] responseHolder = new Response[1];
        Consumer<Page> listener = page -> responseHolder[0] = new PlaywrightResponse<>(null);

        this.onLoad(listener);

        if (callback != null) {
            callback.run();
        }

        // Wait until navigation event received
        long timeout = options != null &&  options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();
        while (responseHolder[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        this.offLoad(listener);

        if (responseHolder[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for navigation");
        }

        return responseHolder[0];
    }

    @Override
    public Page waitForPopup(WaitForPopupOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Request waitForRequest(String urlOrPredicate, WaitForRequestOptions options, Runnable callback) {
        final Request[] result = new Request[1];

        Consumer<Request> listener = request -> {
            if (request.url().equals(urlOrPredicate)) {
                result[0] = request;
            }
        };

        this.onRequest(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offRequest(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for request: " + urlOrPredicate);
        }

        return result[0];
    }


    @Override
    public Request waitForRequest(Pattern urlPattern, WaitForRequestOptions options, Runnable callback) {
        final Request[] result = new Request[1];

        Consumer<Request> listener = request -> {
            if (urlPattern.matcher(request.url()).matches()) {
                result[0] = request;
            }
        };

        this.onRequest(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offRequest(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for request matching pattern: " + urlPattern.pattern());
        }

        return result[0];
    }

    @Override
    public Request waitForRequest(Predicate<Request> requestPredicate, WaitForRequestOptions options, Runnable callback) {
        final Request[] result = new Request[1];

        Consumer<Request> listener = request -> {
            if (requestPredicate.test(request)) {
                result[0] = request;
            }
        };

        this.onRequest(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offRequest(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for request with predicate");
        }

        return result[0];
    }

    @Override
    public Request waitForRequestFinished(WaitForRequestFinishedOptions options, Runnable callback) {
        final Request[] result = new Request[1];

        Consumer<Request> listener = request -> result[0] = request;

        this.onRequestFinished(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offRequestFinished(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for request to finish");
        }

        return result[0];
    }

    @Override
    public Response waitForResponse(String urlOrPredicate, WaitForResponseOptions options, Runnable callback) {
        final Response[] result = new Response[1];

        Consumer<Response> listener = response -> {
            if (response.url().equals(urlOrPredicate)) {
                result[0] = response;
            }
        };

        this.onResponse(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offResponse(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for response: " + urlOrPredicate);
        }

        return result[0];
    }

    @Override
    public Response waitForResponse(Pattern urlPattern, WaitForResponseOptions options, Runnable callback) {
        final Response[] result = new Response[1];

        Consumer<Response> listener = response -> {
            if (urlPattern.matcher(response.url()).matches()) {
                result[0] = response;
            }
        };

        this.onResponse(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offResponse(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for response matching pattern: " + urlPattern.pattern());
        }

        return result[0];
    }

    @Override
    public Response waitForResponse(Predicate<Response> responsePredicate, WaitForResponseOptions options, Runnable callback) {
        final Response[] result = new Response[1];

        Consumer<Response> listener = response -> {
            if (responsePredicate.test(response)) {
                result[0] = response;
            }
        };

        this.onResponse(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offResponse(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for response with predicate");
        }

        return result[0];
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        long timeout = options != null &&  options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            ElementHandle handle = querySelector(selector, null);
            if (handle != null) {
                return handle;
            }
            try {
                Thread.sleep(50); // Poll alle 50 ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new PlaywrightException("Timeout exceeded while waiting for selector: " + selector);
    }


    @Override
    public void waitForCondition(BooleanSupplier condition, WaitForConditionOptions options) {
        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new PlaywrightException("Timeout exceeded while waiting for condition");
    }

    @Override
    public void waitForTimeout(double timeout) {
        long millis = (long) timeout; // Hier brauchst du keinen Double-Wrapper, ist primitive
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void waitForURL(String expectedUrl, WaitForURLOptions options) {
        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            String currentUrl = this.url();
            if (currentUrl != null && currentUrl.equals(expectedUrl)) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new PlaywrightException("Timeout exceeded while waiting for URL: " + expectedUrl);
    }

    @Override
    public void waitForURL(Pattern expectedUrlPattern, WaitForURLOptions options) {
        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            String currentUrl = this.url();
            if (currentUrl != null && expectedUrlPattern.matcher(currentUrl).matches()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new PlaywrightException("Timeout exceeded while waiting for URL pattern: " + expectedUrlPattern.pattern());
    }

    @Override
    public void waitForURL(Predicate<String> urlPredicate, WaitForURLOptions options) {
        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeout) {
            String currentUrl = this.url();
            if (currentUrl != null && urlPredicate.test(currentUrl)) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new PlaywrightException("Timeout exceeded while waiting for URL with predicate");
    }

    @Override
    public WebSocket waitForWebSocket(WaitForWebSocketOptions options, Runnable callback) {
        final WebSocket[] result = new WebSocket[1];
        Consumer<WebSocket> listener = ws -> result[0] = ws;

        this.onWebSocket(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offWebSocket(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for WebSocket");
        }

        return result[0];
    }

    @Override
    public Worker waitForWorker(WaitForWorkerOptions options, Runnable callback) {
        final Worker[] result = new Worker[1];
        Consumer<Worker> listener = worker -> result[0] = worker;

        this.onWorker(listener);

        if (callback != null) {
            callback.run();
        }

        long timeout = options != null && options.timeout != null ? (long) options.timeout.doubleValue() : 30_000L;
        long start = System.currentTimeMillis();

        while (result[0] == null && System.currentTimeMillis() - start < timeout) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.offWorker(listener);

        if (result[0] == null) {
            throw new PlaywrightException("Timeout exceeded while waiting for Worker");
        }

        return result[0];
    }

    @Override
    public List<Worker> workers() {
        return Collections.emptyList();
    }

    public WDBrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public String getBrowsingContextId() {
        if (browsingContext == null) {
            throw new PlaywrightException("Browsing context is null.");
        }
        return browsingContext.value();
    }

    public WDUserContext getUserContext() {
        return userContextId;
    }

    public String getUserContextId() {
        return userContextId.value();
    }

    public BrowserImpl getBrowser() {
        return browser;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WebDriver getWebDriver() {
        return webDriver;
    }

    private static String xpLit(String s) {
        if (s.indexOf('\'') == -1) return "'" + s + "'";
        if (s.indexOf('"') == -1) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) sb.append(",");
            char c = s.charAt(i);
            if (c == '\'') sb.append("\"'\"");
            else sb.append("'").append(c).append("'");
        }
        sb.append(")");
        return sb.toString();
    }
    private static String cssEsc(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static <T> T as(Object ev, Class<T> cls) {
        if (cls.isInstance(ev)) return cls.cast(ev);
        if (ev instanceof com.google.gson.JsonObject) {
            try {
                return de.bund.zrb.support.JsonToPlaywrightMapper.mapToInterface((com.google.gson.JsonObject) ev, cls);
            } catch (Throwable ignore) { /* not our type */ }
        }
        return null;
    }

    /** Wartet rAF*2 + Fonts.ready (falls vorhanden). Kein JSHandle, kein Polling. */
    private void waitForActionabilityBeforeScreenshot(double timeoutMs) {
        long cap = Math.max(1000L, (long) timeoutMs); // defensiv
        List<WDLocalValue> args = Collections.singletonList(WDLocalValue.fromObject(cap));

        // Einmalige Promise, die nach Fonts.ready (wenn vorhanden) und 2 rAF auflöst.
        webDriver.script().callFunction(
                "function(maxMs){"
                        + "  const deadline = Date.now() + (typeof maxMs==='number' ? maxMs : 5000);"
                        + "  return new Promise((resolve)=>{"
                        + "    const finish = ()=>resolve();"
                        + "    const twoRaf = ()=>{"
                        + "      try{"
                        + "        if (document.visibilityState==='visible' && typeof requestAnimationFrame==='function'){"
                        + "          requestAnimationFrame(()=>requestAnimationFrame(finish));"
                        + "        } else { setTimeout(finish,50); }"
                        + "      } catch(_) { setTimeout(finish,50); }"
                        + "    };"
                        + "    const afterFonts = ()=>{"
                        + "      try {"
                        + "        if (document.fonts && document.fonts.ready && typeof document.fonts.ready.then==='function'){"
                        + "          // Fonts stabilisieren das Layout; nicht unendlich warten:"
                        + "          let timed = false;"
                        + "          const t = setTimeout(()=>{ if(!timed){ timed=true; twoRaf(); } }, Math.max(0, deadline - Date.now()));"
                        + "          document.fonts.ready.then(()=>{ if(!timed){ timed=true; clearTimeout(t); twoRaf(); } }, ()=>{ if(!timed){ timed=true; clearTimeout(t); twoRaf(); } });"
                        + "        } else { twoRaf(); }"
                        + "      } catch(_) { twoRaf(); }"
                        + "    };"
                        + "    afterFonts();"
                        + "  });"
                        + "}",
                /* awaitPromise */ true,
                new WDTarget.ContextTarget(browsingContext),
                args,
                null,
                WDResultOwnership.NONE,
                null
        );
    }

    /**
     * Extracts the requestId from network event parameters.
     * The requestId is used to correlate Request objects across different network events.
     */
    private String extractRequestId(de.bund.zrb.type.network.WDBaseParameters params) {
        if (params == null) return null;
        de.bund.zrb.type.network.WDRequestData reqData = params.getRequest();
        if (reqData == null) return null;
        de.bund.zrb.type.network.WDRequest request = reqData.getRequest();
        if (request == null) return null;
        return request.value();
    }


}
