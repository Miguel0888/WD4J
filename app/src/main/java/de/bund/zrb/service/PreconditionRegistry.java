package de.bund.zrb.service;

import com.google.gson.reflect.TypeToken;
import de.bund.zrb.model.Precondition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Manage preconditions exactly like TestRegistry manages tests.
 * Keep it minimal: in-memory list, save/load via SettingsService.
 */
public class PreconditionRegistry {

    private static final PreconditionRegistry INSTANCE = new PreconditionRegistry();
    private final List<Precondition> preconditions = new ArrayList<Precondition>();

    private PreconditionRegistry() {
        load();
    }

    public static PreconditionRegistry getInstance() {
        return INSTANCE;
    }

    // ---------- Query ----------

    /** Return the live list (UI may modify it directly, same as TestRegistry). */
    public List<Precondition> getAll() {
        return preconditions;
    }

    /** Find a precondition by id or return null. */
    public Precondition getById(String id) {
        if (id == null) return null;
        for (Precondition p : preconditions) {
            if (id.equals(p.getId())) return p;
        }
        return null;
    }

    // ---------- Mutate ----------

    /** Create a new precondition with a fresh UUID and optional name, add to list, and return it. */
    public Precondition create(String name) {
        Precondition p = new Precondition();
        p.setId(UUID.randomUUID().toString());
        p.setName(name);
        preconditions.add(p);
        return p;
    }

    /** Append a new precondition to the in-memory list. Ensure it has an id. */
    public void addPrecondition(Precondition precondition) {
        if (precondition.getId() == null || precondition.getId().trim().isEmpty()) {
            precondition.setId(UUID.randomUUID().toString());
        }
        preconditions.add(precondition);
    }

    /** Remove a precondition by id. Return true if removed. */
    public boolean removeById(String id) {
        if (id == null) return false;
        Iterator<Precondition> it = preconditions.iterator();
        while (it.hasNext()) {
            if (id.equals(it.next().getId())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    // ---------- Persistence ----------

    /** Persist all preconditions to precond.json (next to tests.json). */
    public void save() {
        SettingsService.getInstance().save("precond.json", preconditions);
    }

    /** Load all preconditions from precond.json into the in-memory list. */
    public void load() {
        Type type = new TypeToken<List<Precondition>>() {}.getType();
        List<Precondition> loaded =
                SettingsService.getInstance().load("precond.json", type);
        if (loaded != null) {
            preconditions.clear();
            // Ensure ids exist for older files
            for (Precondition p : loaded) {
                if (p.getId() == null || p.getId().trim().isEmpty()) {
                    p.setId(UUID.randomUUID().toString());
                }
                preconditions.add(p);
            }
        }
    }
}
