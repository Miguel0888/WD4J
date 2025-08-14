function (sendMessage) {

    const SUPPRESS_FLAG = "__zrbSuppressRecording";

    // ---------- Guard & channel bridge ----------
    if (typeof sendMessage !== "function") {
        console.error("[recorder] invalid sendMessage");
        return;
    }
    function postEvents(eventsArray) {
        try {
            if (!Array.isArray(eventsArray) || eventsArray.length === 0) return;
            sendMessage({ type: "recording-event", events: eventsArray });
        } catch (e) {
            console.error("[recorder] postEvents failed:", e);
        }
    }

    // ---------- Utilities ----------
    const isHashy = (s) => typeof s === "string" && /^[A-Za-z0-9]{8,}$/.test(s);
    const isNsClass = (s) => typeof s === "string" && /^ns-[a-z0-9\-]+$/.test(s);
    const isGeneratedClass = (s) => isHashy(s) || isNsClass(s);

    function cssAttrEsc(val) {
        // single-quoted CSS attr value
        return "'" + String(val).replace(/\\/g, "\\\\").replace(/'/g, "\\'") + "'";
    }

    function collectAria(el) {
        const out = {};
        for (const a of el.attributes) {
            if (a.name.startsWith("aria-")) out[a.name] = a.value;
        }
        return Object.keys(out).length ? out : null;
    }

    function collectTestAttrs(el) {
        const out = {};
        for (const a of el.attributes) {
            if (a.name === "data-testid" || a.name.startsWith("test-")) out[a.name] = a.value;
        }
        return Object.keys(out).length ? out : null;
    }

    function collectOtherAttrs(el) {
        const keys = ["type", "maxlength", "autocomplete"];
        const out = {};
        for (const k of keys) {
            const v = el.getAttribute(k);
            if (v != null) out[k] = v;
        }
        // keep other data-* (but skip testing keys)
        for (const a of el.attributes) {
            if (a.name.startsWith("data-") && a.name !== "data-testid") {
                out[a.name] = a.value;
            }
        }
        return Object.keys(out).length ? out : null;
    }

    function absoluteXPath(el) {
        if (!el || el.nodeType !== 1) return "";
        if (el.id) return "//*[@id='" + el.id.replace(/'/g, "\\'") + "']";
        const parts = [];
        for (; el && el.nodeType === 1; el = el.parentNode) {
            let index = 1;
            let sib = el.previousElementSibling;
            while (sib) {
                if (sib.nodeType === 1 && sib.tagName === el.tagName) index++;
                sib = sib.previousElementSibling;
            }
            parts.unshift(el.tagName.toLowerCase() + "[" + index + "]");
        }
        return "/" + parts.join("/");
    }

    function classesOf(el) {
        if (!el || !el.classList) return null;
        const arr = Array.from(el.classList).filter(c => !isGeneratedClass(c));
        return arr.length ? arr.join(" ") : null;
    }

    function closestAncestorWithId(el, { excludeSelf = true } = {}) {
        let cur = excludeSelf ? el.parentElement : el;
        while (cur) {
            if (cur.nodeType === 1 && cur.id) return cur;
            cur = cur.parentElement;
        }
        return null;
    }

    // ---------- CSS builder (id-frei, optional relativ zu Anker) ----------
    function idFreeCssForGeneric(el, maxDepth = 4, stopAt = null) {
        // Kompakter, stabiler Pfad ohne IDs; wenn stopAt gesetzt, endet der Pfad VOR stopAt.
        const chain = [];
        let cur = el;
        let depth = 0;

        while (cur && cur.nodeType === 1 && depth < maxDepth) {
            if (stopAt && cur === stopAt) break;

            const tag = cur.tagName.toLowerCase();
            let piece = tag;

            // stabile Klassen (max 2)
            const stable = (cur.classList ? Array.from(cur.classList).filter(c => !isGeneratedClass(c)) : []);
            if (stable.length) piece += "." + stable.slice(0, 2).join(".");

            // role/label
            const role = cur.getAttribute && cur.getAttribute("role");
            if (role) piece += "[role=" + cssAttrEsc(role) + "]";
            if (cur.hasAttribute && cur.hasAttribute("data-label")) {
                piece += "[data-label=" + cssAttrEsc(cur.getAttribute("data-label")) + "]";
            }
            if (cur.hasAttribute && cur.hasAttribute("aria-label")) {
                piece += "[aria-label=" + cssAttrEsc(cur.getAttribute("aria-label")) + "]";
            }

            // disambiguate siblings of same tag
            const siblings = cur.parentElement ? Array.from(cur.parentElement.children).filter(n => n.tagName === cur.tagName) : [];
            if (siblings.length > 1) {
                let idx = 1;
                for (let s = cur.previousElementSibling; s; s = s.previousElementSibling) {
                    if (s.tagName === cur.tagName) idx++;
                }
                piece += ":nth-of-type(" + idx + ")";
            }

            chain.unshift(piece);

            // early stop bei klaren Widget-Roots
            if (!stopAt && cur.matches && (
                cur.matches(".ui-selectonemenu[role='combobox']") ||
                cur.matches("nav,[role='navigation']") ||
                cur.matches("table,[role='table']")
            )) break;

            cur = cur.parentElement;
            depth++;
        }

        return chain.join(" > ");
    }

    function suggestParentCss(el) {
        // Nützlicher Fallback, wenn kein Parent mit ID existiert
        const som = el.closest(".ui-selectonemenu[role='combobox']");
        if (som) return ".ui-selectonemenu[role='combobox']";
        const dlg = el.closest("[role='dialog'], .ui-dialog");
        if (dlg) return "[role='dialog'], .ui-dialog";
        const nav = el.closest("nav,[role='navigation']");
        if (nav) return "nav,[role='navigation']";
        const table = el.closest("table,[role='table']");
        if (table) return "table,[role='table']";
        const form = el.closest("form");
        if (form) return "form";
        // generisch: nimm den nächstgelegenen stabilen Vorfahren als CSS
        const anc = el.parentElement;
        return anc ? idFreeCssForGeneric(anc, 3) : null;
    }

    // ---------- PrimeFaces selectOneMenu helpers ----------
    function isSelectOneMenuRoot(el) {
        return el && el.matches && el.matches(".ui-selectonemenu[role='combobox']");
    }
    function findSelectOneMenuRoot(el) {
        return el ? el.closest(".ui-selectonemenu[role='combobox']") : null;
    }
    function isSelectOneMenuTrigger(el) {
        return el && el.matches && el.matches(".ui-selectonemenu-trigger");
    }
    function isSelectOneMenuOption(el) {
        return el && el.matches && el.matches("li.ui-selectonemenu-item[role='option']");
    }
    function isSelectOneMenuList(el) {
        return el && el.matches && el.matches("ul.ui-selectonemenu-items[role='listbox']");
    }

    // relative Selektoren (ohne Anker)
    function cssForSoMTrigger() {
        return ".ui-selectonemenu-trigger";
    }
    function cssForSoMOption(li) {
        const label = li.getAttribute("data-label") || li.textContent.trim();
        return "li.ui-selectonemenu-item[role='option'][data-label=" + cssAttrEsc(label) + "]";
    }

    // ---------- table helpers ----------
    function extractRowColumns(el) {
        const tr = el.closest("tr");
        if (!tr) return null;
        const cols = Array.from(tr.querySelectorAll("td"))
            .map(td => (td.textContent || "").trim())
            .filter(t => t.length > 0);
        return cols.length ? JSON.stringify(cols) : null;
    }

    // ---------- DTO ----------
    function createEventDTO() {
        return {
            selector: null,
            action: null,
            value: null,
            key: null,
            extractedValues: {},
            inputName: null,
            buttonText: null,
            pagination: null,
            elementId: null,   // ID des Elements selbst (nur wenn vorhanden)
            parentId: null,    // NEU: ID des nächsten übergeordneten Elements mit ID
            parentCss: null,   // NEU: CSS-Anker, falls keine parentId existiert
            classes: null,
            xpath: null,
            aria: null,
            attributes: null,
            test: null
        };
    }

    // helper: nur echte, direkt klickbare IDs zulassen
    function isDirectId(el) {
        if (!el || !el.id) return false;
        // NIE den SoM-Container selbst
        if (el.matches(".ui-selectonemenu[role='combobox']")) return false;
        // direkt klickbare/ansprechbare Elemente
        if (el.matches("input, select, textarea, button, a[href], [role='button'], [role='menuitem']")) return true;
        // PrimeFaces-SoM Teile
        if (el.matches(".ui-selectonemenu-label, .ui-selectonemenu-trigger, li.ui-selectonemenu-item[role='option']")) return true;
        return false;
    }

    function buildDtoForEvent(nativeEvent) {
        // nur echte Interaktoren (Root NICHT absichtlich drin)
        const target = nativeEvent.target;
        const INTERACTIVE_SEL = [
            "button",
            "a[href]",
            "input",
            "select",
            "textarea",
            "[role='button']",
            "[role='menuitem']",
            "li.ui-selectonemenu-item[role='option']",
            ".ui-selectonemenu-trigger",
            ".ui-selectonemenu-label",
            ".ui-autocomplete",
            ".ui-dropdown",
            "td",
            "tr"
        ].join(", ");
        let el = target.closest(INTERACTIVE_SEL) || target;

        const dto = createEventDTO();

        // action
        if (nativeEvent.type === "input" || nativeEvent.type === "change") dto.action = "input";
        else if (nativeEvent.type === "keydown") { dto.action = "press"; dto.key = nativeEvent.key || null; }
        else dto.action = "click";

        // element basics
        dto.elementId = isDirectId(el) ? el.id : null;   // << nur direkte ID!
        dto.classes = classesOf(el);
        dto.xpath = absoluteXPath(el);
        dto.aria = collectAria(el);
        dto.attributes = collectOtherAttrs(el);
        dto.test = collectTestAttrs(el);

        if (el.tagName === "INPUT" || el.tagName === "SELECT" || el.tagName === "TEXTAREA") {
            dto.inputName = el.name || null;
            if (dto.action === "input") dto.value = el.value ?? null;
        }

        // buttons / links visible text
        if (el.tagName === "BUTTON" || el.getAttribute("role") === "button") {
            const t = (el.textContent || "").trim();
            if (t) dto.buttonText = t;
        }

        const nav = el.closest("[role='navigation']");
        if (nav) dto.pagination = nav.getAttribute("aria-label") || "navigation";

        // table row columns
        const columns = extractRowColumns(el);
        if (columns) dto.extractedValues.columns = columns;

        // ---------- PrimeFaces selectOneMenu: Trigger ----------
        if (isSelectOneMenuTrigger(el)) {
            const root = findSelectOneMenuRoot(el) || el.closest(".ui-selectonemenu");
            dto.selector = cssForSoMTrigger(); // RELATIV
            if (root && root.id) dto.parentId = root.id; else dto.parentCss = ".ui-selectonemenu[role='combobox']";
            dto.extractedValues.widget = "selectOneMenu";
            if (root) {
                const labelEl = root.querySelector(".ui-selectonemenu-label");
                if (labelEl) dto.extractedValues.displayLabel = (labelEl.textContent || "").trim();
            }
            return dto;
        }

        // ---------- PrimeFaces selectOneMenu: Option ----------
        if (isSelectOneMenuOption(el)) {
            dto.selector = cssForSoMOption(el); // RELATIV zur UL
            dto.extractedValues.widget = "selectOneMenu";
            dto.extractedValues.itemLabel = el.getAttribute("data-label") || (el.textContent || "").trim();

            const list = el.closest("ul.ui-selectonemenu-items[role='listbox']");
            if (list && list.id) dto.parentId = list.id; else dto.parentCss = "ul.ui-selectonemenu-items[role='listbox']";

            if (list) {
                const items = Array.from(list.querySelectorAll("li.ui-selectonemenu-item[role='option']"));
                const idx = items.indexOf(el);
                if (idx >= 0) dto.extractedValues.itemIndex = idx;
                dto.extractedValues.listboxId = list.id || null; // raw id
            }

            const panel = list ? list.closest(".ui-selectonemenu-panel") : null;
            const comboboxId = panel ? (panel.id || "").replace(/_panel$/, "") : null;
            if (comboboxId) dto.extractedValues.comboboxId = comboboxId;

            return dto;
        }

        // ---------- PrimeFaces selectOneMenu: Root (Labelbereich) ----------
        if (isSelectOneMenuRoot(el)) {
            dto.elementId = null; // Root-ID NIE als elementId verwenden!
            const parentWithId = closestAncestorWithId(el, { excludeSelf: false }); // kann el selbst sein
            if (parentWithId && parentWithId.id) {
                dto.parentId = parentWithId.id;
                dto.selector = idFreeCssForGeneric(el.querySelector(".ui-selectonemenu-label") || el, 3, parentWithId)
                    || ".ui-selectonemenu-label";
            } else {
                dto.parentCss = ".ui-selectonemenu[role='combobox']";
                dto.selector = ".ui-selectonemenu-label";
            }
            dto.extractedValues.widget = "selectOneMenu";
            const labelEl = el.querySelector(".ui-selectonemenu-label");
            if (labelEl) dto.extractedValues.displayLabel = (labelEl.textContent || "").trim();
            dto.extractedValues.comboboxId = el.id || null;
            return dto;
        }

        // ---------- generic case ----------
        const parentWithId = closestAncestorWithId(el, { excludeSelf: true });
        if (parentWithId && parentWithId.id) {
            dto.parentId = parentWithId.id;
            dto.selector = idFreeCssForGeneric(el, 4, parentWithId) || idFreeCssForGeneric(el);
        } else {
            dto.selector = idFreeCssForGeneric(el);
            dto.parentCss = suggestParentCss(el);
        }

        return dto;
    }

    // ---------- Debounce duplicate events ----------
    let lastSig = null;
    let lastTs = 0;
    function sigFromDto(dto) {
        // build a light signature for de-dup
        return [
            dto.action || "",
            dto.selector || "",
            dto.elementId || "",
            dto.value || "",
            dto.key || "",
            dto.parentId || "",
            dto.parentCss || ""
        ].join("|");
    }
    function shouldEmit(sig) {
        const now = Date.now();
        if (sig === lastSig && (now - lastTs) < 100) return false; // 100ms debounce
        lastSig = sig; lastTs = now;
        return true;
    }

    // ---------- Listener core ----------
    function onAnyEvent(nativeEvent) {
        try {
            if (window[SUPPRESS_FLAG]) return;         // Replays nicht loggen
            if (nativeEvent.type === "click") {
                if (!nativeEvent.isTrusted || nativeEvent.detail === 0) return; // keine synthetischen Klicks
            }
            const dto = buildDtoForEvent(nativeEvent);
            const sig = sigFromDto(dto);
            if (!shouldEmit(sig)) return;
            postEvents([compact(dto)]);
        } catch (e) {
            console.error("[recorder] onAnyEvent error:", e);
        }
    }

    // remove nulls
    function compact(obj) {
        const out = {};
        for (const [k, v] of Object.entries(obj)) {
            if (v == null) continue;
            if (typeof v === "object" && !Array.isArray(v)) {
                const sub = compact(v);
                if (Object.keys(sub).length) out[k] = sub;
            } else {
                out[k] = v;
            }
        }
        return out;
    }

    function bindInteractiveListeners(root) {
        const qs = "button, a, input, select, textarea, [role='button'], [role='menuitem'], .ui-selectonemenu, .ui-selectonemenu-trigger, .ui-autocomplete, .ui-dropdown, td, tr";
        const els = (root || document).querySelectorAll(qs);
        els.forEach(el => {
            el.removeEventListener("click", onAnyEvent, true);
            el.addEventListener("click", onAnyEvent, true);

            el.removeEventListener("change", onAnyEvent, true);
            el.addEventListener("change", onAnyEvent, true);

            // KEIN input/keydown hier – das läuft global!
        });
    }

    // ---------- PrimeFaces AJAX rebind ----------
    function hookPrimeFacesAjax() {
        if (!window.PrimeFaces || !window.PrimeFaces.ajax || !window.PrimeFaces.ajax.Queue) return;
        try {
            const q = window.PrimeFaces.ajax.Queue;
            const origAdd = q.add, origRemove = q.remove;
            q.add = function(cfg) {
                return origAdd.apply(this, arguments);
            };
            q.remove = function(cfg) {
                setTimeout(() => bindInteractiveListeners(document), 0);
                return origRemove.apply(this, arguments);
            };
            bindInteractiveListeners(document);
        } catch (e) {
            console.warn("[recorder] PF hook failed:", e);
        }
    }

    // ---------- Mutation observer (rebinding safety net) ----------
    let mo;
    function startObserver() {
        try {
            mo = new MutationObserver(muts => {
                let needRebind = false;
                for (const m of muts) {
                    if (m.type === "childList" && (m.addedNodes && m.addedNodes.length)) {
                        needRebind = true; break;
                    }
                }
                if (needRebind) bindInteractiveListeners(document);
            });
            mo.observe(document.documentElement || document.body, { childList: true, subtree: true });
        } catch (e) {
            console.warn("[recorder] MO failed:", e);
        }
    }

    // ---------- Init ----------
    function init() {
        // 1) Elementweise nur CLICK binden (bitte bindInteractiveListeners entsprechend anpassen, s.u.)
        bindInteractiveListeners(document);
        hookPrimeFacesAjax();
        startObserver();

        // 2) Fallback nur für Klicks (onAnyEvent filtert synthetische bereits)
        document.addEventListener("click", onAnyEvent, true);

        // --- Hilfen ---
        const lastVal = new WeakMap(); // Element -> zuletzt geloggter Wert
        const isEditable = t =>
            t instanceof HTMLInputElement || t instanceof HTMLTextAreaElement || t.isContentEditable;

        function snapshotAndEmitInput(target, srcEvent) {
            const v = (target.value ?? target.textContent ?? "") + "";
            if (lastVal.get(target) === v) return;      // nichts Neues → nichts senden
            const dto = buildDtoForEvent(srcEvent);
            dto.action = "input";
            dto.value = v;
            postEvents([compact(dto)]);
            lastVal.set(target, v);
        }

        // 3) Feingranular: einzelne Edit-Operationen (optional nützlich zum Debuggen)
        document.addEventListener("beforeinput", e => {
            if (window[SUPPRESS_FLAG]) return;
            const t = e.target;
            if (!e.isTrusted || !isEditable(t)) return;

            const dto = buildDtoForEvent(e);
            dto.action = "type";                 // einzelne Edit-Operation
            dto.key = null;
            dto.value = e.data ?? "";            // bei Löschungen null → ""
            dto.inputType = e.inputType || "";
            postEvents([compact(dto)]);
            // lastVal NICHT hier updaten – der Feld-Snapshot kommt über input/change/Commit
        }, true);

        // 4) Snapshot nach jeder echten Eingabe
        document.addEventListener("input", e => {
            if (window[SUPPRESS_FLAG]) return;
            const t = e.target;
            if (!e.isTrusted || !isEditable(t)) return;
            snapshotAndEmitInput(t, e);
        }, true);

        // 5) Commit-Snapshot auf Blur/Change (viele Frameworks übernehmen hier erst)
        document.addEventListener("change", e => {
            if (window[SUPPRESS_FLAG]) return;
            const t = e.target;
            if (!e.isTrusted || !isEditable(t)) return;
            snapshotAndEmitInput(t, e);
        }, true);

        // 6) Sondertasten + Commit vor Enter/Tab
        const SPECIAL_KEYS = new Set([
            "Enter","Tab","Escape","Backspace","Delete","Home","End",
            "ArrowLeft","ArrowRight","ArrowUp","ArrowDown","PageUp","PageDown"
        ]);

        document.addEventListener("keydown", e => {
            if (window[SUPPRESS_FLAG]) return;
            if (!e.isTrusted) return;

            const t = e.target;
            // **WICHTIG**: vor Enter/Tab den aktuellen Feldwert aufnehmen,
            // falls das Widget bisher keine input/change-Events gefeuert hat.
            if (isEditable(t) && (e.key === "Enter" || e.key === "Tab")) {
                snapshotAndEmitInput(t, e);
            }

            if (!SPECIAL_KEYS.has(e.key)) return;       // normale Zeichen nicht hier
            const dto = buildDtoForEvent(e);
            dto.action = "press";
            dto.key = e.key;
            postEvents([compact(dto)]);
        }, true);

        // 7) Keyup-Fallback: manche Masken feuern weder input noch change beim Tippen
        document.addEventListener("keyup", e => {
            if (window[SUPPRESS_FLAG]) return;
            if (!e.isTrusted) return;
            const t = e.target;
            if (!isEditable(t)) return;
            if (SPECIAL_KEYS.has(e.key)) return;        // Sondertasten ignorieren
            snapshotAndEmitInput(t, e);                 // nur wenn sich der Wert geändert hat, wird gesendet
        }, true);

        console.log("[recorder] ready");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
}
