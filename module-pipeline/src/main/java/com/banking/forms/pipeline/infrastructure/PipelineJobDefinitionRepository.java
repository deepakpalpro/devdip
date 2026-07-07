package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineJobDefinition;
import com.banking.forms.pipeline.domain.PipelineJobType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineJobDefinitionRepository extends JpaRepository<PipelineJobDefinition, UUID> {

    List<PipelineJobDefinition> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<PipelineJobDefinition> findByTenantIdAndCode(UUID tenantId, String code);

    List<PipelineJobDefinition> findByJobTypeAndEnabledTrue(PipelineJobType jobType);

    List<PipelineJobDefinition> findByTenantIdAndFormVersionIdAndJobTypeAndTriggerEventAndEnabledTrue(
            UUID tenantId, UUID formVersionId, PipelineJobType jobType, String triggerEvent);
}
