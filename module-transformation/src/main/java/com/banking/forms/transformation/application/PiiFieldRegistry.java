package com.banking.forms.transformation.application;

import com.banking.forms.transformation.domain.PiiStrategy;

/**
 * Resolves the {@link PiiStrategy} for a given form field. Implementations can be driven by static
 * heuristics, per-form config, or a persisted registry.
 */
public interface PiiFieldRegistry {

    /**
     * @param formCode the owning form (allows per-form overrides; may be ignored)
     * @param fieldKey the leaf field key (last path segment for nested/embedded fields)
     * @return the strategy to apply; {@link PiiStrategy#NONE} when the field is not sensitive
     */
    PiiStrategy strategyFor(String formCode, String fieldKey);
}
