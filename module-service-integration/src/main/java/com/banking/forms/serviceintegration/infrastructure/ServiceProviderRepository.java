package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.domain.ServiceProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, UUID> {

    Optional<ServiceProvider> findByCode(String code);

    List<ServiceProvider> findAllByOrderByAdapterTypeAscPriorityAsc();

    List<ServiceProvider> findByEnabledTrueOrderByPriorityAsc();
}
