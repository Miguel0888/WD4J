/* Rich selector hover overlay; expose window.toggleTooltip(enabled:boolean).
   Match WD4J preload contract: provide a bare function expression (not invoked here). */
function (sendMessage) {
    'use strict';

    // ---- state ----------------------------------------------------------------
    var KEY = '__zrb_selectorOverlay';
    var S = window[KEY];
    if (!S) {
        S = {
            enabled: false,
            box: null,
            label: null,
            panel: null,
            onMove: null,
            lastTs: 0
        };
        window[KEY] = S;
    }

    // ---- utils ----------------------------------------------------------------
    function isHashy(s) { return typeof s === 'string' && /^[A-Za-z0-9]{8,}$/.test(s); }
    function isNsClass(s) { return typeof s === 'string' && /^ns-[a-z0-9\-]+$/.test(s); }
    function isGenId(id) { return isHashy(id); }
    function isGenClass(c) { return isHashy(c) || isNsClass(c); }

    function textOf(el) {
        if (!el) return '';
        var t = el.innerText || el.textContent || '';
        t = String(t).replace(/\s+/g, ' ').trim();
        return t.length > 500 ? t.slice(0, 500) + '…' : t;
    }

    function collectAria(el) {
        var out = {};
        if (!el || !el.attributes) return out;
        for (var i = 0; i < el.attributes.length; i++) {
            var a = el.attributes[i];
            if (a && a.name && a.name.indexOf('aria-') === 0) out[a.name] = a.value;
        }
        return out;
    }

    function collectData(el) {
        var out = {};
        if (!el || !el.attributes) return out;
        for (var i = 0; i < el.attributes.length; i++) {
            var a = el.attributes[i];
            if (a && a.name && a.name.indexOf('data-') === 0) {
                var val = a.value;
                out[a.name] = (val && val.length > 200) ? (val.slice(0, 200) + '…') : val;
            }
        }
        return out;
    }

    function collectAttrs(el) {
        var keys = ['name','type','maxlength','autocomplete','placeholder','href','value'];
        var out = {};
        for (var i = 0; i < keys.length; i++) {
            var v = el.getAttribute ? el.getAttribute(keys[i]) : null;
            if (v != null) out[keys[i]] = v;
        }
        return out;
    }

    // Build compact CSS selector (id > classes > chain)
    function cssSelector(el, maxDepth) {
        if (!el || el.nodeType !== 1) return '';
        if (el.id && !isGenId(el.id)) return '#' + el.id;

        var chain = [], cur = el, depth = 0, lim = (typeof maxDepth === 'number' ? maxDepth : 5);
        while (cur && cur.nodeType === 1 && depth < lim) {
            var piece = cur.tagName.toLowerCase();

            if (cur.classList && cur.classList.length) {
                var keep = [];
                for (var i = 0; i < cur.classList.length && keep.length < 2; i++) {
                    var c = cur.classList.item(i);
                    if (!isGenClass(c)) keep.push(c);
                }
                if (keep.length) piece += '.' + keep.join('.');
            }

            var p = cur.parentElement;
            if (p) {
                var same = 0;
                for (var j = 0; j < p.children.length; j++) {
                    if (p.children[j].tagName === cur.tagName) same++;
                }
                if (same > 1) {
                    var nth = 1, sib = cur;
                    while ((sib = sib.previousElementSibling) != null) {
                        if (sib.tagName === cur.tagName) nth++;
                    }
                    piece += ':nth-of-type(' + nth + ')';
                }
            }

            chain.unshift(piece);

            if (cur.matches && (
                cur.matches('.ui-selectonemenu[role="combobox"]') ||
                cur.matches('nav,[role="navigation"]') ||
                cur.matches('table,[role="table"]')
            )) break;

            cur = p; depth++;
        }
        return chain.join(' > ');
    }

    // Absolute XPath as fallback
    function xPath(el) {
        if (!el || el.nodeType !== 1) return '';
        if (el.id) return "//*[@id='" + el.id.replace(/'/g, "\\'") + "']";
        var parts = [];
        for (; el && el.nodeType === 1; el = el.parentNode) {
            var idx = 1, sib = el;
            while ((sib = sib.previousElementSibling) != null) {
                if (sib.nodeType === 1 && sib.tagName === el.tagName) idx++;
            }
            parts.unshift(el.tagName.toLowerCase() + '[' + idx + ']');
        }
        return '/' + parts.join('/');
    }

    function jsonShort(obj) {
        try { return JSON.stringify(obj, null, 2); } catch(e) { return String(obj); }
    }

    // ---- DOM overlay + info panel ---------------------------------------------
    function ensureUi() {
        if (!S.box) {
            var b = document.createElement('div');
            b.id = '__zrb_selector_overlay';
            b.style.position = 'absolute';
            b.style.pointerEvents = 'none';
            b.style.border = '2px dashed #00f';
            b.style.background = 'rgba(0,0,255,0.06)';
            b.style.zIndex = '2147483647';
            b.style.display = 'none';
            (document.documentElement || document.body).appendChild(b);
            S.box = b;
        }
        if (!S.label) {
            var lbl = document.createElement('div');
            lbl.setAttribute('data-role','selector-floating');
            lbl.style.position = 'absolute';
            lbl.style.top = '-20px';
            lbl.style.left = '0';
            lbl.style.font = '12px monospace';
            lbl.style.padding = '2px 4px';
            lbl.style.background = 'rgba(0,0,0,0.7)';
            lbl.style.color = '#fff';
            lbl.style.whiteSpace = 'nowrap';
            lbl.style.maxWidth = '800px';
            lbl.style.overflow = 'hidden';
            lbl.style.textOverflow = 'ellipsis';
            S.box.appendChild(lbl);
            S.label = lbl;
        }
        if (!S.panel) {
            var p = document.createElement('div');
            p.id = '__zrb_selector_panel';
            p.style.position = 'fixed';
            p.style.right = '8px';
            p.style.top = '8px';
            p.style.maxWidth = '480px';
            p.style.maxHeight = '60vh';
            p.style.overflow = 'auto';
            p.style.font = '12px/1.35 monospace';
            p.style.background = 'rgba(0,0,0,0.75)';
            p.style.color = '#fff';
            p.style.padding = '8px 10px';
            p.style.borderRadius = '4px';
            p.style.zIndex = '2147483647';
            p.style.boxShadow = '0 2px 12px rgba(0,0,0,0.4)';
            p.style.display = 'none';
            (document.documentElement || document.body).appendChild(p);
            S.panel = p;
        }
    }

    function updateUi(el, pageX, pageY) {
        if (!el) return;
        ensureUi();

        // Box around element
        var r = el.getBoundingClientRect();
        S.box.style.left = (r.left + window.scrollX) + 'px';
        S.box.style.top  = (r.top  + window.scrollY) + 'px';
        S.box.style.width  = r.width + 'px';
        S.box.style.height = r.height + 'px';
        S.box.style.display = 'block';

        // Floating short label near cursor
        var shortSel = cssSelector(el, 5) || (el.tagName ? el.tagName.toLowerCase() : 'unknown');
        S.label.textContent = shortSel;
        var lx = pageX + 10, ly = pageY + 10;
        S.label.style.transform = 'translate(' + (lx - (r.left + window.scrollX)) + 'px,' + (ly - (r.top + window.scrollY)) + 'px)';

        // Rich panel
        var info = {};
        info.tag = el.tagName ? el.tagName.toLowerCase() : '';
        info.id = el.id || null;
        info.classList = (el.classList && el.classList.length)
            ? Array.prototype.slice.call(el.classList) : null;
        info.role = el.getAttribute ? el.getAttribute('role') : null;
        info.text = textOf(el);
        info.css = cssSelector(el, 6);
        info.xpath = xPath(el);
        var aria = collectAria(el);
        var attrs = collectAttrs(el);
        var data = collectData(el);

        // Render panel (simple, readable)
        var html = '';
        html += '<div style="font-weight:bold;margin-bottom:6px;">Selector Overlay</div>';
        html += '<div><b>Tag</b>: ' + escapeHtml(info.tag) + '</div>';
        if (info.id) html += '<div><b>ID</b>: ' + escapeHtml(info.id) + '</div>';
        if (info.classList && info.classList.length) {
            html += '<div><b>Classes</b>: ' + escapeHtml(info.classList.join(' ')) + '</div>';
        }
        if (info.role) html += '<div><b>Role</b>: ' + escapeHtml(info.role) + '</div>';
        if (info.text) html += '<div><b>Text</b>: ' + escapeHtml(info.text) + '</div>';
        html += '<div style="margin-top:6px;"><b>CSS</b>:</div><pre style="white-space:pre-wrap;">' + escapeHtml(info.css) + '</pre>';
        html += '<div><b>XPath</b>:</div><pre style="white-space:pre-wrap;">' + escapeHtml(info.xpath) + '</pre>';
        if (Object.keys(aria).length) {
            html += '<div><b>ARIA</b>:</div><pre style="white-space:pre-wrap;">' + escapeHtml(jsonShort(aria)) + '</pre>';
        }
        if (Object.keys(attrs).length) {
            html += '<div><b>Attributes</b>:</div><pre style="white-space:pre-wrap;">' + escapeHtml(jsonShort(attrs)) + '</pre>';
        }
        if (Object.keys(data).length) {
            html += '<div><b>Data</b>:</div><pre style="white-space:pre-wrap;">' + escapeHtml(jsonShort(data)) + '</pre>';
        }
        S.panel.innerHTML = html;
        S.panel.style.display = 'block';
    }

    function escapeHtml(s) {
        if (s == null) return '';
        s = String(s);
        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    function hideUi() {
        if (S.box) S.box.style.display = 'none';
        if (S.panel) S.panel.style.display = 'none';
        if (S.label) S.label.textContent = '';
    }

    // ---- mouse handling --------------------------------------------------------
    function onMove(e) {
        var now = Date.now();
        if (now - S.lastTs < 30) return; // throttle
        S.lastTs = now;

        var t = (e.target && e.target.nodeType === 1) ? e.target : document.elementFromPoint(e.clientX, e.clientY);
        if (!t || t.nodeType !== 1) return;

        updateUi(t, e.pageX, e.pageY);
    }

    function bind() {
        if (S.onMove) return;
        S.onMove = onMove;
        window.addEventListener('mousemove', S.onMove, true); // capture
    }

    function unbind() {
        if (!S.onMove) return;
        window.removeEventListener('mousemove', S.onMove, true);
        S.onMove = null;
    }

    // ---- public API ------------------------------------------------------------
    window.toggleTooltip = function (enabled) {
        var want = !!enabled;
        if (want === S.enabled) return;
        S.enabled = want;

        if (S.enabled) { bind(); } else { unbind(); hideUi(); }
    };
}
