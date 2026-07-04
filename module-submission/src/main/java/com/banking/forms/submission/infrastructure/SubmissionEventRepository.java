package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.SubmissionEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionEventRepository extends JpaRepository<SubmissionEvent, Long> {

    List<SubmissionEvent> findBySubmissionIdOrderByIdAsc(UUID submissionId);
}
