package com.banking.forms.formdefinition.infrastructure;

import com.banking.forms.formdefinition.domain.FormVersion;
import com.banking.forms.formdefinition.domain.FormVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormVersionRepository extends JpaRepository<FormVersion, UUID> {

    List<FormVersion> findByFormDefinitionIdOrderByVersionNumberDesc(UUID formDefinitionId);

    Optional<FormVersion> findFirstByFormDefinitionIdAndStatusOrderByVersionNumberDesc(
            UUID formDefinitionId, FormVersionStatus status);
}
