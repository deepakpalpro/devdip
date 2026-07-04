package com.banking.forms.downstream.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the scheduled outbox poller ({@link DownstreamDispatcher}). */
@Configuration
@EnableScheduling
public class DownstreamConfig {}
