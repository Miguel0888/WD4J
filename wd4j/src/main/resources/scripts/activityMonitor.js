function (sendMessage) {
    if (typeof sendMessage !== "function") return;
    if (window.__zrbActivityInstalled) return;
    window.__zrbActivityInstalled = true;

    const state = {
        actionSeq: 0,
        lastActionTs: 0,
        lastChangeTs: 0,
        inflightXHR: 0,
        inflightFetch: 0,
        pfQueueDepth: 0,
        lastDomContentLoaded: 0,
        lastLoad: 0
    };

    const compact = (o) => {
        const out = {};
        for (const k in o) if (o[k] != null) out[k] = o[k];
        return out;
    };
    const emit = () => {
        state.lastChangeTs = Date.now();
        try { sendMessage({ type: "activity-event", data: compact(state) }); } catch (_) {}
    };

    // Public API for Java side
    window.__zrbMarkAction = function (seq) {
        state.actionSeq = typeof seq === "number" ? seq : (state.actionSeq + 1);
        state.lastActionTs = Date.now();
        emit();
        return state.actionSeq;
    };
    window.__zrbGetActivity = function () { return compact(state); };

    // DOM milestones
    document.addEventListener("DOMContentLoaded", function () {
        state.lastDomContentLoaded = Date.now(); emit();
    }, { once: false });
    window.addEventListener("load", function () {
        state.lastLoad = Date.now(); emit();
    }, true);

    // MutationObserver as "visual churn" proxy
    try {
        const mo = new MutationObserver(function (muts) {
            for (let i = 0; i < muts.length; i++) {
                const m = muts[i];
                if ((m.addedNodes && m.addedNodes.length) || (m.removedNodes && m.removedNodes.length)) {
                    break;
                }
            }
            emit();
        });
        mo.observe(document.documentElement || document, { childList: true, subtree: true });
    } catch (_) {}

    // XHR hook
    try {
        const OrigXHR = window.XMLHttpRequest;
        function PatchedXHR() {
            const xhr = new OrigXHR();
            xhr.addEventListener("loadstart", function(){ state.inflightXHR++; emit(); }, true);
            xhr.addEventListener("loadend", function(){ if (state.inflightXHR>0) state.inflightXHR--; emit(); }, true);
            return xhr;
        }
        PatchedXHR.prototype = OrigXHR.prototype;
        window.XMLHttpRequest = PatchedXHR;
    } catch (_) {}

    // fetch hook
    try {
        const origFetch = window.fetch;
        if (typeof origFetch === "function") {
            window.fetch = function() {
                state.inflightFetch++; emit();
                return origFetch.apply(this, arguments).then(
                    (res) => { state.inflightFetch--; emit(); return res; },
                    (err) => { state.inflightFetch--; emit(); throw err; }
                );
            };
        }
    } catch (_) {}

    // PrimeFaces queue depth (best effort)
    function refreshPfQueue() {
        try {
            const q = window.PrimeFaces && window.PrimeFaces.ajax && window.PrimeFaces.ajax.Queue;
            const len = q && q.requests ? q.requests.length : 0;
            if (len !== state.pfQueueDepth) { state.pfQueueDepth = len; emit(); }
        } catch (_) {}
    }
    setInterval(refreshPfQueue, 50);

    emit();
}
