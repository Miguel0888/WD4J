// recorder.js
function (sendMessage) {
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

    // ---------- CSS (id-frei) ----------
    function idFreeCssForGeneric(el, maxDepth = 4) {
        // Build a compact, id-freier Pfad mit Klassen/role/aria/data-*
        const chain = [];
        let cur = el;
        let depth = 0;

        while (cur && cur.nodeType === 1 && depth < maxDepth) {
            const tag = cur.tagName.toLowerCase();

            // prefer semantic anchors
            let piece = tag;

            // stable classes (max 2)
            const stable = (cur.classList ? Array.from(cur.classList).filter(c => !isGeneratedClass(c)) : []);
            if (stable.length) piece += "." + stable.slice(0, 2).join(".");

            // role/landmark
            const role = cur.getAttribute && cur.getAttribute("role");
            if (role) piece += "[role=" + cssAttrEsc(role) + "]";

            // specific data-* that define structure (not tests)
            if (cur.hasAttribute && cur.hasAttribute("data-label")) {
                piece += "[data-label=" + cssAttrEsc(cur.getAttribute("data-label")) + "]";
            }
            if (cur.hasAttribute && cur.hasAttribute("aria-label")) {
                piece += "[aria-label=" + cssAttrEsc(cur.getAttribute("aria-label")) + "]";
            }

            // disambiguate siblings of same tag/class
            const siblings = cur.parentElement ? Array.from(cur.parentElement.children).filter(n => n.tagName === cur.tagName) : [];
            if (siblings.length > 1) {
                let idx = 1;
                for (let s = cur.previousElementSibling; s; s = s.previousElementSibling) {
                    if (s.tagName === cur.tagName) idx++;
                }
                piece += ":nth-of-type(" + idx + ")";
            }

            chain.unshift(piece);

            // stop early on clear widget roots
            if (cur.matches && (
                cur.matches(".ui-selectonemenu[role='combobox']") ||
                cur.matches("nav,[role='navigation']") ||
                cur.matches("table,[role='table']")
            )) break;

            cur = cur.parentElement;
            depth++;
        }

        return chain.join(" > ");
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

    function cssForSoMTrigger(el) {
        // id-frei → Root + trigger
        return ".ui-selectonemenu[role='combobox'] .ui-selectonemenu-trigger";
    }
    function cssForSoMOption(li) {
        const label = li.getAttribute("data-label") || li.textContent.trim();
        const hasList = li.closest("ul.ui-selectonemenu-items[role='listbox']");
        const listSel = hasList ? "ul.ui-selectonemenu-items[role='listbox']" : "ul.ui-selectonemenu-items";
        return `${listSel} li.ui-selectonemenu-item[role='option'][data-label=${cssAttrEsc(label)}]`;
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
            elementId: null,
            classes: null,
            xpath: null,
            aria: null,
            attributes: null,
            test: null
        };
    }

    // ---------- Build DTO from event target ----------
    function buildDtoForEvent(nativeEvent) {
        // find meaningful interactive element
        const target = nativeEvent.target;
        let el = target.closest("button, a, input, select, textarea, [role='button'], [role='menuitem'], li, .ui-selectonemenu, .ui-selectonemenu-trigger, .ui-autocomplete, .ui-dropdown, td, tr") || target;

        const dto = createEventDTO();

        // action
        if (nativeEvent.type === "input" || nativeEvent.type === "change") dto.action = "input";
        else if (nativeEvent.type === "keydown") { dto.action = "press"; dto.key = nativeEvent.key || null; }
        else dto.action = "click";

        // element basics
        dto.elementId = el.id || null;
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

        // ---------- PrimeFaces selectOneMenu special cases ----------
        if (isSelectOneMenuTrigger(el)) {
            const root = findSelectOneMenuRoot(el) || el.closest(".ui-selectonemenu");
            dto.selector = cssForSoMTrigger(el); // id-frei
            if (root) {
                dto.extractedValues.widget = "selectOneMenu";
                dto.extractedValues.comboboxId = root.id || null; // raw id, extra info
                const labelEl = root.querySelector(".ui-selectonemenu-label");
                if (labelEl) dto.extractedValues.displayLabel = (labelEl.textContent || "").trim();
            }
            return dto;
        }

        if (isSelectOneMenuOption(el)) {
            dto.selector = cssForSoMOption(el); // id-frei, data-label basiert
            dto.extractedValues.widget = "selectOneMenu";
            dto.extractedValues.itemLabel = el.getAttribute("data-label") || (el.textContent || "").trim();
            // index
            const list = el.closest("ul.ui-selectonemenu-items[role='listbox']");
            if (list) {
                const items = Array.from(list.querySelectorAll("li.ui-selectonemenu-item[role='option']"));
                const idx = items.indexOf(el);
                if (idx >= 0) dto.extractedValues.itemIndex = idx;
                dto.extractedValues.listboxId = list.id || null; // raw id
            }
            // combobox context (if resolvable)
            const panel = list ? list.closest(".ui-selectonemenu-panel") : null;
            const comboboxId = panel ? (panel.id || "").replace(/_panel$/, "") : null;
            if (comboboxId) dto.extractedValues.comboboxId = comboboxId;
            return dto;
        }

        if (isSelectOneMenuRoot(el)) {
            // clicking on root (e.g., label area) – still provide a solid id-free selector
            dto.selector = ".ui-selectonemenu[role='combobox'] .ui-selectonemenu-label";
            dto.extractedValues.widget = "selectOneMenu";
            const labelEl = el.querySelector(".ui-selectonemenu-label");
            if (labelEl) dto.extractedValues.displayLabel = (labelEl.textContent || "").trim();
            dto.extractedValues.comboboxId = el.id || null; // raw id
            return dto;
        }

        // ---------- generic CSS (id-frei) ----------
        dto.selector = idFreeCssForGeneric(el);

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
            dto.key || ""
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
            const dto = buildDtoForEvent(nativeEvent);
            const sig = sigFromDto(dto);
            if (!shouldEmit(sig)) return;
            // send single event as array of one (wire format)
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
            el.removeEventListener("input", onAnyEvent, true);
            el.addEventListener("input", onAnyEvent, true);
            el.removeEventListener("keydown", onAnyEvent, true);
            el.addEventListener("keydown", onAnyEvent, true);
        });
    }

    // ---------- PrimeFaces AJAX rebind ----------
    function hookPrimeFacesAjax() {
        if (!window.PrimeFaces || !window.PrimeFaces.ajax || !window.PrimeFaces.ajax.Queue) return;
        try {
            const q = window.PrimeFaces.ajax.Queue;
            const origAdd = q.add, origRemove = q.remove;
            q.add = function(cfg) {
                // console.debug("[recorder] PF ajax start", cfg);
                return origAdd.apply(this, arguments);
            };
            q.remove = function(cfg) {
                // console.debug("[recorder] PF ajax end", cfg);
                setTimeout(() => bindInteractiveListeners(document), 0);
                return origRemove.apply(this, arguments);
            };
            // initial bind
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
        bindInteractiveListeners(document);
        hookPrimeFacesAjax();
        startObserver();

        // global capture as last resort
        document.addEventListener("click", onAnyEvent, true);
        document.addEventListener("input", onAnyEvent, true);
        document.addEventListener("change", onAnyEvent, true);
        document.addEventListener("keydown", onAnyEvent, true);

        console.log("[recorder] ready");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
}
