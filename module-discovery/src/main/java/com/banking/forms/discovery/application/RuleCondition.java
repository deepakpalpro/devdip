package com.banking.forms.discovery.application;

/** A single triage predicate evaluated against a questionnaire answer. */
public record RuleCondition(String questionKey, ConditionOperator operator, Object value) {}
