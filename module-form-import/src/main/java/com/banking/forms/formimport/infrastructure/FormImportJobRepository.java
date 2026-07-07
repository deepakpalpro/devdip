package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.domain.FormImportJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormImportJobRepository extends JpaRepository<FormImportJob, UUID> {

    Optional<FormImportJob> findByIdAndTenantId(UUID id, UUID tenantId);

    List<FormImportJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
