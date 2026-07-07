package com.banking.forms.serviceintegration.domain;

/** Where a service instance binding applies — narrowest scope wins at runtime. */
public enum ServiceBindingScope {
    FORM,
    PIPELINE,
    PIPELET
}
