package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.AiEvaluation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {

    Optional<AiEvaluation> findBySubmissionId(UUID submissionId);
}
