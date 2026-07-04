package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.Submission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Submission> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
}
