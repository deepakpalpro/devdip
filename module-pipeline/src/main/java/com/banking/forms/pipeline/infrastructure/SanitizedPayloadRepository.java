package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.SanitizedPayload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanitizedPayloadRepository extends JpaRepository<SanitizedPayload, UUID> {

    Optional<SanitizedPayload> findBySubmissionId(UUID submissionId);
}
