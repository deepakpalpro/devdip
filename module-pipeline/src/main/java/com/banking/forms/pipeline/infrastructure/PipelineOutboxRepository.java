package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineOutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineOutboxRepository extends JpaRepository<PipelineOutboxEvent, UUID> {

    List<PipelineOutboxEvent> findByPublishedFalseOrderByOccurredAtAsc(Pageable pageable);

    List<PipelineOutboxEvent> findBySubmissionIdOrderByOccurredAtAsc(UUID submissionId);

    Optional<PipelineOutboxEvent> findFirstBySubmissionIdAndEventTypeAndPublishedFalse(
            UUID submissionId, String eventType);
}
