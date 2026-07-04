package com.banking.forms.formdefinition.infrastructure;

import com.banking.forms.formdefinition.domain.FormDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {

    Optional<FormDefinition> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<FormDefinition> findByIdAndTenantId(UUID id, UUID tenantId);

    List<FormDefinition> findByTenantId(UUID tenantId);
}
