package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineJobRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineJobRunRepository extends JpaRepository<PipelineJobRun, UUID> {

    List<PipelineJobRun> findByJobDefinitionIdOrderByStartedAtDesc(UUID jobDefinitionId);
}
