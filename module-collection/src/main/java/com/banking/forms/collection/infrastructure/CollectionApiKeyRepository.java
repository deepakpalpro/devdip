package com.banking.forms.collection.infrastructure;

import com.banking.forms.collection.domain.CollectionApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionApiKeyRepository extends JpaRepository<CollectionApiKey, UUID> {

    List<CollectionApiKey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<CollectionApiKey> findByKeyHashAndEnabledTrue(String keyHash);
}
