// Provide hover-based selector overlay with a stable public API.
// Expose: window.toggleTooltip(enabled:boolean)
// No dependency on recorder.js; keep state per realm.
/* eslint-disable no-var */
(function () {
    'use strict';

    // Keep single instance per realm
    var STATE_KEY = '__zrb_selectorOverlay';
    var state = window[STATE_KEY];
    if (!state) {
        state = {
            enabled: false,
            overlay: null,
            labelEl: null,
            onMove: null,
            lastTs: 0
        };
        window[STATE_KEY] = state;
    }

    // ---- heuristics for stable selectors (id > classes > generic chain) ----
    function isHashy(s) {
        return typeof s === 'string' && /^[A-Za-z0-9]{8,}$/.test(s);
    }
    function isNsClass(s) {
        return typeof s === 'string' && /^ns-[a-z0-9\-]+$/.test(s);
    }
    function isGeneratedId(id) {
        return isHashy(id);
    }
    function isGeneratedClass(cls) {
        return isHashy(cls) || isNsClass(cls);
    }

    function cssAttrEsc(val) {
        return "'" + String(val).replace(/\\/g, "\\\\").replace(/'/g, "\\'") + "'";
    }

    // Build a compact, stable selector for an element
    function buildSelector(el, maxDepth) {
        if (!el || el.nodeType !== 1) return '';
        // Prefer non-generated id
        if (el.id && !isGeneratedId(el.id)) {
            return '#' + el.id;
        }

        var chain = [];
        var cur = el;
        var depth = 0;
        var limit = typeof maxDepth === 'number' ? maxDepth : 5;

        while (cur && cur.nodeType === 1 && depth < limit) {
            var tag = cur.tagName.toLowerCase();
            var piece = tag;

            // stable classes (max 2)
            if (cur.classList && cur.classList.length) {
                var classes = [];
                for (var i = 0; i < cur.classList.length && classes.length < 2; i++) {
                    var c = cur.classList.item(i);
                    if (!isGeneratedClass(c)) classes.push(c);
                }
                if (classes.length) piece += '.' + classes.join('.');
            }

            // Accessibility hints help stability
            var role = cur.getAttribute && cur.getAttribute('role');
            if (role) piece += '[role=' + cssAttrEsc(role) + ']';
            if (cur.hasAttribute && cur.hasAttribute('aria-label')) {
                var al = cur.getAttribute('aria-label');
                if (al) piece += '[aria-label=' + cssAttrEsc(al) + ']';
            }

            // Disambiguate same-tag siblings
            var parent = cur.parentElement;
            if (parent) {
                var totalSame = 0;
                for (var k = 0; k < parent.children.length; k++) {
                    if (parent.children[k].tagName === cur.tagName) totalSame++;
                }
                if (totalSame > 1) {
                    var nth = 1;
                    var sib = cur;
                    while ((sib = sib.previousElementSibling) != null) {
                        if (sib.tagName === cur.tagName) nth++;
                    }
                    piece += ':nth-of-type(' + nth + ')';
                }
            }

            chain.unshift(piece);

            // Early stop on obvious widget roots
            if (cur.matches && (
                cur.matches('.ui-selectonemenu[role="combobox"]') ||
                cur.matches('nav,[role="navigation"]') ||
                cur.matches('table,[role="table"]')
            )) {
                break;
            }

            cur = parent;
            depth++;
        }

        return chain.join(' > ');
    }

    // Ultimate fallback when all else fails
    function absoluteXPath(el) {
        if (!el || el.nodeType !== 1) return '';
        if (el.id) return "//*[@id='" + el.id.replace(/'/g, "\\'") + "']";
        var parts = [];
        for (; el && el.nodeType === 1; el = el.parentNode) {
            var index = 1;
            var sib = el;
            while ((sib = sib.previousElementSibling) != null) {
                if (sib.nodeType === 1 && sib.tagName === el.tagName) index++;
            }
            parts.unshift(el.tagName.toLowerCase() + '[' + index + ']');
        }
        return '/' + parts.join('/');
    }

    // ---- overlay creation and update ----
    function ensureOverlay() {
        if (state.overlay) return;

        var box = document.createElement('div');
        box.setAttribute('id', '__zrb_selector_overlay');
        box.style.position = 'absolute';
        box.style.pointerEvents = 'none';
        box.style.border = '2px dashed #00f';
        box.style.background = 'rgba(0,0,255,0.08)';
        box.style.zIndex = '2147483647';
        box.style.display = 'none';

        var label = document.createElement('div');
        label.setAttribute('data-role', 'selector-label');
        label.style.position = 'absolute';
        label.style.top = '-20px';
        label.style.left = '0';
        label.style.font = '12px/1.2 monospace';
        label.style.padding = '2px 4px';
        label.style.background = 'rgba(0,0,0,0.7)';
        label.style.color = '#fff';
        label.style.whiteSpace = 'nowrap';
        label.style.maxWidth = '800px';
        label.style.overflow = 'hidden';
        label.style.textOverflow = 'ellipsis';

        box.appendChild(label);
        (document.documentElement || document.body).appendChild(box);

        state.overlay = box;
        state.labelEl = label;
    }

    function updateOverlayFor(el, pageX, pageY) {
        if (!el || !state.overlay) return;

        // Compute rect and selector text
        var rect = el.getBoundingClientRect();
        var selector = buildSelector(el, 5);
        if (!selector) selector = absoluteXPath(el) || (el.tagName ? el.tagName.toLowerCase() : 'unknown');

        // Position box around target
        state.overlay.style.left = Math.max(0, rect.left + window.scrollX) + 'px';
        state.overlay.style.top = Math.max(0, rect.top + window.scrollY) + 'px';
        state.overlay.style.width = Math.max(0, rect.width) + 'px';
        state.overlay.style.height = Math.max(0, rect.height) + 'px';
        state.overlay.style.display = 'block';

        // Update label at mouse position (slightly offset)
        if (state.labelEl) {
            state.labelEl.textContent = selector;
            var lx = pageX + 10;
            var ly = pageY + 10;
            // Keep label within viewport width
            var vw = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
            if (lx + 300 > vw) lx = Math.max(0, vw - 300);
            state.labelEl.style.transform = 'translate(' + (lx - (rect.left + window.scrollX)) + 'px,' + (ly - (rect.top + window.scrollY)) + 'px)';
        }
    }

    function hideOverlay() {
        if (state.overlay) {
            state.overlay.style.display = 'none';
            if (state.labelEl) state.labelEl.textContent = '';
        }
    }

    // ---- mouse move binding with throttling ----
    function onMouseMove(e) {
        var now = Date.now();
        if (now - state.lastTs < 30) return; // ~33 fps
        state.lastTs = now;

        var tgt = e.target && e.target.nodeType === 1 ? e.target : document.elementFromPoint(e.clientX, e.clientY);
        if (!tgt || tgt.nodeType !== 1) return;

        updateOverlayFor(tgt, e.pageX, e.pageY);
    }

    function bindMouse() {
        if (state.onMove) return;
        state.onMove = onMouseMove;
        // Use capture to avoid libraries stopping propagation
        window.addEventListener('mousemove', state.onMove, true);
    }

    function unbindMouse() {
        if (!state.onMove) return;
        window.removeEventListener('mousemove', state.onMove, true);
        state.onMove = null;
    }

    // ---- public API (expected by Java side) ----
    window.toggleTooltip = function (enabled) {
        var want = !!enabled;
        if (want === state.enabled) return;

        state.enabled = want;
        ensureOverlay();

        if (state.enabled) {
            bindMouse();
        } else {
            unbindMouse();
            hideOverlay();
        }
    };
})();
