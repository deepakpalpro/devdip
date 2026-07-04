package com.banking.forms.formimport.infrastructure;

import com.banking.forms.formimport.domain.FormImportProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormImportProviderRepository extends JpaRepository<FormImportProvider, UUID> {

    Optional<FormImportProvider> findByCode(String code);

    List<FormImportProvider> findBySourceTypeAndEnabledTrueOrderByPriorityAsc(String sourceType);

    List<FormImportProvider> findAllByOrderBySourceTypeAscPriorityAsc();
}
