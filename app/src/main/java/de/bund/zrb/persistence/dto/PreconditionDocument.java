package de.bund.zrb.persistence.dto;

import de.bund.zrb.model.Precondition;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the JSON root document:
 * {
 *   "preconditions": [ ... ]
 * }
 */
public class PreconditionDocument {

    private List<Precondition> preconditions = new ArrayList<Precondition>();

    public List<Precondition> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<Precondition> preconditions) {
        this.preconditions = (preconditions != null) ? preconditions : new ArrayList<Precondition>();
    }
}
