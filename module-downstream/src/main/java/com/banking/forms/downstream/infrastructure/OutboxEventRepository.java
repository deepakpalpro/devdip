package com.banking.forms.downstream.infrastructure;

import com.banking.forms.downstream.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<OutboxEvent> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
