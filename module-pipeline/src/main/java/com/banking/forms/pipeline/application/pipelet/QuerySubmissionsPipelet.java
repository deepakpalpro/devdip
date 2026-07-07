package com.banking.forms.pipeline.application.pipelet;

import com.banking.forms.pipeline.application.PipelineTimelineRecorder;
import com.banking.forms.pipeline.spi.Pipelet;
import com.banking.forms.pipeline.spi.PipeletConfig;
import com.banking.forms.pipeline.spi.PipeletContext;
import com.banking.forms.pipeline.spi.PipeletResult;
import com.banking.forms.submission.domain.Submission;
import com.banking.forms.submission.domain.SubmissionStatus;
import com.banking.forms.submission.infrastructure.SubmissionRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Loads submission IDs matching a status filter into the pipelet context (batch job helper). */
@Component
public class QuerySubmissionsPipelet implements Pipelet {

    private final SubmissionRepository submissionRepository;
    private final PipelineTimelineRecorder timeline;

    public QuerySubmissionsPipelet(SubmissionRepository submissionRepository, PipelineTimelineRecorder timeline) {
        this.submissionRepository = submissionRepository;
        this.timeline = timeline;
    }

    @Override
    public String code() {
        return "query-submissions";
    }

    @Override
    public PipeletResult execute(PipeletContext context, PipeletConfig config) {
        String statusText = config.text("status", null);
        List<Submission> submissions;
        if (statusText != null && !statusText.isBlank()) {
            SubmissionStatus status = SubmissionStatus.valueOf(statusText);
            submissions = submissionRepository.findByTenantIdAndFormVersionIdAndStatus(
                    context.tenantId(), context.formVersionId(), status);
        } else {
            submissions = submissionRepository.findByTenantIdAndFormVersionId(
                    context.tenantId(), context.formVersionId());
        }
        List<java.util.UUID> ids = submissions.stream().map(Submission::getId).toList();
        context.setAttribute("queriedSubmissionIds", ids);
        timeline.record(context.submissionId(), "QUERY_SUBMISSIONS", "count", ids.size());
        return PipeletResult.success(String.valueOf(ids.size()));
    }
}
