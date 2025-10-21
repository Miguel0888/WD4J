package de.bund.zrb.model;

import de.bund.zrb.service.UserRegistry;
import de.bund.zrb.service.UserRegistry.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GivenRegistry {

    private static final GivenRegistry INSTANCE = new GivenRegistry();
    private final Map<String, GivenTypeDefinition> definitions = new LinkedHashMap<String, GivenTypeDefinition>();

    public static GivenRegistry getInstance() {
        return INSTANCE;
    }

    private GivenRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        GivenTypeDefinition urlCheck = new GivenTypeDefinition("url-is", "Aktuelle URL prüfen");
        urlCheck.addField("expectedUrl", "Erwartete URL", "", String.class);
        register(urlCheck);

        GivenTypeDefinition elementExists = new GivenTypeDefinition("element-exists", "Element existiert");
        elementExists.addField("selector", "CSS Selector", "body", String.class);
        register(elementExists);

        GivenTypeDefinition cookieCheck = new GivenTypeDefinition("cookie-present", "Cookie vorhanden");
        cookieCheck.addField("cookieName", "Cookie-Name", "", String.class);
        register(cookieCheck);

        GivenTypeDefinition localStorageCheck = new GivenTypeDefinition("localstorage-key", "LocalStorage-Key vorhanden");
        localStorageCheck.addField("key", "Key", "", String.class);
        register(localStorageCheck);

        GivenTypeDefinition jsEval = new GivenTypeDefinition("js-eval", "JavaScript Bedingung (true/false)");
        jsEval.addField("script", "JavaScript Ausdruck", "return document.readyState === 'complete';", Code.class);
        register(jsEval);

        GivenTypeDefinition loggedIn = new GivenTypeDefinition("logged-in", "Benutzer ist eingeloggt");
        List<String> usernames = UserRegistry.getInstance().getAll()
                .stream().map(User::getUsername).collect(Collectors.<String>toList());
        loggedIn.addField("username", "Benutzername", "", String.class, usernames);
        register(loggedIn);

        // --- Wichtig: preconditionRef bekannt machen (Editor fällt nicht ins Leere) ---
        GivenTypeDefinition preRef = new GivenTypeDefinition("preconditionRef", "Referenz auf Precondition");
        preRef.addField("id", "Precondition-UUID", "", String.class);
        register(preRef);
    }

    public void register(GivenTypeDefinition def) {
        definitions.put(def.getType(), def);
    }

    public GivenTypeDefinition get(String type) {
        return definitions.get(type);
    }

    public Collection<GivenTypeDefinition> getAll() {
        return definitions.values();
    }
}
