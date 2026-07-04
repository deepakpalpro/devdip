package com.banking.forms.downstream.infrastructure;

import com.banking.forms.downstream.domain.DownstreamProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownstreamProviderRepository extends JpaRepository<DownstreamProvider, UUID> {

    Optional<DownstreamProvider> findByCode(String code);

    List<DownstreamProvider> findAllByOrderByConnectorTypeAscPriorityAsc();

    /** All enabled providers, best (lowest) priority first — the fan-out set for a submission. */
    List<DownstreamProvider> findByEnabledTrueOrderByPriorityAsc();
}
