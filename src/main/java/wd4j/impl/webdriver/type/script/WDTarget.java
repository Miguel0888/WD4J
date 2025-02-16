package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;

/**
 * The script.Target type represents a value that is either a script.Realm or a browsingContext.BrowsingContext.
 *
 * This is useful in cases where a navigable identifier can stand in for the realm associated with the navigable’s
 * active document.
 */
public abstract class WDTarget {

    public static class RealmWDTarget extends WDTarget {
        private final WDRealm WDRealm;

        public RealmWDTarget(WDRealm WDRealm) {
            this.WDRealm = WDRealm;
        }

        public WDRealm getRealm() {
            return WDRealm;
        }
    }

    public static class ContextWDTarget extends WDTarget {
        private final WDBrowsingContext context;
        private final String sandbox; // Optional

        public ContextWDTarget(WDBrowsingContext context) {
            this(context, null);
        }

        public ContextWDTarget(WDBrowsingContext context, String sandbox) {
            this.context = context;
            this.sandbox = sandbox;
        }

        public WDBrowsingContext getContext() {
            return context;
        }

        public String getSandbox() {
            return sandbox;
        }
    }
}


