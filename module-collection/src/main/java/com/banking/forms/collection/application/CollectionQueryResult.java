package com.banking.forms.collection.application;

import java.util.List;

/** Paginated collection query response. */
public record CollectionQueryResult(int page, int size, long total, List<CollectionRecordView> items) {}
