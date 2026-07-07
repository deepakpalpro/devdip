package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineDefinitionRepository extends JpaRepository<PipelineDefinition, UUID> {

    List<PipelineDefinition> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<PipelineDefinition> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<PipelineDefinition> findFirstByTenantIdAndSystemDefaultTrueAndStatus(UUID tenantId, String status);
}
