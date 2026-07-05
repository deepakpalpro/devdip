package com.banking.forms.observability.metrics;

import com.banking.forms.pipeline.application.PipelineResult;
import com.banking.forms.pipeline.application.SubmissionPipelineService;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PipelineMetricsAspect {

    private final PlatformMetrics metrics;

    public PipelineMetricsAspect(PlatformMetrics metrics) {
        this.metrics = metrics;
    }

    @Around("execution(* com.banking.forms.pipeline.application.SubmissionPipelineService.process(..))")
    public Object recordPipeline(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metrics.startPipelineTimer();
        try {
            Object result = joinPoint.proceed();
            if (result instanceof PipelineResult pipelineResult) {
                metrics.recordPipelineRun(pipelineResult.status(), sample);
            } else {
                metrics.recordPipelineRun("UNKNOWN", sample);
            }
            return result;
        } catch (Throwable ex) {
            metrics.recordPipelineRun("FAILED", sample);
            throw ex;
        }
    }
}
