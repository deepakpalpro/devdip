package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.domain.ServiceBinding;
import com.banking.forms.serviceintegration.domain.ServiceBindingScope;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceBindingRepository extends JpaRepository<ServiceBinding, UUID> {

    List<ServiceBinding> findByTenantIdAndFormVersionIdAndScopeAndEnabledTrue(
            UUID tenantId, UUID formVersionId, ServiceBindingScope scope);

    List<ServiceBinding> findByTenantIdAndPipelineDefinitionIdAndScopeAndEnabledTrue(
            UUID tenantId, UUID pipelineDefinitionId, ServiceBindingScope scope);

    List<ServiceBinding> findByTenantIdAndPipelineStepIdAndScopeAndEnabledTrue(
            UUID tenantId, UUID pipelineStepId, ServiceBindingScope scope);

    List<ServiceBinding> findByTenantIdAndFormVersionIdOrderByCreatedAtAsc(UUID tenantId, UUID formVersionId);
}
