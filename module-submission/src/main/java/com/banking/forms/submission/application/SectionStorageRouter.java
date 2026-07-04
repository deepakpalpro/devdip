package com.banking.forms.submission.application;

import com.banking.forms.formdefinition.domain.StorageStrategy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Resolves the {@link SectionStorageStrategy} for a form's configured {@link StorageStrategy}. */
@Component
public class SectionStorageRouter {

    private final Map<StorageStrategy, SectionStorageStrategy> strategies = new EnumMap<>(StorageStrategy.class);

    public SectionStorageRouter(List<SectionStorageStrategy> implementations) {
        for (SectionStorageStrategy implementation : implementations) {
            strategies.put(implementation.strategy(), implementation);
        }
    }

    public SectionStorageStrategy resolve(StorageStrategy strategy) {
        SectionStorageStrategy resolved = strategies.get(strategy);
        if (resolved == null) {
            throw new IllegalStateException("No storage strategy registered for " + strategy);
        }
        return resolved;
    }
}
