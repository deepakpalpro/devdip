package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.domain.ServiceInstance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, UUID> {

    List<ServiceInstance> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<ServiceInstance> findByTenantIdAndCode(UUID tenantId, String code);

    List<ServiceInstance> findByIdInAndEnabledTrue(List<UUID> ids);
}
