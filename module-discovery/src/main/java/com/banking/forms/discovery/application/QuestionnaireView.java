package com.banking.forms.discovery.application;

import com.fasterxml.jackson.databind.JsonNode;

/** The questionnaire as presented to the consumer: metadata plus the questions schema. */
public record QuestionnaireView(String code, String name, JsonNode schema) {}
