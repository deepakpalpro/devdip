package com.banking.forms.bff.collection.api;

import com.banking.forms.collection.application.CollectionQueryRequest;
import com.banking.forms.collection.application.CollectionQueryResult;
import com.banking.forms.collection.application.CollectionQueryService;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** External collection query API — sanitized submission data only (Phase 6.3). */
@RestController
@RequestMapping("/api/collection/v1")
public class CollectionQueryController {

    public static final String TENANT_ATTRIBUTE = "collectionTenantId";

    private final CollectionQueryService queryService;

    public CollectionQueryController(CollectionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/submissions")
    public CollectionQueryResult querySubmissions(
            @RequestAttribute(TENANT_ATTRIBUTE) UUID tenantId,
            @RequestParam(required = false) String formCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant submittedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant submittedBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fields) {
        Set<String> projection = parseFields(fields);
        return queryService.query(
                tenantId,
                new CollectionQueryRequest(
                        formCode, status, submittedAfter, submittedBefore, page, size, projection));
    }

    private static Set<String> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
