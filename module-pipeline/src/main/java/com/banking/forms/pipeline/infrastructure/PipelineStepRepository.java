package com.banking.forms.pipeline.infrastructure;

import com.banking.forms.pipeline.domain.PipelineStep;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineStepRepository extends JpaRepository<PipelineStep, UUID> {

    List<PipelineStep> findByPipelineDefinitionIdOrderByStepOrderAsc(UUID pipelineDefinitionId);

    void deleteByPipelineDefinitionId(UUID pipelineDefinitionId);
}
