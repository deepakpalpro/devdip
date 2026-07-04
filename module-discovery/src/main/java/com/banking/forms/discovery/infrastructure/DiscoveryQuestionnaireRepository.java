package com.banking.forms.discovery.infrastructure;

import com.banking.forms.discovery.domain.DiscoveryQuestionnaire;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryQuestionnaireRepository extends JpaRepository<DiscoveryQuestionnaire, UUID> {

    Optional<DiscoveryQuestionnaire> findByTenantIdAndCode(UUID tenantId, String code);

    List<DiscoveryQuestionnaire> findByTenantIdAndStatus(UUID tenantId, String status);
}
