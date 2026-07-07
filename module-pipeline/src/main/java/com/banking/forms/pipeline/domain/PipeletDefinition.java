package com.banking.forms.pipeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/** Catalog row describing an available pipelet (developer bean keyed by {@link #code}). */
@Entity
@Table(name = "pipelet_definition")
public class PipeletDefinition {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private java.util.UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Lob
    @Column(name = "config_schema_json")
    private String configSchemaJson;

    @Column(nullable = false)
    private boolean enabled;

    protected PipeletDefinition() {}

    public java.util.UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getConfigSchemaJson() {
        return configSchemaJson;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
