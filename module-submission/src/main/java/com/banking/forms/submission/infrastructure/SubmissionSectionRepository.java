package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.SubmissionSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionSectionRepository extends JpaRepository<SubmissionSection, UUID> {

    List<SubmissionSection> findBySubmissionId(UUID submissionId);

    Optional<SubmissionSection> findBySubmissionIdAndSectionKey(UUID submissionId, String sectionKey);
}
