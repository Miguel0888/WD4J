package de.bund.zrb.ui.tabs;

import de.bund.zrb.model.Precondtion;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.service.PreconditionRegistry;

import java.util.List;

/** Perform lightweight validation of Given precondition references for editor tabs. */
public final class PreconditionListValidator {

    private PreconditionListValidator() {
        // no instances
    }

    /** Validate the list or throw an {@link IllegalStateException} with a human readable message. */
    public static void validateOrThrow(String scopeLabel, List<Precondtion> givens) {
        if (givens == null) {
            return;
        }

        String prefix = (scopeLabel == null || scopeLabel.trim().isEmpty())
                ? "Preconditions"
                : scopeLabel.trim();

        for (int i = 0; i < givens.size(); i++) {
            Precondtion given = givens.get(i);
            if (given == null) {
                continue;
            }

            String type = safe(given.getType());
            if (type.isEmpty()) {
                throw new IllegalStateException(prefix + ": Eintrag " + (i + 1) + " hat keinen Typ.");
            }

            if (PreconditionListUtil.TYPE_PRECONDITION_REF.equals(type)) {
                String refId = PreconditionListUtil.extractPreconditionId(given);
                if (refId == null || refId.trim().isEmpty()) {
                    throw new IllegalStateException(prefix + ": Eintrag " + (i + 1)
                            + " verweist auf eine Precondition ohne ID.");
                }
                Precondition target = PreconditionRegistry.getInstance().getById(refId.trim());
                if (target == null) {
                    throw new IllegalStateException(prefix + ": Unbekannte Precondition-ID " + refId + ".");
                }
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

