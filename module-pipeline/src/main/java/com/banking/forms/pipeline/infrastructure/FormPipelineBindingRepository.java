package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.FormPipelineBinding;
import com.banking.forms.pipeline.domain.PipelineTrigger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormPipelineBindingRepository extends JpaRepository<FormPipelineBinding, UUID> {

    Optional<FormPipelineBinding> findByFormVersionIdAndTriggerEventAndEnabledTrue(
            UUID formVersionId, String triggerEvent);

    Optional<FormPipelineBinding> findByFormVersionIdAndTriggerEvent(UUID formVersionId, String triggerEvent);

    List<FormPipelineBinding> findByFormVersionIdOrderByTriggerEventAsc(UUID formVersionId);

    List<FormPipelineBinding> findByTenantIdAndFormVersionId(UUID tenantId, UUID formVersionId);
}
