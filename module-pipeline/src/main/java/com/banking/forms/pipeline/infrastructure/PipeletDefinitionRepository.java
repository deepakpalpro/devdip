package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipeletDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipeletDefinitionRepository extends JpaRepository<PipeletDefinition, UUID> {

    List<PipeletDefinition> findByEnabledTrueOrderByCodeAsc();

    Optional<PipeletDefinition> findByCode(String code);
}
