package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.domain.PipelineJobDefinition;
import com.banking.forms.pipeline.domain.PipelineJobType;
import com.banking.forms.pipeline.infrastructure.PipelineJobDefinitionRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/** Polls enabled batch jobs and runs them when their cron schedule is due. */
@Component
public class PipelineJobScheduler {

    private final PipelineJobDefinitionRepository jobRepository;
    private final PipelineJobRunner jobRunner;

    public PipelineJobScheduler(PipelineJobDefinitionRepository jobRepository, PipelineJobRunner jobRunner) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
    }

    @Scheduled(fixedDelayString = "${pipeline.jobs.poll-interval-ms:60000}")
    public void pollBatchJobs() {
        for (PipelineJobDefinition job : jobRepository.findByJobTypeAndEnabledTrue(PipelineJobType.BATCH)) {
            if (!isDue(job)) {
                continue;
            }
            try {
                jobRunner.run(job.getTenantId(), job.getId(), null);
            } catch (RuntimeException ignored) {
                // fail-safe — job run row records the failure
            }
        }
    }

    private boolean isDue(PipelineJobDefinition job) {
        String cron = job.getScheduleCron();
        if (cron == null || cron.isBlank()) {
            return job.getLastRunAt() == null;
        }
        try {
            CronExpression expression = CronExpression.parse(cron);
            Instant anchor = job.getLastRunAt() == null ? Instant.now().minusSeconds(86400) : job.getLastRunAt();
            Instant next = expression.next(anchor);
            return next != null && !next.isAfter(Instant.now());
        } catch (Exception ex) {
            return false;
        }
    }
}
