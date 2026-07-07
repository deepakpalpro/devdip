package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Submission> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    List<Submission> findByTenantIdAndFormVersionId(UUID tenantId, UUID formVersionId);

    List<Submission> findByTenantIdAndFormVersionIdAndStatus(
            UUID tenantId, UUID formVersionId, SubmissionStatus status);
}
