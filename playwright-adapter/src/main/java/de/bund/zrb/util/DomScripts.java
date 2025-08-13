package de.bund.zrb.util;

public final class DomScripts {
    private DomScripts() {}

    /** Generisches fill+commit: focus → value → input → change → blur */
    public static final String FILL_WITH_COMMIT =
            "function(value){"
                    + "  const el=this;"
                    + "  el.focus();"
                    + "  el.value=(value==null?'':String(value));"
                    + "  el.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "  el.dispatchEvent(new Event('change',{bubbles:true}));"
                    + "  el.blur();"
                    + "}";

    /** PrimeFaces p:inputNumber: zusätzlich auf Hidden *_hinput warten */
    public static final String FILL_PF_INPUTNUMBER_AND_WAIT =
            "function(value){"
                    + "  const el=this;"
                    + "  const root=el.closest('span.ui-inputnumber')||el.parentElement;"
                    + "  const hid=root?root.querySelector(\"input[id$='_hinput']\"):null;"
                    + "  const prev=hid?hid.value:null;"
                    + "  el.focus();"
                    + "  el.value=(value==null?'':String(value));"
                    + "  el.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "  el.dispatchEvent(new Event('change',{bubbles:true}));"
                    + "  el.blur();"
                    + "  if(!hid) return true;"
                    + "  return new Promise(resolve=>{"
                    + "    const deadline=Date.now()+1500;"
                    + "    (function tick(){"
                    + "      if(hid.value!==prev && hid.value!=='0' && hid.value!=='0.00'){resolve(true);return;}"
                    + "      if(Date.now()>deadline){resolve(false);return;}"
                    + "      setTimeout(tick,20);"
                    + "    })();"
                    + "  });"
                    + "}";
}
