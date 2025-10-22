package de.bund.zrb.persistence;

import de.bund.zrb.model.Precondition;

import java.io.IOException;
import java.util.List;

/**
 * Define persistence operations for preconditions.
 * Keep interface small and intention-revealing.
 */
public interface PreconditionRepository {

    /** Load all preconditions from storage. Return empty list if file does not exist. */
    List<Precondition> loadAll() throws IOException;

    /** Persist all preconditions atomically. */
    void saveAll(List<Precondition> preconditions) throws IOException;
}
