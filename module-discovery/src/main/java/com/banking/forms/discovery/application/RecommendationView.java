package com.banking.forms.discovery.application;

import java.util.List;

/** A single ranked recommendation returned to the consumer. */
public record RecommendationView(
        String formCode, String formName, String category, double score, boolean recommended, List<String> reasons) {}
