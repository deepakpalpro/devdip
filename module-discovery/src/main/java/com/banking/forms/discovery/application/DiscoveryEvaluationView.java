package com.banking.forms.discovery.application;

import java.util.List;
import java.util.UUID;

/** The result of evaluating a questionnaire: the persisted session id and ranked recommendations. */
public record DiscoveryEvaluationView(UUID sessionId, List<RecommendationView> recommendations) {}
