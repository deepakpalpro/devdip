package com.banking.forms.discovery.application;

/** Maps a questionnaire answer to a target form field, used by the pre-population engine. */
public record FieldMapping(String questionKey, String targetSection, String targetField) {}
