package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Hält die Wiring-Strategie (Attach/Detach) für Page- und Context-Mode.
 * - attachPage/attachContext: registrieren Handler und RETURNEN die konkrete Handler-Instanz.
 * - detachPage/detachContext: deregistrieren anhand der zuvor zurückgegebenen Handler-Instanz.
 *
 * Anwendercode speichert sich die zurückgegebenen Handler je Event,
 * z.B. in Map<WDEventNames, Consumer<?>>, um später gezielt abzuhängen.
 */
public final class WDEventWiringConfig {

    /** Event -> (Page, sink) -> registered handler */
    private final Map<WDEventNames, BiFunction<Page, BiConsumer<String,Object>, Consumer<?>>> attachPage;
    /** Event -> (Page, handler) -> off(...) */
    private final Map<WDEventNames, BiConsumer<Page, Consumer<?>>> detachPage;

    /** Event -> (Context, sink) -> registered handler */
    private final Map<WDEventNames, BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>>> attachContext;
    /** Event -> (Context, handler) -> off(...) */
    private final Map<WDEventNames, BiConsumer<BrowserContext, Consumer<?>>> detachContext;

    private WDEventWiringConfig(Builder b) {
        this.attachPage    = unmodifiableEnumMap(b.attachPage);
        this.detachPage    = unmodifiableEnumMap(b.detachPage);
        this.attachContext = unmodifiableEnumMap(b.attachContext);
        this.detachContext = unmodifiableEnumMap(b.detachContext);
    }

    public Map<WDEventNames, BiFunction<Page, BiConsumer<String,Object>, Consumer<?>>> getAttachPage() {
        return attachPage;
    }
    public Map<WDEventNames, BiConsumer<Page, Consumer<?>>> getDetachPage() {
        return detachPage;
    }
    public Map<WDEventNames, BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>>> getAttachContext() {
        return attachContext;
    }
    public Map<WDEventNames, BiConsumer<BrowserContext, Consumer<?>>> getDetachContext() {
        return detachContext;
    }

    // ---------- Builder ----------
    public static final class Builder {
        private final EnumMap<WDEventNames, BiFunction<Page, BiConsumer<String,Object>, Consumer<?>>> attachPage =
                new EnumMap<WDEventNames, BiFunction<Page, BiConsumer<String,Object>, Consumer<?>>>(WDEventNames.class);
        private final EnumMap<WDEventNames, BiConsumer<Page, Consumer<?>>> detachPage =
                new EnumMap<WDEventNames, BiConsumer<Page, Consumer<?>>>(WDEventNames.class);

        private final EnumMap<WDEventNames, BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>>> attachContext =
                new EnumMap<WDEventNames, BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>>>(WDEventNames.class);
        private final EnumMap<WDEventNames, BiConsumer<BrowserContext, Consumer<?>>> detachContext =
                new EnumMap<WDEventNames, BiConsumer<BrowserContext, Consumer<?>>>(WDEventNames.class);

        public Builder enablePage(final WDEventNames ev,
                                  final BiFunction<Page, BiConsumer<String,Object>, Consumer<?>> attach,
                                  final BiConsumer<Page, Consumer<?>> detach) {
            if (ev != null && attach != null && detach != null) {
                attachPage.put(ev, attach);
                detachPage.put(ev, detach);
            }
            return this;
        }

        public Builder enableContext(final WDEventNames ev,
                                     final BiFunction<BrowserContext, BiConsumer<String,Object>, Consumer<?>> attach,
                                     final BiConsumer<BrowserContext, Consumer<?>> detach) {
            if (ev != null && attach != null && detach != null) {
                attachContext.put(ev, attach);
                detachContext.put(ev, detach);
            }
            return this;
        }

        /** Entfernt ein Page-Event (wird dann nicht mehr verdrahtet). */
        public Builder disablePage(final WDEventNames ev) {
            if (ev != null) {
                attachPage.remove(ev);
                detachPage.remove(ev);
            }
            return this;
        }

        /** Entfernt ein Context-Event (wird dann nicht mehr verdrahtet). */
        public Builder disableContext(final WDEventNames ev) {
            if (ev != null) {
                attachContext.remove(ev);
                detachContext.remove(ev);
            }
            return this;
        }

        public WDEventWiringConfig build() {
            return new WDEventWiringConfig(this);
        }

        // ---------- Komfort: Defaults für gängige Events ----------
        public Builder withDefaultPageWiring() {
            // PAGE: CONTEXT_DESTROYED ~ onClose
            enablePage(WDEventNames.CONTEXT_DESTROYED,
                    (page, sink) -> {
                        final Consumer<Page> h = new Consumer<Page>() {
                            @Override public void accept(Page p) { sink.accept(WDEventNames.CONTEXT_DESTROYED.getName(), p); }
                        };
                        page.onClose(h);
                        return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Page> h = (Consumer<Page>) handler;
                            page.offClose(h);
                        }
                    });

            // PAGE: ENTRY_ADDED ~ onConsoleMessage
            enablePage(WDEventNames.ENTRY_ADDED,
                    (page, sink) -> {
                        final Consumer<ConsoleMessage> h = new Consumer<ConsoleMessage>() {
                            @Override public void accept(ConsoleMessage c) { sink.accept(WDEventNames.ENTRY_ADDED.getName(), c); }
                        };
                        page.onConsoleMessage(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<ConsoleMessage> h = (Consumer<ConsoleMessage>) handler;
                            page.offConsoleMessage(h);
                        }
                    });

            // PAGE: USER_PROMPT_OPENED ~ onDialog
            enablePage(WDEventNames.USER_PROMPT_OPENED,
                    (page, sink) -> {
                        final Consumer<Dialog> h = new Consumer<Dialog>() {
                            @Override public void accept(Dialog d) { sink.accept(WDEventNames.USER_PROMPT_OPENED.getName(), d); }
                        };
                        page.onDialog(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Dialog> h = (Consumer<Dialog>) handler;
                            page.offDialog(h);
                        }
                    });

            // PAGE: DOM_CONTENT_LOADED
            enablePage(WDEventNames.DOM_CONTENT_LOADED,
                    (page, sink) -> {
                        final Consumer<Page> h = new Consumer<Page>() {
                            @Override public void accept(Page p) { sink.accept(WDEventNames.DOM_CONTENT_LOADED.getName(), p); }
                        };
                        page.onDOMContentLoaded(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Page> h = (Consumer<Page>) handler;
                            page.offDOMContentLoaded(h);
                        }
                    });

            // PAGE: LOAD
            enablePage(WDEventNames.LOAD,
                    (page, sink) -> {
                        final Consumer<Page> h = new Consumer<Page>() {
                            @Override public void accept(Page p) { sink.accept(WDEventNames.LOAD.getName(), p); }
                        };
                        page.onLoad(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Page> h = (Consumer<Page>) handler;
                            page.offLoad(h);
                        }
                    });

            // PAGE: BEFORE_REQUEST_SENT ~ onRequest
            enablePage(WDEventNames.BEFORE_REQUEST_SENT,
                    (page, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.BEFORE_REQUEST_SENT.getName(), r); }
                        };
                        page.onRequest(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            page.offRequest(h);
                        }
                    });

            // PAGE: FETCH_ERROR ~ onRequestFailed
            enablePage(WDEventNames.FETCH_ERROR,
                    (page, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.FETCH_ERROR.getName(), r); }
                        };
                        page.onRequestFailed(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            page.offRequestFailed(h);
                        }
                    });

            // PAGE: RESPONSE_COMPLETED ~ onRequestFinished
            enablePage(WDEventNames.RESPONSE_COMPLETED,
                    (page, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.RESPONSE_COMPLETED.getName(), r); }
                        };
                        page.onRequestFinished(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            page.offRequestFinished(h);
                        }
                    });

            // PAGE: RESPONSE_STARTED ~ onResponse
            enablePage(WDEventNames.RESPONSE_STARTED,
                    (page, sink) -> {
                        final Consumer<Response> h = new Consumer<Response>() {
                            @Override public void accept(Response r) { sink.accept(WDEventNames.RESPONSE_STARTED.getName(), r); }
                        };
                        page.onResponse(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Response> h = (Consumer<Response>) handler;
                            page.offResponse(h);
                        }
                    });

            // PAGE: REALM_CREATED ~ onWorker
            enablePage(WDEventNames.REALM_CREATED,
                    (page, sink) -> {
                        final Consumer<Worker> h = new Consumer<Worker>() {
                            @Override public void accept(Worker w) { sink.accept(WDEventNames.REALM_CREATED.getName(), w); }
                        };
                        page.onWorker(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Worker> h = (Consumer<Worker>) handler;
                            page.offWorker(h);
                        }
                    });

            // PAGE: DOWNLOAD_WILL_BEGIN ~ onDownload
            enablePage(WDEventNames.DOWNLOAD_WILL_BEGIN,
                    (page, sink) -> {
                        final Consumer<Download> h = new Consumer<Download>() {
                            @Override public void accept(Download d) { sink.accept(WDEventNames.DOWNLOAD_WILL_BEGIN.getName(), d); }
                        };
                        page.onDownload(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Download> h = (Consumer<Download>) handler;
                            page.offDownload(h);
                        }
                    });

            // PAGE: FILE_DIALOG_OPENED ~ onFileChooser
            enablePage(WDEventNames.FILE_DIALOG_OPENED,
                    (page, sink) -> {
                        final Consumer<FileChooser> h = new Consumer<FileChooser>() {
                            @Override public void accept(FileChooser fc) { sink.accept(WDEventNames.FILE_DIALOG_OPENED.getName(), fc); }
                        };
                        page.onFileChooser(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<FileChooser> h = (Consumer<FileChooser>) handler;
                            page.offFileChooser(h);
                        }
                    });

            // PAGE: Frames + Popup
            enablePage(WDEventNames.CONTEXT_CREATED,
                    (page, sink) -> {
                        // wir registrieren zwei Handler unter demselben Enum: FrameAttached & Popup -> beide bei detach entfernen
                        final Consumer<Frame> h1 = new Consumer<Frame>() {
                            @Override public void accept(Frame f) { sink.accept(WDEventNames.CONTEXT_CREATED.getName(), f); }
                        };
                        page.onFrameAttached(h1);

                        final Consumer<Page> h2 = new Consumer<Page>() {
                            @Override public void accept(Page p) { sink.accept(WDEventNames.CONTEXT_CREATED.getName(), p); }
                        };
                        page.onPopup(h2);

                        // Wir returnen h1; detach behandelt BEIDE (s.u.)
                        return h1;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Frame> h1 = (Consumer<Frame>) handler;
                            page.offFrameAttached(h1);
                            // Popup-Handler kann nicht aus handler rekonstruiert werden -> defensiv nichts tun;
                            // wenn du es strikt brauchst, splitte CONTEXT_CREATED in zwei separate Einträge.
                        }
                    });

            enablePage(WDEventNames.NAVIGATION_STARTED,
                    (page, sink) -> {
                        final Consumer<Frame> h = new Consumer<Frame>() {
                            @Override public void accept(Frame f) { sink.accept(WDEventNames.NAVIGATION_STARTED.getName(), f); }
                        };
                        page.onFrameNavigated(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Frame> h = (Consumer<Frame>) handler;
                            page.offFrameNavigated(h);
                        }
                    });

            enablePage(WDEventNames.CONTEXT_DESTROYED,
                    (page, sink) -> {
                        final Consumer<Frame> h = new Consumer<Frame>() {
                            @Override public void accept(Frame f) { sink.accept(WDEventNames.CONTEXT_DESTROYED.getName(), f); }
                        };
                        page.onFrameDetached(h); return h;
                    },
                    new BiConsumer<Page, Consumer<?>>() {
                        @Override public void accept(Page page, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Frame> h = (Consumer<Frame>) handler;
                            page.offFrameDetached(h);
                        }
                    });

            return this;
        }

        public Builder withDefaultContextWiring() {
            enableContext(WDEventNames.BEFORE_REQUEST_SENT,
                    (ctx, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.BEFORE_REQUEST_SENT.getName(), r); }
                        };
                        ctx.onRequest(h); return h;
                    },
                    new BiConsumer<BrowserContext, Consumer<?>>() {
                        @Override public void accept(BrowserContext ctx, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            ctx.offRequest(h);
                        }
                    });

            enableContext(WDEventNames.FETCH_ERROR,
                    (ctx, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.FETCH_ERROR.getName(), r); }
                        };
                        ctx.onRequestFailed(h); return h;
                    },
                    new BiConsumer<BrowserContext, Consumer<?>>() {
                        @Override public void accept(BrowserContext ctx, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            ctx.offRequestFailed(h);
                        }
                    });

            enableContext(WDEventNames.RESPONSE_COMPLETED,
                    (ctx, sink) -> {
                        final Consumer<Request> h = new Consumer<Request>() {
                            @Override public void accept(Request r) { sink.accept(WDEventNames.RESPONSE_COMPLETED.getName(), r); }
                        };
                        ctx.onRequestFinished(h); return h;
                    },
                    new BiConsumer<BrowserContext, Consumer<?>>() {
                        @Override public void accept(BrowserContext ctx, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Request> h = (Consumer<Request>) handler;
                            ctx.offRequestFinished(h);
                        }
                    });

            enableContext(WDEventNames.RESPONSE_STARTED,
                    (ctx, sink) -> {
                        final Consumer<Response> h = new Consumer<Response>() {
                            @Override public void accept(Response r) { sink.accept(WDEventNames.RESPONSE_STARTED.getName(), r); }
                        };
                        ctx.onResponse(h); return h;
                    },
                    new BiConsumer<BrowserContext, Consumer<?>>() {
                        @Override public void accept(BrowserContext ctx, Consumer<?> handler) {
                            @SuppressWarnings("unchecked") Consumer<Response> h = (Consumer<Response>) handler;
                            ctx.offResponse(h);
                        }
                    });

            return this;
        }
    }

    // ---------- Static helpers ----------
    public static Builder builder() {
        return new Builder();
    }

    public static WDEventWiringConfig defaults() {
        return builder()
                .withDefaultPageWiring()
                .withDefaultContextWiring()
                .build();
    }

    private static <K extends Enum<K>, V> Map<K, V> unmodifiableEnumMap(EnumMap<K, V> src) {
        return Collections.unmodifiableMap(new EnumMap<K, V>(src));
    }
}
