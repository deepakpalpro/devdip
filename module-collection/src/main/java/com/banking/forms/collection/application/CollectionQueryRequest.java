package com.banking.forms.collection.application;

import java.time.Instant;
import java.util.Set;

/** Filter and pagination parameters for external collection queries. */
public record CollectionQueryRequest(
        String formCode,
        String status,
        Instant submittedAfter,
        Instant submittedBefore,
        int page,
        int size,
        Set<String> fieldProjection) {

    public int safePage() {
        return Math.max(page, 0);
    }

    public int safeSize() {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
