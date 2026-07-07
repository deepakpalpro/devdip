package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Page<Submission> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Submission> findByTenantIdAndStatus(UUID tenantId, SubmissionStatus status, Pageable pageable);

    Page<Submission> findByTenantIdAndFormVersionIdIn(UUID tenantId, List<UUID> formVersionIds, Pageable pageable);

    Page<Submission> findByTenantIdAndFormVersionIdInAndStatus(
            UUID tenantId, List<UUID> formVersionIds, SubmissionStatus status, Pageable pageable);

    List<Submission> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    List<Submission> findByTenantIdAndFormVersionId(UUID tenantId, UUID formVersionId);

    List<Submission> findByTenantIdAndFormVersionIdAndStatus(
            UUID tenantId, UUID formVersionId, SubmissionStatus status);
}
