function(){
    'use strict';

    // --- hard reset: remove any old overlays and listeners ---
    try {
        var oldIds = ['__zrb_selector_overlay','__zrb_selector_panel','__zrb_selector_label'];
        for (var i=0;i<oldIds.length;i++){
            var n=document.getElementById(oldIds[i]);
            if (n && n.parentNode) n.parentNode.removeChild(n);
        }
    } catch(e){}
    if (window.__zrbSel && window.__zrbSel.unbind) { try{ window.__zrbSel.unbind(); }catch(e){} }
    window.__zrbSel = { enabled:false, onMove:null, box:null, lbl:null, panel:null, ts:0 };

    // --- CSS.escape polyfill (handle ":" in IDs) ---
    if (typeof window.CSS === 'undefined') window.CSS = {};
    if (typeof window.CSS.escape !== 'function') {
        window.CSS.escape = function(value){
            var s=String(value), out='', i=0, ch;
            for(; i<s.length; i++){
                ch = s.charCodeAt(i);
                if (ch===0) { out+='\uFFFD'; continue; }
                if ((ch>=48&&ch<=57)||(ch>=65&&ch<=90)||(ch>=97&&ch<=122)||ch===45||ch===95) out+=s[i];
                else out += '\\' + s[i];
            }
            return out;
        };
    }

    // --- helpers (ES5 only) ---
    function isHashy(s){ return typeof s==='string' && /^[A-Za-z0-9]{8,}$/.test(s); }
    function isNsClass(s){ return typeof s==='string' && /^ns-[a-z0-9\-]+$/.test(s); }
    function isGenId(id){ return isHashy(id); }
    function isGenClass(c){ return isHashy(c)||isNsClass(c); }

    var _matches = Element.prototype.matches || Element.prototype.msMatchesSelector ||
        Element.prototype.webkitMatchesSelector || function(){return false;};
    function matches(n,sel){ try{ return n && n.nodeType===1 && _matches.call(n,sel); }catch(e){ return false; } }

    function parentOf(n){
        if(!n) return null;
        if(n.parentElement) return n.parentElement;
        var r=n.getRootNode&&n.getRootNode();
        return (r&&r.host&&r.host.nodeType===1)?r.host:null;
    }

    function textOf(el){
        if(!el) return '';
        var t = el.innerText || el.textContent || '';
        return String(t).replace(/\s+/g,' ').trim().slice(0,500);
    }

    function collectAllAttrs(el){
        var out={}, i,a;
        if(!el || !el.attributes) return out;
        for(i=0;i<el.attributes.length;i++){
            a = el.attributes[i]; if(!a) continue;
            out[a.name] = a.value;
        }
        return out;
    }
    function onlyAria(attrs){ var o={},k; for(k in attrs){ if(attrs.hasOwnProperty(k)&&k.indexOf('aria-')===0) o[k]=attrs[k]; } return o; }
    function onlyData(attrs){ var o={},k; for(k in attrs){ if(attrs.hasOwnProperty(k)&&k.indexOf('data-')===0) o[k]=attrs[k]; } return o; }
    function stripAriaData(attrs){ var o={},k; for(k in attrs){ if(attrs.hasOwnProperty(k)&&k.indexOf('aria-')!==0 && k.indexOf('data-')!==0) o[k]=attrs[k]; } return o; }

    function cssSelector(el, maxDepth){
        try{
            if(!el || el.nodeType!==1) return 'node';
            if(el.id && !isGenId(el.id)) return '#'+CSS.escape(el.id);
            var lim = typeof maxDepth==='number' ? maxDepth : 6;

            var anchor=null, a=parentOf(el);
            while(a && a.nodeType===1){
                if(a.id && !isGenId(a.id)){ anchor=a; break; }
                a = parentOf(a);
            }

            var chain=[], cur=el, depth=0;
            while(cur && cur.nodeType===1 && depth<lim){
                var piece = (cur.tagName?cur.tagName.toLowerCase():'node');

                if(cur.classList && cur.classList.length){
                    var keep=[], i=0;
                    for(; i<cur.classList.length && keep.length<2; i++){
                        var c=cur.classList.item(i);
                        if(!isGenClass(c)) keep.push(c);
                    }
                    if(keep.length) piece += '.'+ keep.map(function(x){return CSS.escape(x);}).join('.');
                }

                var p = parentOf(cur);
                if(p){
                    var same=0, j=0; for(; j<p.children.length; j++){ if(p.children[j].tagName===cur.tagName) same++; }
                    if(same>1){
                        var nth=1, sib=cur; while((sib = sib.previousElementSibling)!=null){ if(sib.tagName===cur.tagName) nth++; }
                        piece += ':nth-of-type('+nth+')';
                    }
                }

                chain.unshift(piece);

                if(cur===anchor) break;
                if(matches(cur,'.ui-selectonemenu[role="combobox"]') || matches(cur,'nav,[role="navigation"]') || matches(cur,'table,[role="table"]')) break;

                cur = p; depth++;
            }

            var sel = chain.join(' > ');
            if(anchor && anchor!==el) sel = '#'+CSS.escape(anchor.id)+' > '+sel;
            if(!sel || sel.replace(/\s|>/g,'')==='') sel = (el.tagName?el.tagName.toLowerCase():'node');
            return sel;
        }catch(e){ return (el && el.tagName ? el.tagName.toLowerCase() : 'node'); }
    }

    function xPath(el){
        try{
            if(!el || el.nodeType!==1) return '//node';
            if(el.id && !isGenId(el.id)) return "//*[@id='"+String(el.id).replace(/'/g,"\\'")+"']";
            var parts=[], cur=el;
            while(cur && cur.nodeType===1){
                var idx=1, sib=cur; while((sib=sib.previousElementSibling)!=null){ if(sib.nodeType===1 && sib.tagName===cur.tagName) idx++; }
                parts.unshift((cur.tagName?cur.tagName.toLowerCase():'node')+'['+idx+']');
                var p=cur.parentElement;
                if(!p){
                    var root=cur.getRootNode&&cur.getRootNode();
                    cur=(root&&root.host&&root.host.nodeType===1)?root.host:null;
                } else cur=p;
            }
            var xp='/'+parts.join('/');
            if(!xp || xp==='/') xp='//'+(el.tagName?el.tagName.toLowerCase():'node');
            return xp;
        }catch(e){ return '//'+(el && el.tagName ? el.tagName.toLowerCase() : 'node'); }
    }

    function topElemAt(x,y){
        var list = (document.elementsFromPoint ? document.elementsFromPoint(x,y)
            : [document.elementFromPoint(x,y)]).filter(Boolean);
        for (var i=0;i<list.length;i++){
            var el=list[i];
            if(!el || el.nodeType!==1) continue;
            if (el.id==='__zrb_selector_overlay' || el.id==='__zrb_selector_panel' || el.id==='__zrb_selector_label') continue;
            if (el.getAttribute && el.getAttribute('data-role')==='selector-floating') continue;
            return el;
        }
        return null;
    }

    // --- build UI (one overlay box, one floating label, one JSON panel) ---
    function ensureUi(){
        var S = window.__zrbSel;
        if(!S.box){
            var b=document.createElement('div');
            b.id='__zrb_selector_overlay';
            b.style.position='absolute';
            b.style.pointerEvents='none';
            b.style.border='2px dashed #00f';
            b.style.background='rgba(0,0,255,0.06)';
            b.style.zIndex='2147483647';
            b.style.display='none';
            (document.documentElement||document.body).appendChild(b);
            S.box=b;
        }
        if(!S.lbl){
            var l=document.createElement('div');
            l.id='__zrb_selector_label';
            l.style.position='absolute';
            l.style.top='-20px'; l.style.left='0';
            l.style.font='12px monospace';
            l.style.padding='2px 4px';
            l.style.background='rgba(0,0,0,0.8)'; l.style.color='#fff';
            l.style.whiteSpace='nowrap'; l.style.maxWidth='800px';
            l.style.overflow='hidden'; l.style.textOverflow='ellipsis';
            S.box.appendChild(l); S.lbl=l;
        }
        if(!S.panel){
            var p=document.createElement('pre'); // show JSON only
            p.id='__zrb_selector_panel';
            p.style.position='fixed';
            p.style.right='8px'; p.style.top='8px';
            p.style.maxWidth='560px'; p.style.maxHeight='60vh';
            p.style.overflow='auto';
            p.style.font='12px/1.35 monospace';
            p.style.background='rgba(0,0,0,0.85)';
            p.style.color='#fff';
            p.style.padding='8px 10px';
            p.style.borderRadius='4px';
            p.style.zIndex='2147483647';
            p.style.boxShadow='0 2px 12px rgba(0,0,0,0.4)';
            p.style.margin='0';
            (document.documentElement||document.body).appendChild(p);
            S.panel=p;
        }
    }

    function compute(el){
        var all = collectAllAttrs(el);
        return {
            tag: el.tagName ? el.tagName.toLowerCase() : null,
            id: el.id || null,
            classes: (el.classList&&el.classList.length)?Array.prototype.slice.call(el.classList):null,
            role: el.getAttribute?(el.getAttribute('role')||null):null,
            text: textOf(el),
            css: cssSelector(el, 6),
            xpath: xPath(el),
            attrs: stripAriaData(all),
            aria: onlyAria(all),
            data: onlyData(all)
        };
    }

    function render(el, pageX, pageY){
        var S = window.__zrbSel; ensureUi();

        var r=el.getBoundingClientRect();
        S.box.style.left=(r.left+window.scrollX)+'px';
        S.box.style.top =(r.top +window.scrollY)+'px';
        S.box.style.width =r.width+'px';
        S.box.style.height=r.height+'px';
        S.box.style.display='block';

        var d = compute(el); // compute ONCE

        var labelCss = d.css.length>140 ? (d.css.slice(0,140)+'…') : d.css;
        var labelXp  = d.xpath.length>140 ? (d.xpath.slice(0,140)+'…') : d.xpath;
        S.lbl.textContent = labelCss + '  |  ' + labelXp;
        var lx=pageX+10, ly=pageY+10;
        S.lbl.style.transform='translate('+(lx-(r.left+window.scrollX))+'px,'+(ly-(r.top+window.scrollY))+'px)';

        // JSON dump to avoid any "empty headings"
        try {
            S.panel.textContent = JSON.stringify(d, null, 2);
        } catch(e){
            S.panel.textContent = String(d);
        }
        S.panel.style.display='block';
    }

    function onMove(e){
        var S=window.__zrbSel, now=Date.now();
        if(now - S.ts < 40) return; S.ts=now;
        var el = topElemAt(e.clientX, e.clientY);
        if(!el) return;
        render(el, e.pageX, e.pageY);
    }

    window.__zrbSel.bind = function(){
        var S=window.__zrbSel;
        if(S.onMove) return;
        S.onMove = onMove;
        window.addEventListener('mousemove', S.onMove, true);
    };
    window.__zrbSel.unbind = function(){
        var S=window.__zrbSel;
        if(!S.onMove) return;
        window.removeEventListener('mousemove', S.onMove, true);
        S.onMove=null;
    };

    window.toggleTooltip = function(enable){
        var S=window.__zrbSel;
        var want=!!enable;
        if (want===S.enabled) return;
        S.enabled=want;
        ensureUi();
        if (S.enabled) { S.bind(); }
        else {
            S.unbind();
            if(S.box) S.box.style.display='none';
            if(S.panel) S.panel.style.display='none';
            if(S.lbl) S.lbl.textContent='';
        }
    };

    // expose minimal helpers (optional)
    window.__wd4j = {
        css: cssSelector,
        xpath: xPath,
        all: function(el){ return el&&el.nodeType===1 ? compute(el) : null; },
        fromPoint: function(x,y){ return topElemAt(x,y); }
    };

    // auto-enable to test immediately
    toggleTooltip(true);
    console.log('[selector-overlay:min] ready. Use toggleTooltip(false) to hide, __wd4j.all($0) to inspect.');
}
