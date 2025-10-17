function (sendMessage) {
    if (typeof sendMessage !== "function") {
        console.error("[growl] invalid sendMessage");
        return;
    }
    if (window.__zrbGrowlInstalled) return;
    window.__zrbGrowlInstalled = true;

    // --- global flags/state ---
    let PF_HOOKED = false;
    let mo = null; // MutationObserver-Handle, um ihn später abzuschalten

    // kleine Dedupe: gleiche (type|title|message) nur einmal pro 2s
    const recent = [];
    function shouldEmitOnce(dto) {
        const ttlMs = 2000;
        const key = [dto.type || "", dto.title || "", dto.message || ""].join("|");
        const t = Date.now();
        for (let i = recent.length - 1; i >= 0; i--) {
            if (t - recent[i].ts > ttlMs) recent.splice(i, 1);
        }
        if (recent.some(r => r.key === key)) return false;
        recent.push({ key: key, ts: t });
        if (recent.length > 64) recent.shift();
        return true;
    }

    // ---------- utils ----------
    const now = () => Date.now();
    const mapSeverity = (sev) => {
        const s = String(sev || "info").toLowerCase();
        if (s === "warn" || s === "warning") return "WARN";
        if (s === "error") return "ERROR";
        if (s === "fatal") return "FATAL";
        return "INFO";
    };
    const compact = (obj) => {
        const out = {};
        for (const [k, v] of Object.entries(obj || {})) {
            if (v == null) continue;
            if (typeof v === "object" && !Array.isArray(v)) {
                const sub = compact(v);
                if (Object.keys(sub).length) out[k] = sub;
            } else {
                out[k] = v;
            }
        }
        return out;
    };
    const emit = (dto) => {
        try {
            if (!shouldEmitOnce(dto)) return; // << DEDUPE
            sendMessage({ type: "growl-event", data: compact(dto) });
        } catch (e) {
            console.error("[growl] emit failed:", e);
        }
    };

    // ---------- DTO builders ----------
    const fromPFMessage = (m) => ({
        // context wird serverseitig aus event.source.context ergänzt
        type: mapSeverity(m && (m.severity || m.sev)),
        title: (m && (m.summary || m.title)) || "",
        message: (m && (m.detail || m.msg)) || "",
        timestamp: now()
    });

    const fromDomNode = (container) => {
        // Beispiel-Container:
        // <div class="ui-growl-item-container ui-state-highlight ... ui-growl-info"><div class="ui-growl-item">...</div></div>
        const cls = container.className || "";
        let sev = "info";
        if (/\bui-growl-warn\b/.test(cls)) sev = "warn";
        else if (/\bui-growl-error\b/.test(cls)) sev = "error";
        else if (/\bui-growl-fatal\b/.test(cls)) sev = "fatal";
        else if (/\bui-growl-info\b/.test(cls)) sev = "info";

        const titleEl = container.querySelector(".ui-growl-title");
        const msgEl =
            container.querySelector(".ui-growl-message > p") ||
            container.querySelector(".ui-growl-message");

        return {
            type: mapSeverity(sev),
            title: titleEl ? titleEl.textContent.trim() : "",
            message: msgEl ? msgEl.textContent.trim() : "",
            timestamp: now()
        };
    };

    // ---------- PrimeFaces hook ----------
    const hookPrimeFaces = () => {
        try {
            const proto = window.PrimeFaces
                && PrimeFaces.widget
                && PrimeFaces.widget.Growl
                && PrimeFaces.widget.Growl.prototype;
            if (!proto || proto.__zrbPatched) return;

            const origShow = proto.show;
            proto.show = function (msgs) {
                try {
                    if (Array.isArray(msgs)) {
                        for (const m of msgs) emit(fromPFMessage(m));
                    }
                } catch (e) {
                    console.warn("[growl] PF hook emit failed:", e);
                }
                return origShow.apply(this, arguments);
            };

            proto.__zrbPatched = true;
            PF_HOOKED = true;
            window.__zrbGrowlPfHooked = true;

            // wenn der PF-Hook aktiv ist, DOM-Fallback sofort abschalten
            if (mo) { try { mo.disconnect(); } catch (_) {} mo = null; }
        } catch (e) {
            console.warn("[growl] PF hook failed:", e);
        }
    };

    // ---------- DOM fallback (covers non-standard inserts) ----------
    const seen = new WeakSet();
    const scanDomOnce = () => {
        try {
            // wenn PF Hook aktiv → DOM-Fallback unterbinden
            if (PF_HOOKED || window.__zrbGrowlPfHooked) return;

            const nodes = document.querySelectorAll(".ui-growl-item-container");
            nodes.forEach((n) => {
                if (seen.has(n)) return;
                seen.add(n);
                emit(fromDomNode(n));
            });
        } catch (e) {
            // ignore
        }
    };

    const startObserver = () => {
        try {
            // wenn PF Hook bereits aktiv → keinen Observer starten
            if (PF_HOOKED || window.__zrbGrowlPfHooked) return;

            mo = new MutationObserver((muts) => {
                if (PF_HOOKED || window.__zrbGrowlPfHooked) {
                    // Falls Hook nachträglich greift, Observer aus
                    try { mo.disconnect(); } catch (_) {}
                    mo = null;
                    return;
                }
                let found = false;
                for (const m of muts) {
                    if (m.addedNodes && m.addedNodes.length) { found = true; break; }
                }
                if (found) scanDomOnce();
            });
            mo.observe(document.body || document.documentElement, { childList: true, subtree: true });
        } catch (e) {
            console.warn("[growl] MutationObserver failed:", e);
        }
    };

    // ---------- Init ----------
    const init = () => {
        hookPrimeFaces();
        // mehrfach versuchen, falls PF später lädt
        setTimeout(hookPrimeFaces, 0);
        setTimeout(hookPrimeFaces, 50);
        setTimeout(hookPrimeFaces, 250);

        // DOM-Fallback nur aktiv, solange kein PF-Hook existiert
        scanDomOnce();
        startObserver();
        // console.log("[growl] ready");
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
}
