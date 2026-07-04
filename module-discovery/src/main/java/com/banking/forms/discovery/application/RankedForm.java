package com.banking.forms.discovery.application;

import java.util.List;

/** A scored form produced by the recommendation engine, with the rationales that contributed. */
public record RankedForm(String formCode, double score, List<String> rationales) {}
