package de.bund.zrb.util;

import de.bund.zrb.WebDriver;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Löst für einen WDTarget (Realm/Context) die BrowsingContext-ID (String).
 * - ContextTarget: direkt.
 * - RealmTarget: via script.getRealms() -> konkrete WDRealmInfo-Unterklasse:
 *     * WindowRealmInfo: context String.
 *     * Worker/Worklet: über owners[] (Realm-IDs oder direkt Context-IDs) bis Window-Realm.
 */
public final class BrowsingContextResolver {

    private BrowsingContextResolver() {}

    public static String resolveContextId(WebDriver webDriver, WDTarget target) {
        Objects.requireNonNull(webDriver, "webDriver");
        Objects.requireNonNull(target, "target");

        if (target instanceof WDTarget.ContextTarget) {
            WDBrowsingContext ctx = ((WDTarget.ContextTarget) target).getContext();
            if (ctx == null || ctx.value() == null) {
                throw new IllegalStateException("ContextTarget has no context id.");
            }
            return ctx.value();
        }

        if (target instanceof WDTarget.RealmTarget) {
            WDRealm realm = ((WDTarget.RealmTarget) target).getRealm();
            if (realm == null || realm.value() == null) {
                throw new IllegalStateException("RealmTarget has no realm id.");
            }
            return resolveFromRealmId(webDriver, realm.value());
        }

        throw new IllegalArgumentException("Unsupported WDTarget: " + target.getClass().getName());
    }

    /** Sucht in allen Realms den mit realmId und läuft Owner-Kette bis Window-Realm/Context. */
    public static String resolveFromRealmId(WebDriver webDriver, String realmId) {
        WDScriptResult.GetRealmsResult realms = webDriver.script().getRealms();
        List<WDRealmInfo> infos = realms.getRealms();
        if (infos == null || infos.isEmpty()) {
            throw new IllegalStateException("No realms available to resolve browsing context.");
        }

        Map<String, WDRealmInfo> index = indexByRealmId(infos);

        WDRealmInfo start = index.get(realmId);
        if (start == null) {
            // Fallback: manchmal liefert BiDi realmId ohne Index – letzte Hoffnung: linear suchen
            start = findByRealmIdLinear(infos, realmId);
            if (start == null) {
                throw new IllegalStateException("Realm not found: " + realmId);
            }
        }

        String ctx = unwindToWindowContext(index, start, 0);
        if (ctx == null) {
            // Optionaler Fallback: eindeutigen Window-Realm mit gleicher Origin nehmen (nur wenn eindeutig!)
            ctx = tryResolveByUniqueOrigin(infos, start.getOrigin());
        }

        if (ctx == null) {
            throw new IllegalStateException("Could not resolve browsingContext for realm: " + realmId);
        }
        return ctx;
    }

    private static Map<String, WDRealmInfo> indexByRealmId(List<WDRealmInfo> infos) {
        Map<String, WDRealmInfo> m = new HashMap<>();
        for (WDRealmInfo ri : infos) {
            WDRealm r = ri.getRealm();
            if (r != null && r.value() != null) {
                m.put(r.value(), ri);
            }
        }
        return m;
    }

    private static WDRealmInfo findByRealmIdLinear(List<WDRealmInfo> infos, String id) {
        for (WDRealmInfo ri : infos) {
            WDRealm r = ri.getRealm();
            if (r != null && id.equals(r.value())) return ri;
        }
        return null;
    }

    /**
     * Rekursion:
     * - WindowRealmInfo => context (String)
     * - Sonst: owners[] ablaufen:
     *     * Ist owner wie eine Context-ID => return owner
     *     * Sonst: als Realm-ID betrachten und rekursiv auflösen
     */
    private static String unwindToWindowContext(Map<String, WDRealmInfo> index, WDRealmInfo node, int depth) {
        if (depth > 32) return null; // Zyklenschutz

        if (node instanceof WDRealmInfo.WindowRealmInfo) {
            String ctx = ((WDRealmInfo.WindowRealmInfo) node).getContext();
            return (ctx == null || ctx.isEmpty()) ? null : ctx;
        }

        List<String> owners = extractOwnerRealmIds(node);
        if (owners != null) {
            for (String owner : owners) {
                if (owner == null || owner.isEmpty()) continue;

                // Manchmal kann ein Owner direkt eine Context-ID sein
                if (looksLikeContextId(owner)) {
                    return owner;
                }

                // Sonst als Realm-ID interpretieren
                WDRealmInfo parent = index.get(owner);
                if (parent != null) {
                    String s = unwindToWindowContext(index, parent, depth + 1);
                    if (s != null) return s;
                }
            }
        }

        return null;
    }

    /** Extrahiert Owner-IDs (Realm- oder Context-IDs) aus den bekannten Info-Typen. */
    private static List<String> extractOwnerRealmIds(WDRealmInfo node) {
        if (node instanceof WDRealmInfo.DedicatedWorkerRealmInfo) {
            return ((WDRealmInfo.DedicatedWorkerRealmInfo) node).getOwners();
        }
        // Falls du owners auch in anderen Klassen ergänzt, hier erweitern:
        // if (node instanceof WDRealmInfo.SharedWorkerRealmInfoX) return ((...) node).getOwners();

        return null; // keine Information → später evtl. Origin-Fallback
    }

    // UUID-ähnlicher Check für BiDi-BrowsingContext-IDs (heuristisch, reicht für Praxis)
    private static final Pattern UUID_LIKE =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static boolean looksLikeContextId(String s) {
        return s != null && UUID_LIKE.matcher(s).matches();
    }

    /**
     * Letzter Ausweg: finde genau EINEN Window-Realm mit gleicher origin und nimm dessen context.
     * Ambiguität → null.
     */
    private static String tryResolveByUniqueOrigin(List<WDRealmInfo> infos, String origin) {
        if (origin == null || origin.isEmpty()) return null;

        String ctx = null;
        int matches = 0;

        for (WDRealmInfo ri : infos) {
            if (ri instanceof WDRealmInfo.WindowRealmInfo
                    && origin.equals(ri.getOrigin())) {
                String c = ((WDRealmInfo.WindowRealmInfo) ri).getContext();
                if (c != null && !c.isEmpty()) {
                    matches++;
                    ctx = c;
                    if (matches > 1) return null; // mehrdeutig
                }
            }
        }
        return matches == 1 ? ctx : null;
    }
}
