package com.banking.forms.discovery.application;

import java.util.List;

/**
 * A triage rule: when every condition matches the user's answers, {@code weight} is added to
 * {@code targetFormCode}'s score and {@code rationale} is surfaced to explain the recommendation.
 */
public record TriageRule(String targetFormCode, double weight, String rationale, List<RuleCondition> conditions) {}
