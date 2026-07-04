package com.banking.forms.submission.infrastructure;

import com.banking.forms.submission.domain.SubmissionFieldValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionFieldValueRepository extends JpaRepository<SubmissionFieldValue, UUID> {

    List<SubmissionFieldValue> findBySectionId(UUID sectionId);

    @Modifying
    @Query("delete from SubmissionFieldValue f where f.sectionId = :sectionId")
    void deleteBySectionId(@Param("sectionId") UUID sectionId);
}
