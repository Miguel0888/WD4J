package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.type.browsingContext.BrowsingContext;

/**
 * The script.Target type represents a value that is either a script.Realm or a browsingContext.BrowsingContext.
 *
 * This is useful in cases where a navigable identifier can stand in for the realm associated with the navigable’s
 * active document.
 */
public abstract class Target {

    public static class RealmTarget extends Target {
        private final Realm realm;

        public RealmTarget(Realm realm) {
            this.realm = realm;
        }

        public Realm getRealm() {
            return realm;
        }
    }

    public static class ContextTarget extends Target {
        private final BrowsingContext context;
        private final String sandbox; // Optional

        public ContextTarget(BrowsingContext context) {
            this(context, null);
        }

        public ContextTarget(BrowsingContext context, String sandbox) {
            this.context = context;
            this.sandbox = sandbox;
        }

        public BrowsingContext getContext() {
            return context;
        }

        public String getSandbox() {
            return sandbox;
        }
    }
}


