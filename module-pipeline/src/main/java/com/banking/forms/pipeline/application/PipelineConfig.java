package com.banking.forms.pipeline.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled pipeline outbox poller ({@link PipelineOutboxDispatcher}). */
@Configuration
@EnableScheduling
public class PipelineConfig {}
