package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.domain.ServiceCallLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCallLogRepository extends JpaRepository<ServiceCallLog, UUID> {

    List<ServiceCallLog> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
