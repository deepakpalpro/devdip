package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineExecution;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineExecutionRepository extends JpaRepository<PipelineExecution, UUID> {

    Optional<PipelineExecution> findFirstBySubmissionIdOrderByStartedAtDesc(UUID submissionId);
}
